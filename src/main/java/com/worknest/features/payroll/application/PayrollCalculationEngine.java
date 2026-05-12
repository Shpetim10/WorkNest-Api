package com.worknest.features.payroll.application;

import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.LeaveRequest;
import com.worknest.domain.entities.PayrollAdjustment;
import com.worknest.domain.enums.EmploymentStatus;
import com.worknest.domain.enums.LeaveType;
import com.worknest.domain.enums.PaymentMethod;
import com.worknest.domain.enums.PayrollAdjustmentType;
import com.worknest.domain.enums.PayrollCalculationStatus;
import com.worknest.domain.enums.PayrollStatus;
import com.worknest.features.payroll.dto.PayrollDtos.AdjustmentDetails;
import com.worknest.features.payroll.dto.PayrollDtos.BasePayDetails;
import com.worknest.features.payroll.dto.PayrollDtos.EmploymentPeriodDetails;
import com.worknest.features.payroll.dto.PayrollDtos.LeaveCalculationDetails;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollAdjustmentLine;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollCalculationResponse;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollLeaveRecordDetails;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollTotals;
import com.worknest.features.payroll.dto.PayrollDtos.WorkPeriodDetails;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayrollCalculationEngine {

    private static final BigDecimal DEFAULT_DAILY_HOURS = BigDecimal.valueOf(8);
    private static final String CURRENCY = "EUR";
    private static final String WORKING_DAY_PRORATION = "WORKING_DAYS";

    private final WorkHoursProvider workHoursProvider;
    private final SickLeavePolicy sickLeavePolicy;

    public PayrollCalculationResponse calculate(
            Employee employee,
            YearMonth payrollMonth,
            List<LeaveRequest> approvedLeavesInYear,
            List<LeaveRequest> approvedLeavesInMonth,
            List<PayrollAdjustment> adjustments,
            PayrollStatus payrollStatus,
            boolean preview
    ) {
        PayrollContext context = buildContext(payrollMonth);
        validateEmployee(employee);

        LocalDate employmentStart = employee.getStartDate();
        LocalDate employmentEnd = employee.getContractExpiryDate();
        LocalDate payableFrom = PayrollDateUtils.max(employmentStart, context.periodStart());
        LocalDate payableTo = employmentEnd == null ? context.periodEnd() : PayrollDateUtils.min(employmentEnd, context.periodEnd());
        if (payableTo.isBefore(payableFrom)) {
            throw new PayrollCalculationException("NO_ACTIVE_CONTRACT_IN_PERIOD",
                    "Employee has no payable employment days in this payroll period.");
        }

        List<String> warnings = new ArrayList<>();
        if (employee.getEmploymentStatus() != EmploymentStatus.ACTIVE
                && employee.getEmploymentStatus() != EmploymentStatus.PROBATION
                && employee.getEmploymentStatus() != EmploymentStatus.ON_LEAVE) {
            warnings.add("Employee status is " + employee.getEmploymentStatus()
                    + ". Calculation is based on contract dates only and should be reviewed.");
        }
        warnings.add("Worked hours are calculated using default working days and default daily hours. Timesheet integration is not implemented yet.");
        warnings.add("Public holidays are not excluded yet. TODO: integrate company holiday calendars.");
        warnings.add("Tax, social security, pension, employer-side contributions, currency and rounding settings are TODO placeholders.");

        BigDecimal payableWorkingDays = PayrollDateUtils.countWorkingDays(payableFrom, payableTo);
        WorkHoursProvider.WorkHoursResult workHours = workHoursProvider.getWorkedHours(employee, context, payableWorkingDays);
        BigDecimal basePay = calculateBasePay(employee, context, payableWorkingDays, workHours.hours());
        BasePayDetails basePayDetails = toBasePayDetails(employee, context, payableWorkingDays, workHours.hours(), basePay);

        LeaveCalculationDetails leaveDetails = calculateLeave(employee, context, approvedLeavesInYear, approvedLeavesInMonth);
        var sickLeaveDetails = sickLeavePolicy.calculate(
                employee, sickLeaves(approvedLeavesInMonth), sickLeaves(approvedLeavesInYear), context);
        if (PlaceholderSickLeavePolicy.STATUS.equals(sickLeaveDetails.status())) {
            warnings.add("Sick leave policy is not configured. Sick leave calculation is currently a placeholder.");
        }

        AdjustmentDetails adjustmentDetails = calculateAdjustments(adjustments);
        // Gross salary is the current base earnings only:
        // fixed pay -> prorated/base monthly salary, hourly pay -> rate * payable hours.
        BigDecimal grossEarnings = basePay;
        BigDecimal totalDeductions = money(adjustmentDetails.totalManualDeduction().add(leaveDetails.unpaidLeaveDeduction()));
        BigDecimal netPay = money(grossEarnings.subtract(totalDeductions));
        boolean netPayNegative = netPay.signum() < 0;
        if (netPayNegative) {
            warnings.add("Net pay is negative (" + netPay + " " + CURRENCY + "). Unpaid deductions exceed gross earnings.");
        }

        return new PayrollCalculationResponse(
                employee.getId(),
                employeeName(employee),
                payrollMonth.getYear(),
                payrollMonth.getMonthValue(),
                CURRENCY,
                employee.getPaymentMethod(),
                PayrollCalculationStatus.SUCCESS,
                payrollStatus,
                preview,
                new EmploymentPeriodDetails(employmentStart, employmentEnd, payableFrom, payableTo),
                new WorkPeriodDetails(
                        context.calendarDaysInMonth(),
                        context.workingDaysInMonth(),
                        payableWorkingDays,
                        context.defaultDailyWorkingHours(),
                        workHours.hours(),
                        workHours.source()),
                basePayDetails,
                leaveDetails,
                sickLeaveDetails,
                adjustmentDetails,
                new PayrollTotals(basePay, grossEarnings, totalDeductions, netPay, netPayNegative),
                warnings
        );
    }

    private PayrollContext buildContext(YearMonth payrollMonth) {
        LocalDate start = payrollMonth.atDay(1);
        LocalDate end = payrollMonth.atEndOfMonth();
        return new PayrollContext(
                payrollMonth,
                start,
                end,
                payrollMonth.lengthOfMonth(),
                PayrollDateUtils.countWorkingDays(start, end).intValue(),
                DEFAULT_DAILY_HOURS,
                CURRENCY
        );
    }

    private void validateEmployee(Employee employee) {
        if (employee.getPaymentMethod() == null) {
            throw new PayrollCalculationException("INVALID_PAYMENT_CONFIGURATION",
                    "Employee payment method is required for payroll calculation.");
        }
        if (employee.getStartDate() == null) {
            throw new PayrollCalculationException("INVALID_EMPLOYMENT_PERIOD",
                    "Employee start date is required for payroll calculation.");
        }
        if (employee.getPaymentMethod() == PaymentMethod.FIXED_MONTHLY) {
            if (!positive(employee.getMonthlySalary())) {
                throw new PayrollCalculationException("INVALID_PAYMENT_CONFIGURATION",
                        "Monthly employee must have a positive monthly salary.");
            }
            return;
        }
        if (employee.getPaymentMethod() == PaymentMethod.HOURLY) {
            if (!positive(employee.getHourlyRate())) {
                throw new PayrollCalculationException("INVALID_PAYMENT_CONFIGURATION",
                        "Hourly employee must have a positive hourly rate.");
            }
            return;
        }
        throw new PayrollCalculationException("INVALID_PAYMENT_CONFIGURATION",
                "Unsupported payment method for payroll calculation.");
    }

    private BigDecimal calculateBasePay(Employee employee, PayrollContext context,
                                        BigDecimal payableWorkingDays, BigDecimal payableHours) {
        if (employee.getPaymentMethod() == PaymentMethod.FIXED_MONTHLY) {
            BigDecimal ratio = payableWorkingDays.divide(BigDecimal.valueOf(context.workingDaysInMonth()), 8, RoundingMode.HALF_UP);
            return money(employee.getMonthlySalary().multiply(ratio));
        }
        return money(employee.getHourlyRate().multiply(payableHours));
    }

    private BasePayDetails toBasePayDetails(Employee employee, PayrollContext context,
                                            BigDecimal payableWorkingDays, BigDecimal payableHours, BigDecimal basePay) {
        if (employee.getPaymentMethod() == PaymentMethod.FIXED_MONTHLY) {
            return new BasePayDetails(
                    "monthlySalary * payableWorkingDays / workingDaysInMonth",
                    employee.getMonthlySalary(),
                    null,
                    payableWorkingDays,
                    context.workingDaysInMonth(),
                    null,
                    basePay,
                    WORKING_DAY_PRORATION);
        }
        return new BasePayDetails(
                "hourlyRate * payableHours",
                null,
                employee.getHourlyRate(),
                payableWorkingDays,
                context.workingDaysInMonth(),
                payableHours,
                basePay,
                null);
    }

    private LeaveCalculationDetails calculateLeave(
            Employee employee,
            PayrollContext context,
            List<LeaveRequest> approvedLeavesInYear,
            List<LeaveRequest> approvedLeavesInMonth
    ) {
        int allowance = employee.getLeaveDaysPerYear() != null ? Math.max(employee.getLeaveDaysPerYear(), 0) : 0;
        BigDecimal usedBefore = countAllowanceLeaves(approvedLeavesInYear, context.periodStart().withDayOfYear(1),
                context.periodStart().minusDays(1));
        BigDecimal takenThisMonth = countAllowanceLeaves(approvedLeavesInMonth, context.periodStart(), context.periodEnd());
        BigDecimal explicitUnpaidThisMonth = countLeavesOfType(approvedLeavesInMonth, LeaveType.UNPAID, context.periodStart(), context.periodEnd());
        BigDecimal remainingAllowance = BigDecimal.valueOf(allowance).subtract(usedBefore).max(BigDecimal.ZERO);
        BigDecimal paidThisMonth = takenThisMonth.min(remainingAllowance);
        BigDecimal unpaidExcess = takenThisMonth.subtract(paidThisMonth).max(BigDecimal.ZERO);
        BigDecimal unpaidDays = unpaidExcess.add(explicitUnpaidThisMonth);
        BigDecimal dailyValue = dailyPayValue(employee, context);
        BigDecimal unpaidDeduction = money(dailyValue.multiply(unpaidDays));

        List<PayrollLeaveRecordDetails> records = approvedLeavesInMonth.stream()
                .sorted(Comparator.comparing(LeaveRequest::getStartDate))
                .map(leave -> new PayrollLeaveRecordDetails(
                        leave.getId(),
                        leave.getLeaveType().name(),
                        leave.getStartDate(),
                        leave.getEndDate(),
                        PayrollDateUtils.countWorkingDays(
                                PayrollDateUtils.max(leave.getStartDate(), context.periodStart()),
                                PayrollDateUtils.min(leave.getEndDate(), context.periodEnd())),
                        leaveTreatment(leave.getLeaveType())))
                .toList();

        return new LeaveCalculationDetails(
                allowance,
                usedBefore,
                takenThisMonth,
                paidThisMonth,
                unpaidDays,
                unpaidDeduction,
                records
        );
    }

    private BigDecimal dailyPayValue(Employee employee, PayrollContext context) {
        if (employee.getPaymentMethod() == PaymentMethod.FIXED_MONTHLY) {
            return employee.getMonthlySalary().divide(BigDecimal.valueOf(context.workingDaysInMonth()), 8, RoundingMode.HALF_UP);
        }
        BigDecimal dailyHours = employee.getDailyWorkingHours() != null
                ? employee.getDailyWorkingHours()
                : context.defaultDailyWorkingHours();
        return employee.getHourlyRate().multiply(dailyHours);
    }

    private BigDecimal countAllowanceLeaves(List<LeaveRequest> leaves, LocalDate start, LocalDate end) {
        Set<LocalDate> uniqueDays = new HashSet<>();
        for (LeaveRequest leave : leaves) {
            if (!isAllowanceLeave(leave.getLeaveType())) {
                continue;
            }
            addWorkingOverlap(uniqueDays, leave, start, end);
        }
        return BigDecimal.valueOf(uniqueDays.size());
    }

    private BigDecimal countLeavesOfType(List<LeaveRequest> leaves, LeaveType type, LocalDate start, LocalDate end) {
        Set<LocalDate> uniqueDays = new HashSet<>();
        for (LeaveRequest leave : leaves) {
            if (leave.getLeaveType() == type) {
                addWorkingOverlap(uniqueDays, leave, start, end);
            }
        }
        return BigDecimal.valueOf(uniqueDays.size());
    }

    private void addWorkingOverlap(Set<LocalDate> uniqueDays, LeaveRequest leave, LocalDate start, LocalDate end) {
        LocalDate from = PayrollDateUtils.max(leave.getStartDate(), start);
        LocalDate to = PayrollDateUtils.min(leave.getEndDate(), end);
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            if (PayrollDateUtils.isWorkingDay(cursor)) {
                uniqueDays.add(cursor);
            }
            cursor = cursor.plusDays(1);
        }
    }

    private boolean isAllowanceLeave(LeaveType type) {
        return type != LeaveType.SICK && type != LeaveType.UNPAID;
    }

    private String leaveTreatment(LeaveType type) {
        if (type == LeaveType.SICK) {
            return "SICK_LEAVE_PLACEHOLDER_POLICY";
        }
        if (type == LeaveType.UNPAID) {
            return "UNPAID_DEDUCTION";
        }
        return "PAID_UP_TO_ANNUAL_ALLOWANCE";
    }

    private List<LeaveRequest> sickLeaves(List<LeaveRequest> leaves) {
        return leaves.stream().filter(leave -> leave.getLeaveType() == LeaveType.SICK).toList();
    }

    private AdjustmentDetails calculateAdjustments(List<PayrollAdjustment> adjustments) {
        List<PayrollAdjustmentLine> bonuses = adjustments.stream()
                .filter(adjustment -> adjustment.getType() == PayrollAdjustmentType.BONUS)
                .map(this::toAdjustmentLine)
                .toList();
        List<PayrollAdjustmentLine> deductions = adjustments.stream()
                .filter(adjustment -> adjustment.getType() == PayrollAdjustmentType.DEDUCTION)
                .map(this::toAdjustmentLine)
                .toList();
        BigDecimal totalBonus = money(bonuses.stream().map(PayrollAdjustmentLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal totalDeduction = money(deductions.stream().map(PayrollAdjustmentLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add));
        return new AdjustmentDetails(bonuses, deductions, totalBonus, totalDeduction);
    }

    private PayrollAdjustmentLine toAdjustmentLine(PayrollAdjustment adjustment) {
        return new PayrollAdjustmentLine(adjustment.getId(), adjustment.getAmount(), adjustment.getReason(), adjustment.getNotes());
    }

    private boolean positive(BigDecimal amount) {
        return amount != null && amount.signum() > 0;
    }

    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String employeeName(Employee employee) {
        if (employee.getUser().getDisplayName() != null && !employee.getUser().getDisplayName().isBlank()) {
            return employee.getUser().getDisplayName();
        }
        return (employee.getUser().getFirstName() + " " + employee.getUser().getLastName()).trim();
    }
}
