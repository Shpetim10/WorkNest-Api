package com.worknest.features.payroll.application;

import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.LeaveRequest;
import com.worknest.domain.entities.PayrollAdjustment;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.EmploymentStatus;
import com.worknest.domain.enums.LeaveType;
import com.worknest.domain.enums.PaymentMethod;
import com.worknest.domain.enums.PayrollAdjustmentType;
import com.worknest.domain.enums.PayrollStatus;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollCalculationResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PayrollCalculationEngineTest {

    private final PayrollCalculationEngine engine = new PayrollCalculationEngine(
            new DefaultWorkHoursProvider(),
            new PlaceholderSickLeavePolicy()
    );

    @Test
    void monthlyEmployeeFullMonthUsesMonthlySalary() {
        Employee employee = employee(PaymentMethod.FIXED_MONTHLY, "2200.00", null, LocalDate.of(2026, 1, 1));

        PayrollCalculationResponse result = engine.calculate(
                employee, YearMonth.of(2026, 5), List.of(), List.of(), List.of(), PayrollStatus.DRAFT, true);

        assertThat(result.totals().basePay()).isEqualByComparingTo("2200.00");
        assertThat(result.totals().grossEarnings()).isEqualByComparingTo("2200.00");
        assertThat(result.basePayCalculation().prorationMethod()).isEqualTo("WORKING_DAYS");
    }

    @Test
    void monthlyEmployeeStartingMidMonthIsProratedByWorkingDays() {
        Employee employee = employee(PaymentMethod.FIXED_MONTHLY, "2200.00", null, LocalDate.of(2026, 5, 18));

        PayrollCalculationResponse result = engine.calculate(
                employee, YearMonth.of(2026, 5), List.of(), List.of(), List.of(), PayrollStatus.DRAFT, true);

        assertThat(result.workPeriod().workingDaysInMonth()).isEqualTo(21);
        assertThat(result.workPeriod().payableWorkingDays()).isEqualByComparingTo("10");
        assertThat(result.totals().basePay()).isEqualByComparingTo("1047.62");
    }

    @Test
    void hourlyEmployeeFullMonthUsesDefaultHoursPlaceholder() {
        Employee employee = employee(PaymentMethod.HOURLY, null, "10.00", LocalDate.of(2026, 1, 1));

        PayrollCalculationResponse result = engine.calculate(
                employee, YearMonth.of(2026, 5), List.of(), List.of(), List.of(), PayrollStatus.DRAFT, true);

        assertThat(result.workPeriod().payableHours()).isEqualByComparingTo("168");
        assertThat(result.totals().basePay()).isEqualByComparingTo("1680.00");
        assertThat(result.workPeriod().workHoursSource()).isEqualTo(DefaultWorkHoursProvider.SOURCE);
    }

    @Test
    void hourlyEmployeeMissingRateFailsClearly() {
        Employee employee = employee(PaymentMethod.HOURLY, null, null, LocalDate.of(2026, 1, 1));

        assertThatThrownBy(() -> engine.calculate(
                employee, YearMonth.of(2026, 5), List.of(), List.of(), List.of(), PayrollStatus.DRAFT, true))
                .isInstanceOf(PayrollCalculationException.class)
                .hasMessage("Hourly employee must have a positive hourly rate.");
    }

    @Test
    void leaveTakenBeforeCurrentMonthReducesAllowanceAndCreatesUnpaidDeduction() {
        Employee employee = employee(PaymentMethod.FIXED_MONTHLY, "2200.00", null, LocalDate.of(2026, 1, 1));
        employee.setLeaveDaysPerYear(20);
        LeaveRequest before = leave(employee, LeaveType.VACATION, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 24));
        LeaveRequest current = leave(employee, LeaveType.VACATION, LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 8));

        PayrollCalculationResponse result = engine.calculate(
                employee,
                YearMonth.of(2026, 5),
                List.of(before, current),
                List.of(current),
                List.of(),
                PayrollStatus.DRAFT,
                true);

        assertThat(result.leaveCalculation().usedPaidLeaveBeforeThisMonth()).isEqualByComparingTo("18");
        assertThat(result.leaveCalculation().leaveTakenThisMonth()).isEqualByComparingTo("5");
        assertThat(result.leaveCalculation().paidLeaveDaysThisMonth()).isEqualByComparingTo("2");
        assertThat(result.leaveCalculation().unpaidLeaveDaysThisMonth()).isEqualByComparingTo("3");
        assertThat(result.leaveCalculation().unpaidLeaveDeduction()).isEqualByComparingTo("314.29");
    }

    @Test
    void sickLeavePlaceholderIsTransparentAndDoesNotAddSilentPay() {
        Employee employee = employee(PaymentMethod.FIXED_MONTHLY, "2200.00", null, LocalDate.of(2026, 1, 1));
        LeaveRequest sickLeave = leave(employee, LeaveType.SICK, LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 6));

        PayrollCalculationResponse result = engine.calculate(
                employee, YearMonth.of(2026, 5), List.of(sickLeave), List.of(sickLeave), List.of(), PayrollStatus.DRAFT, true);

        assertThat(result.sickLeaveCalculation().daysTakenThisMonth()).isEqualByComparingTo("3");
        assertThat(result.sickLeaveCalculation().companyPaidAmount()).isNull();
        assertThat(result.sickLeaveCalculation().status()).isEqualTo(PlaceholderSickLeavePolicy.STATUS);
        assertThat(result.warnings()).anyMatch(warning -> warning.contains("Sick leave policy is not configured"));
    }

    @Test
    void bonusAndDeductionAreAppliedSeparately() {
        Employee employee = employee(PaymentMethod.HOURLY, null, "10.00", LocalDate.of(2026, 1, 1));
        PayrollAdjustment bonus = adjustment(employee, PayrollAdjustmentType.BONUS, "300.00");
        PayrollAdjustment deduction = adjustment(employee, PayrollAdjustmentType.DEDUCTION, "100.00");

        PayrollCalculationResponse result = engine.calculate(
                employee, YearMonth.of(2026, 5), List.of(), List.of(), List.of(bonus, deduction), PayrollStatus.DRAFT, true);

        assertThat(result.adjustments().totalBonus()).isEqualByComparingTo("300.00");
        assertThat(result.adjustments().totalManualDeduction()).isEqualByComparingTo("100.00");
        assertThat(result.totals().grossEarnings()).isEqualByComparingTo("1680.00");
    }

    private Employee employee(PaymentMethod paymentMethod, String monthlySalary, String hourlyRate, LocalDate startDate) {
        Company company = new Company();
        company.setId(UUID.randomUUID());
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

    private LeaveRequest leave(Employee employee, LeaveType type, LocalDate start, LocalDate end) {
        LeaveRequest leave = new LeaveRequest();
        leave.setId(UUID.randomUUID());
        leave.setCompany(employee.getCompany());
        leave.setEmployee(employee);
        leave.setLeaveType(type);
        leave.setStartDate(start);
        leave.setEndDate(end);
        leave.setTotalDays((int) java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1);
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
