package com.worknest.features.dashboard.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.dashboard.application.MobileDashboardService;
import com.worknest.features.dashboard.dto.MobileDashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mobile/dashboard")
@Tag(name = "Mobile Dashboard", description = "Self-service mobile dashboard API for employees")
public class MobileDashboardController {

    private final MobileDashboardService dashboardService;

    @GetMapping
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('EMPLOYEE', 'STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Get employee mobile dashboard details", description = "Returns today's check-in state, recent leave balances, latest payroll info, and unread announcement data")
    public ApiResponse<MobileDashboardResponse> getDashboard() {
        return ApiResponse.success("Mobile dashboard loaded successfully", dashboardService.getDashboard());
    }
}
