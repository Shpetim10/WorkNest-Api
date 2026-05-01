package com.worknest.features.attendance.dto;

import com.worknest.domain.enums.AttendanceCaptureMethod;
import com.worknest.domain.enums.AttendanceDecision;
import com.worknest.domain.enums.AttendanceEventType;
import com.worknest.domain.enums.AttendanceReviewStatus;
import com.worknest.domain.enums.GeofenceDecision;
import com.worknest.domain.enums.NetworkDecision;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AttendanceEventDetailDto(
        UUID eventId,
        AttendanceEventType eventType,
        AttendanceCaptureMethod captureMethod,
        Instant serverRecordedAt,
        AttendanceDecision attendanceDecision,
        List<String> warningFlags,
        GeofenceDecision geofenceDecision,
        NetworkDecision networkDecision,
        AttendanceReviewStatus reviewStatus,
        String employeeNote,
        String reviewNote,
        String reviewedByName,
        Instant reviewedAt
) {
}
