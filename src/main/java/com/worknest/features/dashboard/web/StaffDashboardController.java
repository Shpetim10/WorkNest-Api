package com.worknest.features.dashboard.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.dashboard.application.StaffDashboardService;
import com.worknest.features.dashboard.dto.StaffDashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/staff/dashboard")
@Tag(name = "Staff Dashboard", description = "Self-service dashboard API for staff members")
public class StaffDashboardController {

    private final StaffDashboardService dashboardService;

    @GetMapping
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('STAFF')")
    @Operation(summary = "Get staff dashboard", description = "Returns today's attendance, leave summary, and recent announcements for the authenticated staff member")
    public ApiResponse<StaffDashboardResponse> getDashboard() {
        return ApiResponse.success("Staff dashboard loaded successfully", dashboardService.getDashboard());
    }
}
