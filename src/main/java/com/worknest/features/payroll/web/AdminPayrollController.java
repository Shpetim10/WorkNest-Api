package com.worknest.features.payroll.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.payroll.application.PayrollService;
import com.worknest.features.payroll.dto.PayrollDtos.BatchPayrollCalculationRequest;
import com.worknest.features.payroll.dto.PayrollDtos.BatchPayrollCalculationResponse;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollAdjustmentRequest;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollAdjustmentResponse;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollCalculationResponse;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollPeriodRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/payroll")
@Tag(name = "Admin Payroll", description = "Payroll calculation, details, and manual adjustment APIs.")
public class AdminPayrollController {

    private final PayrollService payrollService;

    @PostMapping("/employees/{employeeId}/adjustments/bonus")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Add a positive bonus adjustment for an employee payroll period")
    public ApiResponse<PayrollAdjustmentResponse> addBonus(
            @PathVariable UUID employeeId,
            @Valid @RequestBody PayrollAdjustmentRequest request
    ) {
        return ApiResponse.success("Payroll bonus added", payrollService.addBonus(employeeId, request));
    }

    @PostMapping("/employees/{employeeId}/adjustments/deduction")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Add a positive deduction adjustment for an employee payroll period")
    public ApiResponse<PayrollAdjustmentResponse> addDeduction(
            @PathVariable UUID employeeId,
            @Valid @RequestBody PayrollAdjustmentRequest request
    ) {
        return ApiResponse.success("Payroll deduction added", payrollService.addDeduction(employeeId, request));
    }

    @GetMapping("/employees/{employeeId}/details")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Preview comprehensive payroll details without persisting a calculation")
    public ApiResponse<PayrollCalculationResponse> details(
            @PathVariable UUID employeeId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ApiResponse.success("Payroll details calculated",
                payrollService.previewEmployeePayroll(employeeId, year, month));
    }

    @GetMapping("/employees/{employeeId}/calculate")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Preview payroll calculation for one employee without persisting")
    public ApiResponse<PayrollCalculationResponse> previewCalculate(
            @PathVariable UUID employeeId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ApiResponse.success("Payroll preview calculated",
                payrollService.previewEmployeePayroll(employeeId, year, month));
    }

    @PostMapping("/employees/{employeeId}/calculate")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Calculate and persist payroll result for one employee")
    public ApiResponse<PayrollCalculationResponse> calculateEmployee(
            @PathVariable UUID employeeId,
            @Valid @RequestBody PayrollPeriodRequest request
    ) {
        return ApiResponse.success("Payroll calculated",
                payrollService.calculateEmployeePayroll(employeeId, request.year(), request.month()));
    }

    @PostMapping("/calculate")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Calculate payroll for all employees or a filtered employee set")
    public ApiResponse<BatchPayrollCalculationResponse> calculateBatch(
            @Valid @RequestBody BatchPayrollCalculationRequest request
    ) {
        return ApiResponse.success("Payroll batch calculated", payrollService.calculateBatch(request));
    }
}
