package com.worknest.features.attendance.dto;

import com.worknest.domain.enums.AttendanceDecision;
import com.worknest.domain.enums.AttendanceEventStatus;
import com.worknest.domain.enums.AttendanceEventType;
import com.worknest.domain.enums.AttendanceState;
import com.worknest.domain.enums.NextAttendanceAction;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ClockAttendanceResponse(
        AttendanceState state,
        NextAttendanceAction nextAllowedAction,
        AttendanceEventType eventTypeCreated,
        AttendanceEventStatus eventStatus,
        AttendanceDecision attendanceDecision,
        Instant clockInTime,
        Instant clockOutTime,
        Instant serverTime,
        LocalDate workDate,
        String timezone,
        List<AttendanceWarningDto> warnings,
        String message,
        AttendanceRecordDto todayRecord
) {
}
