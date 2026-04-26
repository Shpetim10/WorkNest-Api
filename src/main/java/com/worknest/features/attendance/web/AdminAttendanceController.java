package com.worknest.features.attendance.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.attendance.application.AdminAttendanceReportService;
import com.worknest.features.attendance.dto.AdminAttendanceMonthlyReportResponse;
import com.worknest.security.AuthSessionPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/attendance")
@Tag(name = "Admin Attendance Reports", description = "Company-wide attendance reporting and export endpoints.")
public class AdminAttendanceController {

    private final AdminAttendanceReportService adminAttendanceReportService;

    @GetMapping("/reports/monthly")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Get monthly attendance report")
    public ApiResponse<AdminAttendanceMonthlyReportResponse> monthly(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) UUID siteId
    ) {
        UUID companyId = principal().companyId();
        return ApiResponse.success(
                "Monthly attendance report generated",
                adminAttendanceReportService.monthly(companyId, year, month, siteId)
        );
    }

    @GetMapping("/export")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Export attendance dataset (structured payload for future file generators)")
    public ApiResponse<AdminAttendanceMonthlyReportResponse> export(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) UUID siteId
    ) {
        UUID companyId = principal().companyId();
        return ApiResponse.success(
                "Attendance export dataset generated",
                adminAttendanceReportService.monthly(companyId, year, month, siteId)
        );
    }

    private AuthSessionPrincipal principal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthSessionPrincipal principal)) {
            throw new IllegalStateException("No authenticated principal found");
        }
        return principal;
    }
}
