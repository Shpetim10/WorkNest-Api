package com.worknest.features.payroll.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.payroll.application.PayrollService;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollCalculationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mobile/payroll")
@Tag(name = "Mobile Payroll", description = "Self-service payroll details APIs for employees and staff.")
public class MobilePayrollController {

    private final PayrollService payrollService;

    @GetMapping("/details")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('EMPLOYEE', 'STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Preview current employee's payroll details for a month")
    public ApiResponse<PayrollCalculationResponse> details(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ApiResponse.success("Payroll details loaded",
                payrollService.previewCurrentEmployeePayroll(year, month));
    }
}
