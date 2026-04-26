package com.worknest.features.attendance.dto;

import com.worknest.domain.enums.AttendanceDayStatus;
import com.worknest.domain.enums.AttendanceReviewStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AttendanceRecordDto(
        UUID dayRecordId,
        Instant clockInTime,
        Instant clockOutTime,
        int workedMinutes,
        AttendanceDayStatus dayStatus,
        AttendanceReviewStatus reviewStatus,
        boolean hasWarnings,
        List<AttendanceWarningDto> warnings
) {
}
