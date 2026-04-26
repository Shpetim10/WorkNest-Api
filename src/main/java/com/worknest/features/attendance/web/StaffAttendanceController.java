package com.worknest.features.attendance.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.attendance.application.StaffAttendanceService;
import com.worknest.features.attendance.dto.AdjustAttendanceDayRecordRequest;
import com.worknest.features.attendance.dto.ManualAttendanceRequest;
import com.worknest.features.attendance.dto.ReviewAttendanceEventRequest;
import com.worknest.features.attendance.dto.StaffTodayAttendanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
@Tag(name = "Staff Attendance", description = "Team attendance review and manual corrections for staff and managers.")
public class StaffAttendanceController {

    private final StaffAttendanceService staffAttendanceService;

    @GetMapping("/today")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Get today's team attendance")
    public ApiResponse<StaffTodayAttendanceResponse> today(
            @RequestParam(required = false) UUID siteId,
            @RequestParam(required = false) UUID departmentId
    ) {
        return ApiResponse.success("Team attendance loaded", staffAttendanceService.today(siteId, departmentId));
    }

    @PostMapping("/manual-events")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Create a manual attendance event")
    public ApiResponse<Void> manualEvent(@Valid @RequestBody ManualAttendanceRequest request) {
        staffAttendanceService.createManualEvent(request);
        return ApiResponse.success("Manual attendance event recorded", null);
    }

    @PostMapping("/events/{eventId}/review")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Review flagged attendance event")
    public ApiResponse<Void> review(
            @PathVariable UUID eventId,
            @Valid @RequestBody ReviewAttendanceEventRequest request
    ) {
        staffAttendanceService.reviewEvent(eventId, request);
        return ApiResponse.success("Attendance event reviewed", null);
    }

    @PutMapping("/day-records/{recordId}")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Adjust attendance day record")
    public ApiResponse<Void> adjust(
            @PathVariable UUID recordId,
            @Valid @RequestBody AdjustAttendanceDayRecordRequest request
    ) {
        staffAttendanceService.adjustDayRecord(recordId, request);
        return ApiResponse.success("Attendance day record updated", null);
    }
}
