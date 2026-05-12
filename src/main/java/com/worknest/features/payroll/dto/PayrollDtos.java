package com.worknest.features.payroll.dto;

import com.worknest.domain.enums.PaymentMethod;
import com.worknest.domain.enums.PayrollAdjustmentType;
import com.worknest.domain.enums.PayrollCalculationStatus;
import com.worknest.domain.enums.PayrollStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
            LeaveCalculationDetails leaveCalculation,
            SickLeaveCalculationDetails sickLeaveCalculation,
            AdjustmentDetails adjustments,
            PayrollTotals totals,
            List<String> warnings
    ) {
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
            String workHoursSource
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

    public record LeaveCalculationDetails(
            int annualPaidLeaveAllowanceDays,
            BigDecimal usedPaidLeaveBeforeThisMonth,
            BigDecimal leaveTakenThisMonth,
            BigDecimal paidLeaveDaysThisMonth,
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
            BigDecimal companyPaidPercentage,
            BigDecimal companyPaidAmount,
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
            BigDecimal grossEarnings,
            BigDecimal totalDeductions,
            BigDecimal netPay,
            boolean netPayNegative
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
            @DecimalMin(value = "0.00", message = "companyPaidPercentage must be >= 0")
            @DecimalMax(value = "100.00", message = "companyPaidPercentage must be <= 100")
            BigDecimal companyPaidPercentage,

            @NotNull(message = "maxCompanyPaidDays is required")
            @Min(value = 0, message = "maxCompanyPaidDays must be >= 0")
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
}
