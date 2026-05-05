package com.worknest.features.attendance.dto;

import com.worknest.domain.enums.AttendanceState;
import com.worknest.domain.enums.NextAttendanceAction;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TodayAttendanceResponse(
        AttendanceState state,
        NextAttendanceAction nextAllowedAction,
        boolean blocked,
        String blockReasonCode,
        String blockReasonMessage,
        UUID siteId,
        String siteName,
        boolean qrRequired,
        boolean locationRequired,
        Instant serverTime,
        String timezone,
        LocalDate workDate,
        AttendanceRecordDto todayRecord,
        List<AttendanceWarningDto> warnings,
        LocalDateTime clockIn,
        LocalDateTime clockOut
) {
}
