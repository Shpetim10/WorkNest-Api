package com.worknest.features.payroll.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.worknest.domain.enums.EmploymentType;
import com.worknest.domain.enums.PaymentMethod;
import com.worknest.domain.enums.PayrollAdjustmentType;
import com.worknest.domain.enums.PayrollCalculationStatus;
import com.worknest.domain.enums.PayrollStatus;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.domain.enums.TaxBase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class PayrollDtos {

    private PayrollDtos() {
    }

    public record PayrollPeriodRequest(
            @Min(value = 1900, message = "Year must be 1900 or later")
            int year,

            @Min(value = 1, message = "Month must be between 1 and 12")
            @Max(value = 12, message = "Month must be between 1 and 12")
            int month
    ) {
    }

    public record BatchPayrollCalculationRequest(
            @Min(value = 1900, message = "Year must be 1900 or later")
            int year,

            @Min(value = 1, message = "Month must be between 1 and 12")
            @Max(value = 12, message = "Month must be between 1 and 12")
            int month,

            List<UUID> employeeIds
    ) {
    }

    public record PayrollAdjustmentRequest(
            @Min(value = 1900, message = "Year must be 1900 or later")
            int year,

            @Min(value = 1, message = "Month must be between 1 and 12")
            @Max(value = 12, message = "Month must be between 1 and 12")
            int month,

            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be positive")
            BigDecimal amount,

            @NotBlank(message = "Reason is required")
            @Size(max = 300, message = "Reason cannot exceed 300 characters")
            String reason,

            @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
            String notes
    ) {
    }

    public record PayrollAdjustmentResponse(
            UUID id,
            UUID employeeId,
            int year,
            int month,
            PayrollAdjustmentType type,
            BigDecimal amount,
            String reason,
            String notes,
            UUID createdByUserId,
            Instant createdAt
    ) {
    }

    public record PayrollCalculationResponse(
            UUID employeeId,
            String employeeName,
            int year,
            int month,
            String currency,
            PaymentMethod paymentMethod,
            PayrollCalculationStatus calculationStatus,
            PayrollStatus payrollStatus,
            boolean preview,
            EmploymentPeriodDetails employmentPeriod,
            WorkPeriodDetails workPeriod,
            BasePayDetails basePayCalculation,
            HourlyAttendancePaymentDetails hourlyAttendancePayment,
            FixedSalaryAttendancePaymentDetails fixedSalaryAttendancePayment,
            LeaveCalculationDetails leaveCalculation,
            SickLeaveCalculationDetails sickLeaveCalculation,
            ParentalLeaveCalculationDetails parentalLeaveCalculation,
            AdjustmentDetails adjustments,
            StatutoryDeductionDetails statutoryDeductions,
            AbsenceDetails absenceDetails,
            OvertimeDetails overtimeDetails,
            PayrollTotals totals,
            List<String> warnings
    ) {
    }

    public record PayrollEmployeeSummaryResponse(
            UUID employeeId,
            String employeeName,
            PlatformRole employeeRole,
            EmploymentType employmentType,
            PaymentMethod paymentMethod,
            BigDecimal monthlySalary,
            BigDecimal hourlyRate,
            String currency,
            PayrollStatus payrollStatus,
            PayrollCalculationStatus calculationStatus,
            boolean preview,
            BigDecimal basePay,
            BigDecimal overtimePay,
            BigDecimal totalBonus,
            BigDecimal totalManualDeduction,
            BigDecimal hourlyFullPayment,
            BigDecimal attendanceDeduction,
            BigDecimal attendancePaymentReceived,
            BigDecimal grossEarnings,
            BigDecimal totalDeductions,
            BigDecimal netPay,
            boolean netPayNegative,
            BigDecimal incomeTax,
            BigDecimal employeeSocialSecurity,
            BigDecimal employeePension,
            BigDecimal employerCostTotal,
            List<String> warnings
    ) {
        @JsonProperty("status")
        public PayrollStatus status() {
            return payrollStatus;
        }

        @JsonProperty("deductions")
        public BigDecimal deductions() {
            return totalManualDeduction;
        }
    }

    public record EmploymentPeriodDetails(
            LocalDate employmentStartDate,
            LocalDate employmentEndDate,
            LocalDate payableFrom,
            LocalDate payableTo
    ) {
    }

    public record WorkPeriodDetails(
            int calendarDaysInMonth,
            int workingDaysInMonth,
            BigDecimal payableWorkingDays,
            BigDecimal defaultDailyWorkingHours,
            BigDecimal payableHours,
            String workHoursSource,
            /** Last date through which attendance was (or will be) summed.
             *  Equals payableTo for past months; equals today for the current month. */
            LocalDate effectiveAttendanceTo,
            /** Working days from payableFrom to effectiveAttendanceTo.
             *  Used to compute the "full-attendance expected" hours for the elapsed period. */
            BigDecimal effectivePayableWorkingDays
    ) {
    }

    public record BasePayDetails(
            String formula,
            BigDecimal monthlySalary,
            BigDecimal hourlyRate,
            BigDecimal payableWorkingDays,
            int workingDaysInMonth,
            BigDecimal payableHours,
            BigDecimal basePay,
            String prorationMethod
    ) {
    }

    public record HourlyAttendancePaymentDetails(
            BigDecimal fullPayableHours,
            BigDecimal attendedHours,
            BigDecimal paidHolidayHours,
            BigDecimal payableHours,
            BigDecimal fullPayment,
            BigDecimal attendanceDeduction,
            BigDecimal paymentReceived,
            String workHoursSource
    ) {
    }

    public record FixedSalaryAttendancePaymentDetails(
            BigDecimal expectedHours,
            BigDecimal attendedHours,
            BigDecimal paidHolidayHours,
            BigDecimal payableAttendanceHours,
            BigDecimal attendanceEquivalentPay,
            BigDecimal baseSalary,
            BigDecimal excessHours,
            BigDecimal excessHourlyRate,
            BigDecimal excessPay,
            String workHoursSource
    ) {
    }

    public record LeaveCalculationDetails(
            int annualPaidLeaveAllowanceDays,
            BigDecimal usedPaidLeaveBeforeThisMonth,
            BigDecimal leaveTakenThisMonth,
            BigDecimal paidLeaveDaysThisMonth,
            BigDecimal paidLeaveAmount,
            BigDecimal unpaidLeaveDaysThisMonth,
            BigDecimal unpaidLeaveDeduction,
            List<PayrollLeaveRecordDetails> leaveRecordsIncluded
    ) {
    }

    public record PayrollLeaveRecordDetails(
            UUID id,
            String leaveType,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal daysCountedInPayroll,
            String payrollTreatment
    ) {
    }

    public record SickLeaveCalculationDetails(
            BigDecimal daysTakenThisMonth,
            BigDecimal companyPaidDays,
            BigDecimal unpaidSickLeaveDays,
            BigDecimal companyPaidPercentage,
            BigDecimal companyPaidAmount,
            BigDecimal paidSickLeaveDeductionEquivalent,
            BigDecimal totalSickLeaveDeduction,
            BigDecimal paidSickLeaveHours,
            BigDecimal unpaidSickLeaveHours,
            BigDecimal unpaidSickLeaveUnpaidAmount,
            BigDecimal insuranceCoveredDays,
            BigDecimal insuranceCoveredAmount,
            String status
    ) {
    }

    public record ParentalLeaveCalculationDetails(
            BigDecimal daysTakenThisMonth,
            BigDecimal companyPaidDays,
            BigDecimal statePaidDays,
            BigDecimal companyPaidPercentage,
            BigDecimal companyPaidAmount,
            BigDecimal paidParentalLeaveDeductionEquivalent,
            BigDecimal totalParentalLeaveDeduction,
            BigDecimal paidParentalLeaveHours,
            BigDecimal stateParentalLeaveHours,
            BigDecimal stateParentalLeaveAmount,
            BigDecimal insuranceCoveredDays,
            BigDecimal insuranceCoveredAmount,
            String status
    ) {
    }

    public record AdjustmentDetails(
            List<PayrollAdjustmentLine> bonuses,
            List<PayrollAdjustmentLine> deductions,
            BigDecimal totalBonus,
            BigDecimal totalManualDeduction
    ) {
    }

    public record PayrollAdjustmentLine(
            UUID id,
            BigDecimal amount,
            String reason,
            String notes
    ) {
    }

    public record PayrollTotals(
            BigDecimal basePay,
            BigDecimal overtimePay,
            BigDecimal grossEarnings,
            BigDecimal statutoryDeductions,
            BigDecimal totalDeductions,
            BigDecimal netPay,
            boolean netPayNegative,
            BigDecimal employerCostTotal
    ) {
    }

    public record BatchPayrollCalculationResponse(
            int year,
            int month,
            int totalEmployees,
            int successfulCalculations,
            int failedCalculations,
            int skippedCalculations,
            List<BatchPayrollEmployeeResult> results
    ) {
    }

    public record BatchPayrollEmployeeResult(
            UUID employeeId,
            PayrollCalculationStatus status,
            BigDecimal grossEarnings,
            String errorCode,
            String message
    ) {
    }

    public record UpsertSickLeavePolicyRequest(
            @NotNull(message = "companyPaidPercentage is required")
            @DecimalMin(value = "0.01", message = "companyPaidPercentage must be > 0")
            @DecimalMax(value = "100.00", message = "companyPaidPercentage must be <= 100")
            BigDecimal companyPaidPercentage,

            @NotNull(message = "maxCompanyPaidDays is required")
            @Min(value = 1, message = "maxCompanyPaidDays must be >= 1")
            @Max(value = 365, message = "maxCompanyPaidDays must be <= 365")
            Integer maxCompanyPaidDays
    ) {
    }

    public record SickLeavePolicyResponse(
            BigDecimal companyPaidPercentage,
            int maxCompanyPaidDays,
            boolean isDefault
    ) {
    }

    public record UpsertParentalLeavePolicyRequest(
            @NotNull(message = "companyPaidPercentage is required")
            @DecimalMin(value = "0.01", message = "companyPaidPercentage must be > 0")
            @DecimalMax(value = "100.00", message = "companyPaidPercentage must be <= 100")
            BigDecimal companyPaidPercentage,

            @NotNull(message = "maxCompanyPaidDays is required")
            @Min(value = 1, message = "maxCompanyPaidDays must be >= 1")
            @Max(value = 365, message = "maxCompanyPaidDays must be <= 365")
            Integer maxCompanyPaidDays
    ) {
    }

    public record ParentalLeavePolicyResponse(
            BigDecimal companyPaidPercentage,
            int maxCompanyPaidDays,
            boolean isDefault
    ) {
    }

    // ── Statutory deductions ─────────────────────────────────────────────────

    public record StatutoryDeductionDetails(
            BigDecimal socialSecurityBase,
            BigDecimal pensionBase,
            BigDecimal taxableIncome,
            BigDecimal employeeSocialSecurity,
            BigDecimal employeePensionContribution,
            BigDecimal incomeTax,
            BigDecimal statutoryDeductionsTotal,
            BigDecimal employerSocialSecurity,
            BigDecimal employerPensionContribution,
            BigDecimal employerCostTotal,
            List<TaxBracketCalculationLine> bracketBreakdown,
            boolean usedSystemDefaults
    ) {
    }

    public record TaxBracketCalculationLine(
            BigDecimal lowerBound,
            BigDecimal upperBound,
            BigDecimal rate,
            BigDecimal taxableSlice,
            BigDecimal taxAmount
    ) {
    }

    // ── Overtime (applicable to both HOURLY and FIXED_MONTHLY) ──────────────

    public record OvertimeDetails(
            BigDecimal expectedHours,
            BigDecimal workedHours,
            BigDecimal overtimeHours,
            BigDecimal overtimeHourlyRate,
            BigDecimal overtimePay
    ) {
    }

    // ── Employee payroll history summary ─────────────────────────────────────

    public record PayrollMonthSummary(
            int year,
            int month,
            PayrollStatus status,
            BigDecimal grossEarnings,
            BigDecimal netPay,
            String currency
    ) {
    }

    // ── Absence reporting (I1 – FIXED_MONTHLY only, informational) ───────────

    public record AbsenceDetails(
            BigDecimal expectedWorkingMinutes,
            BigDecimal attendedMinutes,
            BigDecimal absentMinutes,
            BigDecimal monetaryEquivalent,
            boolean applied
    ) {
    }

    // ── Leave treatment classification (B5) ──────────────────────────────────

    public enum PayrollLeaveTreatment {
        PAID_FROM_BALANCE,
        UNPAID_EXCESS,
        UNPAID_EXPLICIT,
        SICK_COMPANY_POLICY,
        PARENTAL_COMPANY_POLICY
    }

    // ── Settings DTOs (§3) ───────────────────────────────────────────────────

    public record PayrollSettingsResponse(
            BigDecimal defaultDailyWorkingHours,
            List<String> weekendDays,
            boolean taxEnabled,
            TaxBase taxBase,
            BigDecimal socialSecurityEmployeeRate,
            BigDecimal socialSecurityEmployerRate,
            BigDecimal pensionEmployeeRate,
            BigDecimal pensionEmployerRate,
            BigDecimal contributionMinBase,
            BigDecimal contributionMaxBase,
            SickLeavePolicyResponse sickLeavePolicy,
            ParentalLeavePolicyResponse parentalLeavePolicy,
            boolean isDefault
    ) {
    }

    public record UpsertPayrollSettingsRequest(
            @NotNull @DecimalMin("0.5") @DecimalMax("24.0")
            BigDecimal defaultDailyWorkingHours,

            @NotEmpty
            List<String> weekendDays,

            @NotNull
            Boolean taxEnabled,

            @NotNull
            TaxBase taxBase,

            @NotNull @DecimalMin("0") @DecimalMax("100")
            BigDecimal socialSecurityEmployeeRate,

            @NotNull @DecimalMin("0") @DecimalMax("100")
            BigDecimal socialSecurityEmployerRate,

            @NotNull @DecimalMin("0") @DecimalMax("100")
            BigDecimal pensionEmployeeRate,

            @NotNull @DecimalMin("0") @DecimalMax("100")
            BigDecimal pensionEmployerRate,

            BigDecimal contributionMinBase,

            BigDecimal contributionMaxBase
    ) {
    }

    public record TaxBracketRequest(
            @NotNull @DecimalMin("0") BigDecimal lowerBound,
            BigDecimal upperBound,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal rate
    ) {
    }

    public record TaxBracketResponse(
            UUID id,
            int ordinal,
            BigDecimal lowerBound,
            BigDecimal upperBound,
            BigDecimal rate
    ) {
    }

    public record ReplaceTaxBracketsRequest(
            @NotNull @Valid
            List<TaxBracketRequest> brackets
    ) {
    }

    // ── Public holiday DTOs (§2.1) ────────────────────────────────────────────

    public record PublicHolidayRequest(
            @NotNull LocalDate date,
            @NotBlank @Size(max = 150) String name,
            @NotNull Boolean recurring,
            @NotNull Boolean paid
    ) {
    }

    public record PublicHolidayResponse(
            UUID id,
            LocalDate date,
            String name,
            boolean recurring,
            boolean paid
    ) {
    }
}
