package com.worknest.features.attendance.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.attendance.application.MobileAttendanceService;
import com.worknest.features.attendance.dto.ClockAttendanceRequest;
import com.worknest.features.attendance.dto.ClockAttendanceResponse;
import com.worknest.features.attendance.dto.MonthlyAttendanceResponse;
import com.worknest.features.attendance.dto.TodayAttendanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mobile/attendance")
@Tag(name = "Mobile Attendance", description = "Self attendance APIs for employee/staff mobile app.")
public class MobileAttendanceController {

    private final MobileAttendanceService mobileAttendanceService;

    @GetMapping("/today")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('EMPLOYEE', 'STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Get today's attendance state")
    public ApiResponse<TodayAttendanceResponse> today() {
        return ApiResponse.success("Today attendance loaded", mobileAttendanceService.getToday());
    }

    @PostMapping("/clock")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('EMPLOYEE', 'STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Clock-in or clock-out (server decides action)")
    public ApiResponse<ClockAttendanceResponse> clock(
            @Valid @RequestBody ClockAttendanceRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success("Attendance clock action processed", mobileAttendanceService.clock(request, httpRequest));
    }

    @GetMapping("/month")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('EMPLOYEE', 'STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Get monthly attendance calendar for current user")
    public ApiResponse<MonthlyAttendanceResponse> month(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ApiResponse.success("Monthly attendance loaded", mobileAttendanceService.month(year, month));
    }
}
