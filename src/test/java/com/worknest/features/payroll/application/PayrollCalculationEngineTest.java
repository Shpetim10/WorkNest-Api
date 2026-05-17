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

        PayrollCalculationResponse result = attendanceEngine.calculate(employee, YearMonth.of(2026, 5),
                List.of(), List.of(), List.of(), List.of(), PayrollStatus.DRAFT, true);

        assertThat(result.hourlyAttendancePayment().fullPayableHours()).isEqualByComparingTo("168.0");
        assertThat(result.hourlyAttendancePayment().attendedHours()).isEqualByComparingTo("120");
        assertThat(result.hourlyAttendancePayment().fullPayment()).isEqualByComparingTo("1680.00");
        assertThat(result.hourlyAttendancePayment().attendanceDeduction()).isEqualByComparingTo("480.00");
        assertThat(result.hourlyAttendancePayment().paymentReceived()).isEqualByComparingTo("1200.00");
        assertThat(result.totals().basePay()).isEqualByComparingTo("1200.00");
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
}
