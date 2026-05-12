package com.worknest.features.payroll.application;

import com.worknest.features.payroll.dto.PayrollDtos.BatchPayrollCalculationRequest;
import com.worknest.features.payroll.dto.PayrollDtos.BatchPayrollCalculationResponse;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollAdjustmentRequest;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollAdjustmentResponse;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollCalculationResponse;
import com.worknest.features.payroll.dto.PayrollDtos.SickLeavePolicyResponse;
import com.worknest.features.payroll.dto.PayrollDtos.UpsertSickLeavePolicyRequest;
import java.util.UUID;

public interface PayrollService {

    PayrollAdjustmentResponse addBonus(UUID employeeId, PayrollAdjustmentRequest request);

    PayrollAdjustmentResponse addDeduction(UUID employeeId, PayrollAdjustmentRequest request);

    PayrollCalculationResponse previewEmployeePayroll(UUID employeeId, int year, int month);

    PayrollCalculationResponse previewCurrentEmployeePayroll(int year, int month);

    PayrollCalculationResponse calculateEmployeePayroll(UUID employeeId, int year, int month);

    BatchPayrollCalculationResponse calculateBatch(BatchPayrollCalculationRequest request);

    SickLeavePolicyResponse getSickLeavePolicy();

    SickLeavePolicyResponse upsertSickLeavePolicy(UpsertSickLeavePolicyRequest request);
}
