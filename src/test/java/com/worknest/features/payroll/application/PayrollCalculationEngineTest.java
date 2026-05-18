package com.worknest.features.payroll.application;

import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.LeaveBalance;
import com.worknest.domain.entities.LeaveRequest;
import com.worknest.domain.entities.PayrollAdjustment;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.EmploymentStatus;
import com.worknest.domain.enums.LeaveType;
import com.worknest.domain.enums.PaymentMethod;
import com.worknest.domain.enums.PayrollAdjustmentType;
import com.worknest.domain.enums.PayrollStatus;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollCalculationResponse;
import com.worknest.domain.entities.CompanyPayrollSettings;
import com.worknest.domain.entities.CompanyTaxBracket;
import com.worknest.domain.enums.TaxBase;
import com.worknest.features.payroll.repository.CompanyPayrollSettingsRepository;
import com.worknest.features.payroll.repository.CompanyTaxBracketRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the payroll calculation engine.
 * Uses mocked repositories; WorkingDayCalculator uses SAT+SUN defaults (no holidays, default work week).
 *
 * Note: tests updated for B5 (leave from LeaveBalance per-type) and B2 (statutory deductions).
 * The old pooled-leaveDaysPerYear model is replaced by per-type LeaveBalance entries.
 */
@ExtendWith(MockitoExtension.class)
class PayrollCalculationEngineTest {

    @Mock
    private CompanyPayrollSettingsRepository settingsRepository;

    @Mock
    private CompanyTaxBracketRepository taxBracketRepository;

    @Mock
    private WorkingDayCalculator workingDayCalculatorMock;

    private PayrollCalculationEngine engine;

    @BeforeEach
    void setUp() {
        // Default: no company settings → system defaults; no tax brackets → 0% tax with warning
        // lenient: not every test exercises all four of these defaults
        lenient().when(settingsRepository.findByCompanyId(any())).thenReturn(Optional.empty());
        lenient().when(taxBracketRepository.findAllByCompanyIdOrderByOrdinalAsc(any())).thenReturn(List.of());
        // WorkingDayCalculator: delegate to PayrollDateUtils defaults (SAT+SUN, no holidays)
        lenient().when(workingDayCalculatorMock.countWorkingDays(any(), any(), any()))
                .thenAnswer(inv -> PayrollDateUtils.countWorkingDays(inv.getArgument(1), inv.getArgument(2)));
        lenient().when(workingDayCalculatorMock.resolveWeekendDays(any()))
                .thenReturn(java.util.EnumSet.of(java.time.DayOfWeek.SATURDAY, java.time.DayOfWeek.SUNDAY));

        engine = new PayrollCalculationEngine(
                new DefaultWorkHoursProvider(),
                new PlaceholderSickLeavePolicy(),
                workingDayCalculatorMock,
                settingsRepository,
                taxBracketRepository
        );
    }

    @Test
    void monthlyEmployeeFullMonthUsesMonthlySalary() {
        Employee employee = employee(PaymentMethod.FIXED_MONTHLY, "2200.00", null, LocalDate.of(2026, 1, 1));

        PayrollCalculationResponse result = calculate(engine, employee, YearMonth.of(2026, 5), List.of(), List.of(), List.of());

        assertThat(result.totals().basePay()).isEqualByComparingTo("2200.00");
        assertThat(result.totals().grossEarnings()).isEqualByComparingTo("2200.00");
        assertThat(result.basePayCalculation().prorationMethod()).isEqualTo("WORKING_DAYS");
    }

    @Test
    void monthlyEmployeeStartingMidMonthIsProratedByWorkingDays() {
        Employee employee = employee(PaymentMethod.FIXED_MONTHLY, "2200.00", null, LocalDate.of(2026, 5, 18));

        PayrollCalculationResponse result = calculate(engine, employee, YearMonth.of(2026, 5), List.of(), List.of(), List.of());

        assertThat(result.workPeriod().workingDaysInMonth()).isEqualTo(21);
        assertThat(result.workPeriod().payableWorkingDays()).isEqualByComparingTo("10");
        assertThat(result.totals().basePay()).isEqualByComparingTo("1047.62");
    }

    @Test
    void hourlyEmployeeFullMonthUsesDefaultHoursPlaceholder() {
        Employee employee = employee(PaymentMethod.HOURLY, null, "10.00", LocalDate.of(2026, 1, 1));

        PayrollCalculationResponse result = calculate(engine, employee, YearMonth.of(2026, 5), List.of(), List.of(), List.of());

        assertThat(result.workPeriod().payableHours()).isEqualByComparingTo("168");
        assertThat(result.totals().basePay()).isEqualByComparingTo("1680.00");
        assertThat(result.workPeriod().workHoursSource()).isEqualTo(DefaultWorkHoursProvider.SOURCE);
    }

    @Test
    void hourlyEmployeeMissingRateFailsClearly() {
        Employee employee = employee(PaymentMethod.HOURLY, null, null, LocalDate.of(2026, 1, 1));

        assertThatThrownBy(() -> calculate(engine, employee, YearMonth.of(2026, 5), List.of(), List.of(), List.of()))
                .isInstanceOf(PayrollCalculationException.class)
                .hasMessage("Hourly employee must have a positive hourly rate.");
    }

    /**
     * B5: Leave is sourced from LeaveBalance per-type.
     * Vacation balance = 20 total, 18 used before this month → 2 remaining → 2 paid, 3 unpaid.
     */
    @Test
    void leaveDeductedFromLeaveBalanceNotFromLegacyAllowancePool() {
        Employee employee = employee(PaymentMethod.FIXED_MONTHLY, "2200.00", null, LocalDate.of(2026, 1, 1));
        LeaveRequest before = leave(employee, LeaveType.VACATION, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 24));
        LeaveRequest current = leave(employee, LeaveType.VACATION, LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 8));

        // B5: Balance has 20 total, 18 used → 2 remaining
        LeaveBalance balance = leaveBalance(employee, LeaveType.VACATION, 2026, "20", "18");

        PayrollCalculationResponse result = calculate(engine, employee, YearMonth.of(2026, 5),
                List.of(before, current), List.of(current), List.of(balance));

        // 5 days in month, 2 paid from remaining balance, 3 unpaid
        assertThat(result.leaveCalculation().paidLeaveDaysThisMonth()).isEqualByComparingTo("2");
        assertThat(result.leaveCalculation().unpaidLeaveDaysThisMonth()).isEqualByComparingTo("3");
        assertThat(result.leaveCalculation().unpaidLeaveDeduction()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void sickLeavePlaceholderIsTransparentAndDoesNotAddSilentPay() {
        Employee employee = employee(PaymentMethod.FIXED_MONTHLY, "2200.00", null, LocalDate.of(2026, 1, 1));
        LeaveRequest sickLeave = leave(employee, LeaveType.SICK, LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 6));

        PayrollCalculationResponse result = calculate(engine, employee, YearMonth.of(2026, 5),
                List.of(sickLeave), List.of(sickLeave), List.of());

        assertThat(result.sickLeaveCalculation().daysTakenThisMonth()).isEqualByComparingTo("3");
        assertThat(result.sickLeaveCalculation().status()).isEqualTo(PlaceholderSickLeavePolicy.STATUS);
        assertThat(result.warnings()).anyMatch(warning -> warning.contains("Sick leave policy is not configured"));
    }

    @Test
    void bonusAndDeductionAreAppliedSeparately() {
        Employee employee = employee(PaymentMethod.HOURLY, null, "10.00", LocalDate.of(2026, 1, 1));
        PayrollAdjustment bonus = adjustment(employee, PayrollAdjustmentType.BONUS, "300.00");
        PayrollAdjustment deduction = adjustment(employee, PayrollAdjustmentType.DEDUCTION, "100.00");

        PayrollCalculationResponse result = calculate(engine, employee, YearMonth.of(2026, 5),
                List.of(), List.of(), List.of(), bonus, deduction);

        assertThat(result.adjustments().totalBonus()).isEqualByComparingTo("300.00");
        assertThat(result.adjustments().totalManualDeduction()).isEqualByComparingTo("100.00");
        assertThat(result.totals().grossEarnings()).isEqualByComparingTo("1980.00"); // 1680 + 300
    }

    @Test
    void bonusIncludedInGrossEarningsForMonthlyEmployee() {
        Employee employee = employee(PaymentMethod.FIXED_MONTHLY, "3000.00", null, LocalDate.of(2026, 1, 1));
        PayrollAdjustment bonus = adjustment(employee, PayrollAdjustmentType.BONUS, "500.00");

        PayrollCalculationResponse result = calculate(engine, employee, YearMonth.of(2026, 5),
                List.of(), List.of(), List.of(), bonus);

        assertThat(result.totals().basePay()).isEqualByComparingTo("3000.00");
        assertThat(result.totals().grossEarnings()).isEqualByComparingTo("3500.00");
    }

    @Test
    void hourlyEmployeeUnpaidLeaveNotDeductedSeparately() {
        Employee employee = employee(PaymentMethod.HOURLY, null, "10.00", LocalDate.of(2026, 1, 1));
        LeaveRequest unpaid = leave(employee, LeaveType.UNPAID, LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 8));

        PayrollCalculationResponse result = calculate(engine, employee, YearMonth.of(2026, 5),
                List.of(unpaid), List.of(unpaid), List.of());

        // For hourly, unpaid deduction is zero (attendance handles it)
        assertThat(result.leaveCalculation().unpaidLeaveDeduction()).isEqualByComparingTo("0.00");
    }

    @Test
    void hourlyEmployeeReceivesPaidAnnualLeaveOnTopOfAttendanceHours() {
        PayrollCalculationEngine attendanceEngine = new PayrollCalculationEngine(
                (employee, context, payableWorkingDays, payableFrom, payableTo) ->
                        new WorkHoursProvider.WorkHoursResult(new BigDecimal("128"), AttendanceWorkHoursProvider.SOURCE),
                new PlaceholderSickLeavePolicy(),
                workingDayCalculatorMock,
                settingsRepository,
                taxBracketRepository
        );
        Employee employee = employee(PaymentMethod.HOURLY, null, "10.00", LocalDate.of(2026, 1, 1));
        employee.setDailyWorkingHours(new BigDecimal("8.0"));
        LeaveRequest vacation = leave(employee, LeaveType.VACATION, LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 8));
        // Balance: 20 total, 0 used → 20 remaining → all 5 days paid
        LeaveBalance balance = leaveBalance(employee, LeaveType.VACATION, 2026, "20", "0");

        PayrollCalculationResponse result = attendanceEngine.calculate(employee, YearMonth.of(2026, 5),
                List.of(vacation), List.of(vacation), List.of(balance), List.of(), PayrollStatus.DRAFT, true);

        assertThat(result.totals().basePay()).isEqualByComparingTo("1280.00");
        assertThat(result.leaveCalculation().paidLeaveDaysThisMonth()).isEqualByComparingTo("5");
        assertThat(result.leaveCalculation().paidLeaveAmount()).isEqualByComparingTo("400.00");
        assertThat(result.totals().grossEarnings()).isEqualByComparingTo("1680.00");
    }

    @Test
    void hourlyEmployeeShowsFullPaymentAttendanceDeductionAndPaymentReceived() {
        // Use a closed past month (April 2026 = 22 working days) so effectiveAttendanceTo = Apr 30
        // and fullPayableHours is deterministic regardless of when this test runs.
        PayrollCalculationEngine attendanceEngine = new PayrollCalculationEngine(
                (employee, context, payableWorkingDays, payableFrom, payableTo) ->
                        new WorkHoursProvider.WorkHoursResult(new BigDecimal("120"), AttendanceWorkHoursProvider.SOURCE),
                new PlaceholderSickLeavePolicy(),
                workingDayCalculatorMock,
                settingsRepository,
                taxBracketRepository
        );
        Employee employee = employee(PaymentMethod.HOURLY, null, "10.00", LocalDate.of(2026, 1, 1));
        employee.setDailyWorkingHours(new BigDecimal("8.0"));

        // April 2026: 22 working days → fullPayableHours = 22 * 8 = 176 → fullPayment = 1760
        // worked 120h → paymentReceived = 1200, attendanceDeduction = 560
        PayrollCalculationResponse result = attendanceEngine.calculate(employee, YearMonth.of(2026, 4),
                List.of(), List.of(), List.of(), List.of(), PayrollStatus.DRAFT, true);

        assertThat(result.hourlyAttendancePayment().fullPayableHours()).isEqualByComparingTo("176.0");
        assertThat(result.hourlyAttendancePayment().attendedHours()).isEqualByComparingTo("120");
        assertThat(result.hourlyAttendancePayment().fullPayment()).isEqualByComparingTo("1760.00");
        assertThat(result.hourlyAttendancePayment().attendanceDeduction()).isEqualByComparingTo("560.00");
        assertThat(result.hourlyAttendancePayment().paymentReceived()).isEqualByComparingTo("1200.00");
        assertThat(result.totals().basePay()).isEqualByComparingTo("1200.00");
        // effectiveAttendanceTo == payableTo for a closed month
        assertThat(result.workPeriod().effectiveAttendanceTo()).isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(result.workPeriod().effectivePayableWorkingDays()).isEqualByComparingTo("22");
    }

    @Test
    void fixedMonthlyEmployeeAllLeaveUnpaidWhenNoBalance() {
        Employee employee = employee(PaymentMethod.FIXED_MONTHLY, "2200.00", null, LocalDate.of(2026, 1, 1));
        // No leave balance → all vacation days are unpaid excess
        LeaveRequest vacation = leave(employee, LeaveType.VACATION, LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 8));

        PayrollCalculationResponse result = calculate(engine, employee, YearMonth.of(2026, 5),
                List.of(vacation), List.of(vacation), List.of()); // no balance entries

        assertThat(result.leaveCalculation().unpaidLeaveDaysThisMonth()).isEqualByComparingTo("5");
        assertThat(result.totals().totalDeductions()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void netPayReflectsGrossMinusDeductions() {
        Employee employee = employee(PaymentMethod.FIXED_MONTHLY, "2200.00", null, LocalDate.of(2026, 1, 1));
        PayrollAdjustment bonus = adjustment(employee, PayrollAdjustmentType.BONUS, "200.00");
        PayrollAdjustment deduction = adjustment(employee, PayrollAdjustmentType.DEDUCTION, "50.00");

        PayrollCalculationResponse result = calculate(engine, employee, YearMonth.of(2026, 5),
                List.of(), List.of(), List.of(), bonus, deduction);

        // I3: net = gross - totalDeductions
        BigDecimal expectedNet = result.totals().grossEarnings().subtract(result.totals().totalDeductions());
        assertThat(result.totals().netPay()).isEqualByComparingTo(expectedNet);
    }

    @Test
    void statutoryDeductionsWarnWhenNoBracketsConfigured() {
        Employee employee = employee(PaymentMethod.FIXED_MONTHLY, "2200.00", null, LocalDate.of(2026, 1, 1));

        PayrollCalculationResponse result = calculate(engine, employee, YearMonth.of(2026, 5),
                List.of(), List.of(), List.of());

        // No settings → default warning + no tax
        assertThat(result.warnings()).anyMatch(w -> w.contains("Statutory deductions used system defaults"));
        assertThat(result.statutoryDeductions().incomeTax()).isEqualByComparingTo("0.00");
        assertThat(result.statutoryDeductions().employeeSocialSecurity()).isEqualByComparingTo("0.00");
    }

    @Test
    void maternityLeaveIsStatutoryAndDoesNotDeductFromPayOrPool() {
        Employee employee = employee(PaymentMethod.FIXED_MONTHLY, "2200.00", null, LocalDate.of(2026, 1, 1));
        LeaveRequest maternity = leave(employee, LeaveType.MATERNITY, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        PayrollCalculationResponse result = calculate(engine, employee, YearMonth.of(2026, 5),
                List.of(maternity), List.of(maternity), List.of());

        // Maternity is statutory — no unpaid deduction, no pool consumption
        assertThat(result.leaveCalculation().unpaidLeaveDeduction()).isEqualByComparingTo("0.00");
        assertThat(result.leaveCalculation().leaveRecordsIncluded())
                .anyMatch(r -> "STATUTORY_MATERNITY".equals(r.payrollTreatment()));
    }

    /**
     * B2: Full statutory deduction round-trip.
     * SS employee 9%, pension employee 6%, one tax bracket: 15% on entire gross.
     * Tax base = GROSS_MINUS_CONTRIBUTIONS → taxable = gross - SS - pension.
     */
    @Test
    void statutoryDeductionsRoundTripWithSsAndPensionAndProgressiveTax() {
        CompanyPayrollSettings settings = payrollSettings(
                "9.000", "14.000",  // SS employee/employer
                "6.000", "5.000",   // pension employee/employer
                null, null,
                TaxBase.GROSS_MINUS_CONTRIBUTIONS, true);
        CompanyTaxBracket bracket = taxBracket(0, BigDecimal.ZERO, null, new BigDecimal("15.000"));

        when(settingsRepository.findByCompanyId(any())).thenReturn(Optional.of(settings));
        when(taxBracketRepository.findAllByCompanyIdOrderByOrdinalAsc(any())).thenReturn(List.of(bracket));

        Employee employee = employee(PaymentMethod.FIXED_MONTHLY, "3000.00", null, LocalDate.of(2026, 1, 1));
        PayrollCalculationResponse result = calculate(engine, employee, YearMonth.of(2026, 5), List.of(), List.of(), List.of());

        BigDecimal gross = result.totals().grossEarnings();                        // 3000.00
        BigDecimal ss = result.statutoryDeductions().employeeSocialSecurity();         // 3000 * 9% = 270.00
        BigDecimal pension = result.statutoryDeductions().employeePensionContribution(); // 3000 * 6% = 180.00
        BigDecimal taxableIncome = result.statutoryDeductions().taxableIncome();   // 3000 - 270 - 180 = 2550.00
        BigDecimal tax = result.statutoryDeductions().incomeTax();                 // 2550 * 15% = 382.50

        assertThat(gross).isEqualByComparingTo("3000.00");
        assertThat(ss).isEqualByComparingTo("270.00");
        assertThat(pension).isEqualByComparingTo("180.00");
        assertThat(taxableIncome).isEqualByComparingTo("2550.00");
        assertThat(tax).isEqualByComparingTo("382.50");
        assertThat(result.statutoryDeductions().statutoryDeductionsTotal())
                .isEqualByComparingTo("832.50"); // 270 + 180 + 382.50
        assertThat(result.totals().netPay()).isEqualByComparingTo("2167.50"); // 3000 - 832.50
        // Reconciliation: net = gross - totalDeductions
        assertThat(result.totals().netPay())
                .isEqualByComparingTo(result.totals().grossEarnings().subtract(result.totals().totalDeductions()));
    }

    /**
     * B2: When gross earnings fall below the contribution minimum base,
     * a warning must be emitted because contributions are inflated.
     */
    @Test
    void contributionMinBaseAboveGrossEarningsEmitsWarning() {
        CompanyPayrollSettings settings = payrollSettings(
                "9.000", "14.000",
                "6.000", "5.000",
                new BigDecimal("5000.00"), null, // minBase = 5000, gross = 2200
                TaxBase.GROSS_MINUS_CONTRIBUTIONS, true);
        when(settingsRepository.findByCompanyId(any())).thenReturn(Optional.of(settings));
        when(taxBracketRepository.findAllByCompanyIdOrderByOrdinalAsc(any())).thenReturn(List.of());

        Employee employee = employee(PaymentMethod.FIXED_MONTHLY, "2200.00", null, LocalDate.of(2026, 1, 1));
        PayrollCalculationResponse result = calculate(engine, employee, YearMonth.of(2026, 5), List.of(), List.of(), List.of());

        assertThat(result.warnings()).anyMatch(w -> w.contains("below the contribution minimum base"));
        // Contributions are calculated on 5000, not 2200
        assertThat(result.statutoryDeductions().employeeSocialSecurity()).isEqualByComparingTo("450.00");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PayrollCalculationResponse calculate(
            PayrollCalculationEngine eng, Employee employee, YearMonth month,
            List<LeaveRequest> inYear, List<LeaveRequest> inMonth, List<LeaveBalance> balances,
            PayrollAdjustment... adjustments) {
        return eng.calculate(employee, month, inYear, inMonth, balances,
                List.of(adjustments), PayrollStatus.DRAFT, true);
    }

    private Employee employee(PaymentMethod paymentMethod, String monthlySalary, String hourlyRate,
                               LocalDate startDate) {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setCurrency("ALL");
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("John");
        user.setLastName("Doe");
        Employee employee = new Employee();
        employee.setId(UUID.randomUUID());
        employee.setCompany(company);
        employee.setUser(user);
        employee.setEmploymentStatus(EmploymentStatus.ACTIVE);
        employee.setStartDate(startDate);
        employee.setPaymentMethod(paymentMethod);
        employee.setMonthlySalary(monthlySalary == null ? null : new BigDecimal(monthlySalary));
        employee.setHourlyRate(hourlyRate == null ? null : new BigDecimal(hourlyRate));
        employee.setLeaveDaysPerYear(20);
        return employee;
    }

    private LeaveBalance leaveBalance(Employee employee, LeaveType type, int year,
                                      String totalDays, String usedDays) {
        LeaveBalance lb = new LeaveBalance();
        lb.setId(UUID.randomUUID());
        lb.setCompany(employee.getCompany());
        lb.setEmployee(employee);
        lb.setYear(year);
        lb.setLeaveType(type);
        lb.setTotalDays(new BigDecimal(totalDays));
        lb.setUsedDays(new BigDecimal(usedDays));
        return lb;
    }

    private LeaveRequest leave(Employee employee, LeaveType type, LocalDate start, LocalDate end) {
        LeaveRequest leave = new LeaveRequest();
        leave.setId(UUID.randomUUID());
        leave.setCompany(employee.getCompany());
        leave.setEmployee(employee);
        leave.setLeaveType(type);
        leave.setStartDate(start);
        leave.setEndDate(end);
        leave.setDaysCount(BigDecimal.valueOf(java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1));
        return leave;
    }

    private PayrollAdjustment adjustment(Employee employee, PayrollAdjustmentType type, String amount) {
        PayrollAdjustment adjustment = new PayrollAdjustment();
        adjustment.setId(UUID.randomUUID());
        adjustment.setEmployee(employee);
        adjustment.setCompany(employee.getCompany());
        adjustment.setType(type);
        adjustment.setAmount(new BigDecimal(amount));
        adjustment.setReason(type.name());
        return adjustment;
    }

    private CompanyPayrollSettings payrollSettings(
            String ssEmployee, String ssEmployer,
            String pensionEmployee, String pensionEmployer,
            BigDecimal minBase, BigDecimal maxBase,
            TaxBase taxBase, boolean taxEnabled) {
        CompanyPayrollSettings s = new CompanyPayrollSettings();
        s.setSocialSecurityEmployeeRate(new BigDecimal(ssEmployee));
        s.setSocialSecurityEmployerRate(new BigDecimal(ssEmployer));
        s.setPensionEmployeeRate(new BigDecimal(pensionEmployee));
        s.setPensionEmployerRate(new BigDecimal(pensionEmployer));
        s.setContributionMinBase(minBase);
        s.setContributionMaxBase(maxBase);
        s.setTaxBase(taxBase);
        s.setTaxEnabled(taxEnabled);
        s.setDefaultDailyWorkingHours(BigDecimal.valueOf(8));
        s.setWeekendDaysJson("[\"SATURDAY\",\"SUNDAY\"]");
        return s;
    }

    private CompanyTaxBracket taxBracket(int ordinal, BigDecimal lo, BigDecimal hi, BigDecimal rate) {
        CompanyTaxBracket b = new CompanyTaxBracket();
        b.setId(UUID.randomUUID());
        b.setOrdinal(ordinal);
        b.setLowerBound(lo);
        b.setUpperBound(hi);
        b.setRate(rate);
        return b;
    }
}
