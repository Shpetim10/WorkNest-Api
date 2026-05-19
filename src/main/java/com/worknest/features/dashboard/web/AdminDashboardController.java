package com.worknest.features.dashboard.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.dashboard.application.AdminDashboardService;
import com.worknest.features.dashboard.dto.AdminDashboardResponse;
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
@RequestMapping("/api/v1/admin/dashboard")
@Tag(name = "Admin Dashboard", description = "Company-level admin dashboard API")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping
    @PreAuthorize("@teamSecurity.hasCurrentCompanyPermission('DASHBOARD_VIEW')")
    @Operation(summary = "Get admin dashboard", description = "Returns KPIs, attendance trends, active days, recent activity, and quick stats for the company dashboard")
    public ApiResponse<AdminDashboardResponse> getDashboard(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String trendPeriod,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return ApiResponse.success(
                "Dashboard retrieved successfully",
                dashboardService.getDashboard(period, trendPeriod, startDate, endDate)
        );
    }
}
