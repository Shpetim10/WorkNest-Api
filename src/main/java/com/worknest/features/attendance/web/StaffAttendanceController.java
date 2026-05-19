package com.worknest.features.attendance.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.common.api.PaginationSupport;
import com.worknest.features.attendance.application.StaffAttendanceService;
import com.worknest.features.attendance.dto.AdjustAttendanceDayRecordRequest;
import com.worknest.features.attendance.dto.AttendanceDashboardResponse;
import com.worknest.features.attendance.dto.DismissWarningsRequest;
import com.worknest.features.attendance.dto.ManualAttendanceRequest;
import com.worknest.features.attendance.dto.ManualCheckInRequest;
import com.worknest.features.attendance.dto.ManualCheckOutRequest;
import com.worknest.features.attendance.dto.ReviewAttendanceEventRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/staff/attendance")
@Tag(name = "Staff Attendance", description = "Team attendance dashboard, manual corrections, and review actions for staff and managers.")
public class StaffAttendanceController {

    private final StaffAttendanceService staffAttendanceService;

    @GetMapping
    @PreAuthorize("@teamSecurity.hasCurrentCompanyPermission('ATTENDANCE_VIEW')")
    @Operation(summary = "Get attendance dashboard — STAFF sees own team, ADMIN may pass siteId/departmentId")
    public ApiResponse<AttendanceDashboardResponse> dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID siteId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success(
                "Attendance dashboard loaded",
                staffAttendanceService.dashboard(date, departmentId, siteId, PaginationSupport.pageable(page, size))
        );
    }

    @PostMapping("/employees/{employeeId}/check-in")
    @PreAuthorize("@teamSecurity.hasCurrentCompanyPermission('ATTENDANCE_CREATE')")
    @Operation(summary = "Manually check in an employee")
    public ApiResponse<Void> checkIn(
            @PathVariable UUID employeeId,
            @Valid @RequestBody ManualCheckInRequest request
    ) {
        staffAttendanceService.manualCheckIn(employeeId, request);
        return ApiResponse.success("Employee manually checked in", null);
    }

    @PostMapping("/employees/{employeeId}/check-out")
    @PreAuthorize("@teamSecurity.hasCurrentCompanyPermission('ATTENDANCE_CREATE')")
    @Operation(summary = "Manually check out an employee")
    public ApiResponse<Void> checkOut(
            @PathVariable UUID employeeId,
            @Valid @RequestBody ManualCheckOutRequest request
    ) {
        staffAttendanceService.manualCheckOut(employeeId, request);
        return ApiResponse.success("Employee manually checked out", null);
    }

    @PostMapping("/day-records/{recordId}/dismiss-warnings")
    @PreAuthorize("@teamSecurity.hasCurrentCompanyPermission('ATTENDANCE_UPDATE')")
    @Operation(summary = "Dismiss attendance warnings for a day record")
    public ApiResponse<Void> dismissWarnings(
            @PathVariable UUID recordId,
            @Valid @RequestBody DismissWarningsRequest request
    ) {
        staffAttendanceService.dismissWarnings(recordId, request);
        return ApiResponse.success("Warnings dismissed", null);
    }

    @PostMapping("/manual-events")
    @PreAuthorize("@teamSecurity.hasCurrentCompanyPermission('ATTENDANCE_CREATE')")
    @Operation(summary = "Create a manual attendance event")
    public ApiResponse<Void> manualEvent(@Valid @RequestBody ManualAttendanceRequest request) {
        staffAttendanceService.createManualEvent(request);
        return ApiResponse.success("Manual attendance event recorded", null);
    }

    @PostMapping("/events/{eventId}/review")
    @PreAuthorize("@teamSecurity.hasCurrentCompanyPermission('ATTENDANCE_REVIEW')")
    @Operation(summary = "Review a flagged attendance event")
    public ApiResponse<Void> review(
            @PathVariable UUID eventId,
            @Valid @RequestBody ReviewAttendanceEventRequest request
    ) {
        staffAttendanceService.reviewEvent(eventId, request);
        return ApiResponse.success("Attendance event reviewed", null);
    }

    @PutMapping("/day-records/{recordId}")
    @PreAuthorize("@teamSecurity.hasCurrentCompanyPermission('ATTENDANCE_UPDATE')")
    @Operation(summary = "Adjust attendance day record")
    public ApiResponse<Void> adjust(
            @PathVariable UUID recordId,
            @Valid @RequestBody AdjustAttendanceDayRecordRequest request
    ) {
        staffAttendanceService.adjustDayRecord(recordId, request);
        return ApiResponse.success("Attendance day record updated", null);
    }
}
