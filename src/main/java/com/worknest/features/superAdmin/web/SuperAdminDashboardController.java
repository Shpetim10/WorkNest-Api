package com.worknest.features.superAdmin.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.superAdmin.application.SuperAdminDashboardService;
import com.worknest.features.superAdmin.dto.SuperAdminDashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Year;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/super-admin")
@RequiredArgsConstructor
@Tag(name = "Super Admin", description = "Platform-level super admin API")
public class SuperAdminDashboardController {

    private final SuperAdminDashboardService dashboardService;

    @GetMapping("/dashboard")
    @PreAuthorize("@superAdminSecurity.isSuperAdmin()")
    @Operation(summary = "Get super admin dashboard", description = "Returns KPIs, registrations, activity and stats for the platform dashboard")
    public ApiResponse<SuperAdminDashboardResponse> getDashboard(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String section
    ) {
        int resolvedYear = year != null ? year : Year.now().getValue();
        return ApiResponse.success(
                "Dashboard retrieved successfully",
                dashboardService.getDashboard(resolvedYear, period, startDate, endDate, section)
        );
    }
}
