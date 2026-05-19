package com.worknest.features.payroll.application;

import com.worknest.common.api.PaginatedResponse;
import com.worknest.features.payroll.dto.PayrollDtos.BatchPayrollCalculationRequest;
import com.worknest.features.payroll.dto.PayrollDtos.BatchPayrollCalculationResponse;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollAdjustmentRequest;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollAdjustmentResponse;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollCalculationResponse;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollEmployeeSummaryResponse;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollMonthSummary;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollPeriodRequest;
import com.worknest.features.payroll.dto.PayrollDtos.SickLeavePolicyResponse;
import com.worknest.features.payroll.dto.PayrollDtos.UpsertSickLeavePolicyRequest;
import java.util.List;
import java.util.UUID;

public interface PayrollService {

    PayrollAdjustmentResponse addBonus(UUID employeeId, PayrollAdjustmentRequest request);

    PayrollAdjustmentResponse addDeduction(UUID employeeId, PayrollAdjustmentRequest request);

    PayrollCalculationResponse previewEmployeePayroll(UUID employeeId, int year, int month);

    PayrollCalculationResponse previewCurrentEmployeePayroll(int year, int month);

    PaginatedResponse<PayrollEmployeeSummaryResponse> listAdminPayrollEmployees(
            int year, int month, String search, Integer page, Integer size);

    PayrollCalculationResponse calculateEmployeePayroll(UUID employeeId, int year, int month);

    BatchPayrollCalculationResponse calculateBatch(BatchPayrollCalculationRequest request);

    /** CALCULATED → APPROVED */
    PayrollCalculationResponse approvePayroll(UUID employeeId, PayrollPeriodRequest request);

    /** APPROVED → FINALIZED */
    PayrollCalculationResponse finalizePayroll(UUID employeeId, PayrollPeriodRequest request);

    /**
     * FINALIZED → PAID.
     */
    PayrollCalculationResponse completePayment(UUID employeeId, PayrollPeriodRequest request);

    /** APPROVED → CALCULATED (unlocks attendance records) */
    PayrollCalculationResponse revertApproval(UUID employeeId, PayrollPeriodRequest request);

    /** FINALIZED → APPROVED */
    PayrollCalculationResponse revertFinalization(UUID employeeId, PayrollPeriodRequest request);

    /** PAID → FINALIZED */
    PayrollCalculationResponse revertPayment(UUID employeeId, PayrollPeriodRequest request);

    SickLeavePolicyResponse getSickLeavePolicy();

    SickLeavePolicyResponse upsertSickLeavePolicy(UpsertSickLeavePolicyRequest request);

    /** Returns all persisted payroll periods for the current authenticated employee, newest first. */
    List<PayrollMonthSummary> listMyPayrollHistory();
}
