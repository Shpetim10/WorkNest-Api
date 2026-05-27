package com.worknest.features.attendance.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.common.api.PaginationSupport;
import com.worknest.features.attendance.application.AdminAttendanceReportService;
import com.worknest.features.attendance.application.StaffAttendanceService;
import com.worknest.features.attendance.dto.AdminAttendanceMonthlyReportResponse;
import com.worknest.features.attendance.dto.AttendanceDashboardResponse;
import com.worknest.security.AuthSessionPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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
    private final StaffAttendanceService staffAttendanceService;

    @GetMapping("/dashboard")
    @PreAuthorize("@teamSecurity.hasCurrentCompanyPermission('ATTENDANCE_VIEW')")
    @Operation(summary = "Get daily attendance dashboard — all company employees, optional site and department filters")
    public ApiResponse<AttendanceDashboardResponse> dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID siteId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success("Admin attendance dashboard loaded",
                staffAttendanceService.dashboard(date, departmentId, siteId, PaginationSupport.pageable(page, size)));
    }

    @GetMapping("/reports/monthly")
    @PreAuthorize("@teamSecurity.hasCurrentCompanyPermission('ATTENDANCE_VIEW')")
    @Operation(summary = "Get monthly attendance report")
    public ApiResponse<AdminAttendanceMonthlyReportResponse> monthly(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) UUID siteId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        UUID companyId = principal().companyId();
        return ApiResponse.success(
                "Monthly attendance report generated",
                adminAttendanceReportService.monthly(companyId, year, month, siteId, PaginationSupport.pageable(page, size))
        );
    }

    @GetMapping("/export")
    @PreAuthorize("@teamSecurity.hasCurrentCompanyPermission('ATTENDANCE_VIEW')")
    @Operation(summary = "Export attendance dataset (structured payload for future file generators)")
    public ApiResponse<AdminAttendanceMonthlyReportResponse> export(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) UUID siteId
    ) {
        UUID companyId = principal().companyId();
        return ApiResponse.success(
                "Attendance export dataset generated",
                adminAttendanceReportService.monthly(companyId, year, month, siteId, Pageable.unpaged())
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
