package com.worknest.features.payroll.application;

import com.worknest.domain.entities.CompanyPayrollSettings;
import com.worknest.domain.entities.CompanyTaxBracket;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.LeaveBalance;
import com.worknest.domain.entities.LeaveRequest;
import com.worknest.domain.entities.PayrollAdjustment;
import com.worknest.domain.enums.EmploymentStatus;
import com.worknest.domain.enums.LeaveType;
import com.worknest.domain.enums.PaymentMethod;
import com.worknest.domain.enums.PayrollAdjustmentType;
import com.worknest.domain.enums.PayrollCalculationStatus;
import com.worknest.domain.enums.PayrollStatus;
import com.worknest.domain.enums.TaxBase;
import com.worknest.features.payroll.dto.PayrollDtos.AbsenceDetails;
import com.worknest.features.payroll.dto.PayrollDtos.AdjustmentDetails;
import com.worknest.features.payroll.dto.PayrollDtos.BasePayDetails;
import com.worknest.features.payroll.dto.PayrollDtos.EmploymentPeriodDetails;
import com.worknest.features.payroll.dto.PayrollDtos.FixedSalaryAttendancePaymentDetails;
import com.worknest.features.payroll.dto.PayrollDtos.HourlyAttendancePaymentDetails;
import com.worknest.features.payroll.dto.PayrollDtos.LeaveCalculationDetails;
import com.worknest.features.payroll.dto.PayrollDtos.OvertimeDetails;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollAdjustmentLine;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollCalculationResponse;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollLeaveTreatment;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollLeaveRecordDetails;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollTotals;
import com.worknest.features.payroll.dto.PayrollDtos.StatutoryDeductionDetails;
import com.worknest.features.payroll.dto.PayrollDtos.TaxBracketCalculationLine;
import com.worknest.features.payroll.dto.PayrollDtos.WorkPeriodDetails;
import com.worknest.features.payroll.repository.CompanyPayrollSettingsRepository;
import com.worknest.features.payroll.repository.CompanyTaxBracketRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayrollCalculationEngine {

    private static final BigDecimal DEFAULT_DAILY_HOURS = BigDecimal.valueOf(8);
    private static final String WORKING_DAY_PRORATION = "WORKING_DAYS";

    private final WorkHoursProvider workHoursProvider;
    private final SickLeavePolicy sickLeavePolicy;
    private final WorkingDayCalculator workingDayCalculator;
    private final CompanyPayrollSettingsRepository settingsRepository;
    private final CompanyTaxBracketRepository taxBracketRepository;

    public PayrollCalculationResponse calculate(
            Employee employee,
            YearMonth payrollMonth,
            List<LeaveRequest> approvedLeavesInYear,
            List<LeaveRequest> approvedLeavesInMonth,
            List<LeaveBalance> leaveBalancesForYear,
            List<PayrollAdjustment> adjustments,
            PayrollStatus payrollStatus,
            boolean preview
    ) {
        UUID companyId = employee.getCompany().getId();
        String currency = employee.getCompany().getCurrency();
        PayrollContext context = buildContext(companyId, payrollMonth, currency);
        validateEmployee(employee, context);

        LocalDate employmentStart = employee.getStartDate();
        LocalDate employmentEnd = employee.getContractExpiryDate();
        LocalDate payableFrom = PayrollDateUtils.max(employmentStart, context.periodStart());
        LocalDate payableTo = employmentEnd == null ? context.periodEnd()
                : PayrollDateUtils.min(employmentEnd, context.periodEnd());
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

        BigDecimal payableWorkingDays = workingDayCalculator.countWorkingDays(companyId, payableFrom, payableTo);

        // Cap to today for current-month payrolls so expected-hours comparisons don't include future days.
        LocalDate effectiveAttendanceTo = PayrollDateUtils.min(payableTo, LocalDate.now());
        if (effectiveAttendanceTo.isBefore(payableFrom)) {
            effectiveAttendanceTo = payableFrom;
        }
        BigDecimal effectivePayableDays = effectiveAttendanceTo.equals(payableTo)
                ? payableWorkingDays
                : workingDayCalculator.countWorkingDays(companyId, payableFrom, effectiveAttendanceTo);

        WorkHoursProvider.WorkHoursResult workHours = workHoursProvider.getWorkedHours(
                employee, context, payableWorkingDays, payableFrom, payableTo);
        if (DefaultWorkHoursProvider.SOURCE.equals(workHours.source())
                && employee.getPaymentMethod() == PaymentMethod.HOURLY) {
            warnings.add("Hourly pay is calculated using default working days — no attendance records found for this period.");
        }

        BigDecimal paidHolidayHours = calculatePaidHolidayTopUpHours(
                employee, context, payableFrom, effectiveAttendanceTo, workHours);
        BigDecimal payableHours = employee.getPaymentMethod() == PaymentMethod.HOURLY
                ? workHours.hours().add(paidHolidayHours)
                : workHours.hours();

        // ── Overtime ────────────────────────────────────────────────────────────
        // Computed before basePay so HOURLY base excludes OT hours (avoids double-paying them).
        OvertimeDetails overtimeDetails = calculateOvertimeDetails(
                employee, context, workHours, effectivePayableDays, paidHolidayHours);
        BigDecimal overtimePay = overtimeDetails != null
                ? overtimeDetails.overtimePay() : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // For HOURLY with overtime, base pay covers only the regular (non-OT) hours.
        // OT hours are separately compensated at overtimeRate via overtimePay.
        BigDecimal basePayableHours = (employee.getPaymentMethod() == PaymentMethod.HOURLY && overtimeDetails != null)
                ? payableHours.subtract(overtimeDetails.overtimeHours())
                : payableHours;

        BigDecimal basePay = calculateBasePay(employee, context, payableWorkingDays, basePayableHours);
        BasePayDetails basePayDetails = toBasePayDetails(employee, context, payableWorkingDays, basePayableHours, basePay);
        HourlyAttendancePaymentDetails hourlyAttendancePayment = calculateHourlyAttendancePayment(
                employee, context, effectivePayableDays, workHours, paidHolidayHours, payableHours, basePay);

        LeaveCalculationDetails leaveDetails = calculateLeave(
                employee, context, approvedLeavesInYear, approvedLeavesInMonth,
                leaveBalancesForYear, workHours.source(), warnings);

        var sickLeaveDetails = sickLeavePolicy.calculate(
                employee, sickLeaves(approvedLeavesInMonth), sickLeaves(approvedLeavesInYear), context);
        if (PlaceholderSickLeavePolicy.STATUS.equals(sickLeaveDetails.status())) {
            warnings.add("Sick leave policy is not configured for this company. Sick leave has no financial effect.");
        }

        AdjustmentDetails adjustmentDetails = calculateAdjustments(adjustments);

        FixedSalaryAttendancePaymentDetails fixedSalaryAttendancePayment = calculateFixedSalaryAttendancePayment(
                employee, context, workHours, effectivePayableDays, paidHolidayHours, overtimeDetails);

        // ── Gross earnings ────────────────────────────────────────────────────
        BigDecimal grossEarnings;
        BigDecimal unpaidLeaveAndSickDeduction;
        if (employee.getPaymentMethod() == PaymentMethod.HOURLY) {
            BigDecimal sickPaidAmount = sickLeaveDetails.companyPaidAmount() != null
                    ? sickLeaveDetails.companyPaidAmount() : BigDecimal.ZERO;
            grossEarnings = money(basePay
                    .add(leaveDetails.paidLeaveAmount())
                    .add(sickPaidAmount)
                    .add(adjustmentDetails.totalBonus())
                    .add(overtimePay));
            unpaidLeaveAndSickDeduction = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            BigDecimal sickDeduction = sickLeaveDetails.totalSickLeaveDeduction() != null
                    ? sickLeaveDetails.totalSickLeaveDeduction() : BigDecimal.ZERO;
            grossEarnings = money(basePay.add(adjustmentDetails.totalBonus()).add(overtimePay));
            unpaidLeaveAndSickDeduction = money(leaveDetails.unpaidLeaveDeduction().add(sickDeduction));
        }

        // ── Absence info (I1) — FIXED_MONTHLY only, informational ────────────
        AbsenceDetails absenceDetails = calculateAbsenceDetails(
                employee, context, workHours, effectivePayableDays, paidHolidayHours);

        // ── Statutory deductions (B2) ─────────────────────────────────────────
        CompanyPayrollSettings settings = settingsRepository.findByCompanyId(companyId).orElse(null);
        List<CompanyTaxBracket> brackets = taxBracketRepository.findAllByCompanyIdOrderByOrdinalAsc(companyId);
        boolean usedDefaults = settings == null;
        if (usedDefaults) {
            warnings.add("Statutory deductions used system defaults; configure company payroll settings.");
        }
        StatutoryDeductionDetails statutory = calculateStatutoryDeductions(
                grossEarnings, settings, brackets, warnings);

        // ── Total deductions and net pay ──────────────────────────────────────
        BigDecimal totalDeductions = money(statutory.statutoryDeductionsTotal()
                .add(adjustmentDetails.totalManualDeduction())
                .add(unpaidLeaveAndSickDeduction));
        BigDecimal netPay = money(grossEarnings.subtract(totalDeductions));

        // ── Reconciliation self-check (I3) ────────────────────────────────────
        assertReconciliation(grossEarnings, totalDeductions, netPay);

        boolean netPayNegative = netPay.signum() < 0;
        if (netPayNegative) {
            warnings.add("Net pay is negative (" + netPay + " " + currency
                    + "). Unpaid deductions exceed gross earnings.");
        }

        return new PayrollCalculationResponse(
                employee.getId(),
                employeeName(employee),
                payrollMonth.getYear(),
                payrollMonth.getMonthValue(),
                currency,
                employee.getPaymentMethod(),
                PayrollCalculationStatus.SUCCESS,
                payrollStatus,
                preview,
                new EmploymentPeriodDetails(employmentStart, employmentEnd, payableFrom, payableTo),
                new WorkPeriodDetails(
                        context.calendarDaysInMonth(),
                        context.workingDaysInMonth(),
                        payableWorkingDays,
                        employeeDailyHours(employee, context),
                        payableHours,
                        workHours.source(),
                        effectiveAttendanceTo,
                        effectivePayableDays),
                basePayDetails,
                hourlyAttendancePayment,
                fixedSalaryAttendancePayment,
                leaveDetails,
                sickLeaveDetails,
                adjustmentDetails,
                statutory,
                absenceDetails,
                overtimeDetails,
                new PayrollTotals(basePay, overtimePay, grossEarnings, statutory.statutoryDeductionsTotal(),
                        totalDeductions, netPay, netPayNegative, statutory.employerCostTotal()),
                warnings
        );
    }

    // ── Context ───────────────────────────────────────────────────────────────

    private PayrollContext buildContext(UUID companyId, YearMonth payrollMonth, String currency) {
        LocalDate start = payrollMonth.atDay(1);
        LocalDate end = payrollMonth.atEndOfMonth();
        BigDecimal dailyHours = settingsRepository.findByCompanyId(companyId)
                .map(CompanyPayrollSettings::getDefaultDailyWorkingHours)
                .orElse(DEFAULT_DAILY_HOURS);
        int workingDays = workingDayCalculator.countWorkingDays(companyId, start, end).intValue();
        return new PayrollContext(
                payrollMonth, start, end,
                payrollMonth.lengthOfMonth(),
                workingDays,
                dailyHours,
                currency
        );
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validateEmployee(Employee employee, PayrollContext context) {
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

    // ── Base pay ──────────────────────────────────────────────────────────────

    private BigDecimal calculateBasePay(Employee employee, PayrollContext context,
                                        BigDecimal payableWorkingDays, BigDecimal payableHours) {
        if (employee.getPaymentMethod() == PaymentMethod.FIXED_MONTHLY) {
            BigDecimal ratio = payableWorkingDays.divide(
                    BigDecimal.valueOf(context.workingDaysInMonth()), 8, RoundingMode.HALF_UP);
            return money(employee.getMonthlySalary().multiply(ratio));
        }
        return money(employee.getHourlyRate().multiply(payableHours));
    }

    private BasePayDetails toBasePayDetails(Employee employee, PayrollContext context,
                                            BigDecimal payableWorkingDays, BigDecimal payableHours,
                                            BigDecimal basePay) {
        if (employee.getPaymentMethod() == PaymentMethod.FIXED_MONTHLY) {
            return new BasePayDetails(
                    "monthlySalary * payableWorkingDays / workingDaysInMonth",
                    employee.getMonthlySalary(), null,
                    payableWorkingDays, context.workingDaysInMonth(),
                    null, basePay, WORKING_DAY_PRORATION);
        }
        return new BasePayDetails(
                "hourlyRate * payableHours",
                null, employee.getHourlyRate(),
                payableWorkingDays, context.workingDaysInMonth(),
                payableHours, basePay, null);
    }

    private HourlyAttendancePaymentDetails calculateHourlyAttendancePayment(
            Employee employee, PayrollContext context,
            BigDecimal effectivePayableDays, WorkHoursProvider.WorkHoursResult workHours,
            BigDecimal paidHolidayHours, BigDecimal payableHours, BigDecimal basePay
    ) {
        if (employee.getPaymentMethod() != PaymentMethod.HOURLY) {
            return null;
        }
        BigDecimal fullPayableHours = effectivePayableDays.multiply(employeeDailyHours(employee, context));
        BigDecimal fullPayment = money(employee.getHourlyRate().multiply(fullPayableHours));
        BigDecimal paymentReceived = basePay;
        BigDecimal attendanceDeduction = DefaultWorkHoursProvider.SOURCE.equals(workHours.source())
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : money(fullPayment.subtract(paymentReceived).max(BigDecimal.ZERO));
        return new HourlyAttendancePaymentDetails(
                fullPayableHours, workHours.hours(), paidHolidayHours, payableHours, fullPayment, attendanceDeduction,
                paymentReceived, workHours.source());
    }

    private FixedSalaryAttendancePaymentDetails calculateFixedSalaryAttendancePayment(
            Employee employee,
            PayrollContext context,
            WorkHoursProvider.WorkHoursResult workHours,
            BigDecimal effectivePayableDays,
            BigDecimal paidHolidayHours,
            OvertimeDetails overtimeDetails
    ) {
        if (employee.getPaymentMethod() != PaymentMethod.FIXED_MONTHLY) {
            return null;
        }
        BigDecimal expectedHours = effectivePayableDays.multiply(employeeDailyHours(employee, context));
        BigDecimal payableAttendanceHours = workHours.hours().add(paidHolidayHours);
        BigDecimal equivalentHourlyRate = fixedSalaryHourlyRate(employee, context);
        BigDecimal attendanceEquivalentPay = DefaultWorkHoursProvider.SOURCE.equals(workHours.source())
                ? null
                : money(payableAttendanceHours.multiply(equivalentHourlyRate));
        BigDecimal excessHours = overtimeDetails != null
                ? overtimeDetails.overtimeHours()
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal excessHourlyRate = overtimeDetails != null
                ? overtimeDetails.overtimeHourlyRate()
                : null;
        BigDecimal excessPay = overtimeDetails != null
                ? overtimeDetails.overtimePay()
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return new FixedSalaryAttendancePaymentDetails(
                money(expectedHours),
                workHours.hours(),
                paidHolidayHours,
                payableAttendanceHours,
                attendanceEquivalentPay,
                employee.getMonthlySalary(),
                excessHours,
                excessHourlyRate,
                excessPay,
                workHours.source());
    }

    // ── Absence reporting (I1) ────────────────────────────────────────────────

    private AbsenceDetails calculateAbsenceDetails(Employee employee, PayrollContext context,
                                                    WorkHoursProvider.WorkHoursResult workHours,
                                                    BigDecimal effectivePayableDays,
                                                    BigDecimal paidHolidayHours) {
        if (employee.getPaymentMethod() != PaymentMethod.FIXED_MONTHLY) {
            return null;
        }
        BigDecimal dailyHoursVal = employeeDailyHours(employee, context);
        BigDecimal expectedMinutes = effectivePayableDays
                .multiply(dailyHoursVal)
                .multiply(BigDecimal.valueOf(60));
        BigDecimal attendedMinutes = workHours.hours().add(paidHolidayHours).multiply(BigDecimal.valueOf(60));
        BigDecimal absentMinutes = expectedMinutes.subtract(attendedMinutes).max(BigDecimal.ZERO);
        BigDecimal dailyPay = employee.getMonthlySalary()
                .divide(BigDecimal.valueOf(context.workingDaysInMonth()), 8, RoundingMode.HALF_UP);
        BigDecimal absentDays = absentMinutes.divide(dailyHoursVal.multiply(BigDecimal.valueOf(60)),
                8, RoundingMode.HALF_UP);
        BigDecimal monetaryEquivalent = money(dailyPay.multiply(absentDays));
        return new AbsenceDetails(expectedMinutes, attendedMinutes, absentMinutes, monetaryEquivalent, false);
    }

    // ── Leave calculation (B5) ────────────────────────────────────────────────

    private LeaveCalculationDetails calculateLeave(
            Employee employee,
            PayrollContext context,
            List<LeaveRequest> approvedLeavesInYear,
            List<LeaveRequest> approvedLeavesInMonth,
            List<LeaveBalance> leaveBalancesForYear,
            String workHoursSource,
            List<String> warnings
    ) {
        UUID companyId = employee.getCompany().getId();
        Map<LeaveType, BigDecimal> remainingBalance = buildRemainingBalanceMap(leaveBalancesForYear);

        BigDecimal totalPaidLeaveAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalUnpaidDays = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalUnpaidDeduction = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPaidDays = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalLeaveDays = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal dailyValue = dailyPayValue(employee, context);
        List<PayrollLeaveRecordDetails> records = new ArrayList<>();

        List<LeaveRequest> sortedInMonth = approvedLeavesInMonth.stream()
                .filter(l -> l.getLeaveType() != LeaveType.SICK)
                .sorted(Comparator.comparing(LeaveRequest::getStartDate))
                .toList();

        for (LeaveRequest leave : sortedInMonth) {
            LocalDate overlapFrom = PayrollDateUtils.max(leave.getStartDate(), context.periodStart());
            LocalDate overlapTo = PayrollDateUtils.min(leave.getEndDate(), context.periodEnd());
            BigDecimal daysInPeriod = workingDayCalculator.countWorkingDays(companyId, overlapFrom, overlapTo);
            if (daysInPeriod.signum() == 0) {
                continue;
            }
            totalLeaveDays = totalLeaveDays.add(daysInPeriod);

            LeaveType type = leave.getLeaveType();
            PayrollLeaveTreatment treatment;
            BigDecimal paidDays = BigDecimal.ZERO;
            BigDecimal unpaidDays = BigDecimal.ZERO;

            switch (type) {
                case UNPAID -> {
                    treatment = PayrollLeaveTreatment.UNPAID_EXPLICIT;
                    unpaidDays = daysInPeriod;
                }
                case MATERNITY -> {
                    treatment = PayrollLeaveTreatment.STATUTORY_MATERNITY;
                    // statutory; no cost to leave pool, no deduction
                }
                case PATERNITY -> {
                    treatment = PayrollLeaveTreatment.STATUTORY_PATERNITY;
                    // statutory; no cost to leave pool, no deduction
                }
                default -> {
                    // VACATION, PERSONAL, OTHER — paid from per-type LeaveBalance
                    BigDecimal remaining = remainingBalance.getOrDefault(type, BigDecimal.ZERO);
                    paidDays = daysInPeriod.min(remaining);
                    unpaidDays = daysInPeriod.subtract(paidDays).max(BigDecimal.ZERO);
                    // Consume from the running balance map
                    remainingBalance.put(type, remaining.subtract(paidDays).max(BigDecimal.ZERO));
                    treatment = unpaidDays.signum() > 0
                            ? PayrollLeaveTreatment.UNPAID_EXCESS
                            : PayrollLeaveTreatment.PAID_FROM_BALANCE;
                }
            }

            if (employee.getPaymentMethod() == PaymentMethod.HOURLY) {
                if (paidDays.signum() > 0 && !DefaultWorkHoursProvider.SOURCE.equals(workHoursSource)) {
                    totalPaidLeaveAmount = totalPaidLeaveAmount.add(money(dailyValue.multiply(paidDays)));
                    totalPaidDays = totalPaidDays.add(paidDays);
                }
            } else {
                totalPaidDays = totalPaidDays.add(paidDays);
            }

            if (unpaidDays.signum() > 0) {
                totalUnpaidDays = totalUnpaidDays.add(unpaidDays);
                totalUnpaidDeduction = totalUnpaidDeduction.add(money(dailyValue.multiply(unpaidDays)));
            }

            records.add(new PayrollLeaveRecordDetails(
                    leave.getId(), type.name(),
                    leave.getStartDate(), leave.getEndDate(),
                    daysInPeriod, treatment.name()));
        }

        BigDecimal reportedUnpaidDeduction = employee.getPaymentMethod() == PaymentMethod.HOURLY
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : totalUnpaidDeduction;
        return new LeaveCalculationDetails(
                0, // annualPaidLeaveAllowanceDays deprecated — now per-type via LeaveBalance
                BigDecimal.ZERO, // usedBefore — now tracked in LeaveBalance
                totalLeaveDays,
                totalPaidDays,
                totalPaidLeaveAmount,
                totalUnpaidDays,
                reportedUnpaidDeduction,
                records
        );
    }

    private Map<LeaveType, BigDecimal> buildRemainingBalanceMap(List<LeaveBalance> balances) {
        // remaining = totalDays - usedDays for each type
        return balances.stream().collect(Collectors.toMap(
                LeaveBalance::getLeaveType,
                lb -> lb.getTotalDays().subtract(lb.getUsedDays()).max(BigDecimal.ZERO),
                (a, b) -> a));
    }

    // ── Statutory deductions (B2) ─────────────────────────────────────────────

    private StatutoryDeductionDetails calculateStatutoryDeductions(
            BigDecimal grossEarnings,
            CompanyPayrollSettings settings,
            List<CompanyTaxBracket> brackets,
            List<String> warnings
    ) {
        BigDecimal ssEmployeeRate = settings != null ? settings.getSocialSecurityEmployeeRate() : BigDecimal.ZERO;
        BigDecimal ssEmployerRate = settings != null ? settings.getSocialSecurityEmployerRate() : BigDecimal.ZERO;
        BigDecimal pensionEmployeeRate = settings != null ? settings.getPensionEmployeeRate() : BigDecimal.ZERO;
        BigDecimal pensionEmployerRate = settings != null ? settings.getPensionEmployerRate() : BigDecimal.ZERO;
        BigDecimal minBase = settings != null ? settings.getContributionMinBase() : null;
        BigDecimal maxBase = settings != null ? settings.getContributionMaxBase() : null;
        TaxBase taxBase = settings != null ? settings.getTaxBase() : TaxBase.GROSS_MINUS_CONTRIBUTIONS;
        boolean taxEnabled = settings == null || settings.isTaxEnabled();

        BigDecimal ssBase = clamp(grossEarnings, minBase, maxBase);
        BigDecimal pensionBase = clamp(grossEarnings, minBase, maxBase);

        if (minBase != null && grossEarnings.compareTo(minBase) < 0) {
            warnings.add("Gross earnings (" + grossEarnings + ") are below the contribution minimum base ("
                    + minBase + "). Contributions are calculated on the minimum base and may exceed gross pay.");
        }

        BigDecimal employeeSS = money(ssBase.multiply(rate(ssEmployeeRate)));
        BigDecimal employeePension = money(pensionBase.multiply(rate(pensionEmployeeRate)));
        BigDecimal employerSS = money(ssBase.multiply(rate(ssEmployerRate)));
        BigDecimal employerPension = money(pensionBase.multiply(rate(pensionEmployerRate)));

        BigDecimal taxableIncome;
        if (taxBase == TaxBase.GROSS_MINUS_CONTRIBUTIONS) {
            taxableIncome = grossEarnings.subtract(employeeSS).subtract(employeePension)
                    .max(BigDecimal.ZERO);
        } else {
            taxableIncome = grossEarnings;
        }

        BigDecimal incomeTax;
        List<TaxBracketCalculationLine> bracketBreakdown;
        if (!taxEnabled) {
            incomeTax = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            bracketBreakdown = List.of();
        } else if (brackets.isEmpty()) {
            incomeTax = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            bracketBreakdown = List.of();
            warnings.add("No tax brackets configured; income tax is 0. Configure company tax brackets.");
        } else {
            bracketBreakdown = computeBracketBreakdown(taxableIncome, brackets);
            incomeTax = money(bracketBreakdown.stream()
                    .map(TaxBracketCalculationLine::taxAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }

        BigDecimal statutoryTotal = money(employeeSS.add(employeePension).add(incomeTax));
        BigDecimal employerCostTotal = money(grossEarnings.add(employerSS).add(employerPension));

        return new StatutoryDeductionDetails(
                ssBase, pensionBase, money(taxableIncome),
                employeeSS, employeePension, incomeTax, statutoryTotal,
                employerSS, employerPension, employerCostTotal,
                bracketBreakdown, settings == null
        );
    }

    private List<TaxBracketCalculationLine> computeBracketBreakdown(
            BigDecimal taxable, List<CompanyTaxBracket> brackets) {
        List<TaxBracketCalculationLine> lines = new ArrayList<>();
        for (CompanyTaxBracket bracket : brackets) {
            BigDecimal lo = bracket.getLowerBound();
            BigDecimal hi = bracket.getUpperBound(); // null = open-ended
            BigDecimal sliceEnd = hi == null ? taxable : taxable.min(hi);
            BigDecimal slice = sliceEnd.subtract(lo).max(BigDecimal.ZERO);
            BigDecimal taxAmount = slice.multiply(rate(bracket.getRate()))
                    .setScale(8, RoundingMode.HALF_UP); // intermediate; round at end
            lines.add(new TaxBracketCalculationLine(lo, hi, bracket.getRate(), slice, taxAmount));
        }
        return lines;
    }

    private BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        BigDecimal result = value;
        if (min != null && result.compareTo(min) < 0) {
            result = min;
        }
        if (max != null && result.compareTo(max) > 0) {
            result = max;
        }
        return result;
    }

    private BigDecimal rate(BigDecimal percentage) {
        return percentage.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
    }

    // ── Reconciliation check (I3) ─────────────────────────────────────────────

    private void assertReconciliation(BigDecimal grossEarnings, BigDecimal totalDeductions, BigDecimal netPay) {
        BigDecimal expected = grossEarnings.subtract(totalDeductions);
        if (expected.compareTo(netPay) != 0) {
            throw new PayrollCalculationException("PAYROLL_RECONCILIATION_FAILED",
                    "Payroll reconciliation failed: gross - deductions = "
                            + expected + " but netPay = " + netPay);
        }
    }

    // ── Adjustments ───────────────────────────────────────────────────────────

    private AdjustmentDetails calculateAdjustments(List<PayrollAdjustment> adjustments) {
        List<PayrollAdjustmentLine> bonuses = adjustments.stream()
                .filter(a -> a.getType() == PayrollAdjustmentType.BONUS)
                .map(this::toAdjustmentLine).toList();
        List<PayrollAdjustmentLine> deductions = adjustments.stream()
                .filter(a -> a.getType() == PayrollAdjustmentType.DEDUCTION)
                .map(this::toAdjustmentLine).toList();
        BigDecimal totalBonus = money(bonuses.stream().map(PayrollAdjustmentLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal totalDeduction = money(deductions.stream().map(PayrollAdjustmentLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return new AdjustmentDetails(bonuses, deductions, totalBonus, totalDeduction);
    }

    private PayrollAdjustmentLine toAdjustmentLine(PayrollAdjustment a) {
        return new PayrollAdjustmentLine(a.getId(), a.getAmount(), a.getReason(), a.getNotes());
    }

    // ── Overtime ──────────────────────────────────────────────────────────────

    private OvertimeDetails calculateOvertimeDetails(
            Employee employee, PayrollContext context,
            WorkHoursProvider.WorkHoursResult workHours,
            BigDecimal effectivePayableDays,
            BigDecimal paidHolidayHours
    ) {
        // No overtime from default/estimated hours — only real attendance data
        if (DefaultWorkHoursProvider.SOURCE.equals(workHours.source())) {
            return null;
        }
        BigDecimal overtimeRate = employee.getOvertimeHourlyRate();
        if (overtimeRate == null || overtimeRate.signum() <= 0) {
            if (employee.getPaymentMethod() != PaymentMethod.FIXED_MONTHLY) {
                return null;
            }
            overtimeRate = fixedSalaryHourlyRate(employee, context);
        }
        BigDecimal effectiveDailyHours = employeeDailyHours(employee, context);
        BigDecimal expectedHours = money(effectivePayableDays.multiply(effectiveDailyHours));
        BigDecimal workedHours = workHours.hours().add(paidHolidayHours);
        BigDecimal overtimeHours = workedHours.subtract(expectedHours).max(BigDecimal.ZERO);
        if (overtimeHours.signum() == 0) {
            return null;
        }
        BigDecimal overtimePay = money(overtimeHours.multiply(overtimeRate));
        return new OvertimeDetails(expectedHours, workedHours, overtimeHours, overtimeRate, overtimePay);
    }

    // ── Daily pay value ───────────────────────────────────────────────────────

    private BigDecimal dailyPayValue(Employee employee, PayrollContext context) {
        if (employee.getPaymentMethod() == PaymentMethod.FIXED_MONTHLY) {
            return employee.getMonthlySalary()
                    .divide(BigDecimal.valueOf(context.workingDaysInMonth()), 8, RoundingMode.HALF_UP);
        }
        return employee.getHourlyRate().multiply(employeeDailyHours(employee, context));
    }

    private BigDecimal calculatePaidHolidayTopUpHours(
            Employee employee,
            PayrollContext context,
            LocalDate payableFrom,
            LocalDate effectiveAttendanceTo,
            WorkHoursProvider.WorkHoursResult workHours
    ) {
        if (DefaultWorkHoursProvider.SOURCE.equals(workHours.source())) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal paidHolidayDefaultHours = workingDayCalculator
                .countPaidHolidays(employee.getCompany().getId(), payableFrom, effectiveAttendanceTo)
                .multiply(employeeDailyHours(employee, context));
        BigDecimal paidHolidayWorkedHours = workHours.paidHolidayWorkedHours() != null
                ? workHours.paidHolidayWorkedHours()
                : BigDecimal.ZERO;
        return paidHolidayDefaultHours.subtract(paidHolidayWorkedHours)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal employeeDailyHours(Employee employee, PayrollContext context) {
        return employee.getDailyWorkingHours() != null && employee.getDailyWorkingHours().signum() > 0
                ? employee.getDailyWorkingHours()
                : context.defaultDailyWorkingHours();
    }

    private BigDecimal fixedSalaryHourlyRate(Employee employee, PayrollContext context) {
        BigDecimal monthlyHours = BigDecimal.valueOf(context.workingDaysInMonth())
                .multiply(employeeDailyHours(employee, context));
        if (monthlyHours.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return employee.getMonthlySalary().divide(monthlyHours, 8, RoundingMode.HALF_UP);
    }

    // ── Sick leave helper ─────────────────────────────────────────────────────

    private List<LeaveRequest> sickLeaves(List<LeaveRequest> leaves) {
        return leaves.stream().filter(l -> l.getLeaveType() == LeaveType.SICK).toList();
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private boolean positive(BigDecimal amount) {
        return amount != null && amount.signum() > 0;
    }

    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String employeeName(Employee employee) {
        if (employee.getUser().getDisplayName() != null
                && !employee.getUser().getDisplayName().isBlank()) {
            return employee.getUser().getDisplayName();
        }
        return (employee.getUser().getFirstName() + " " + employee.getUser().getLastName()).trim();
    }
}
