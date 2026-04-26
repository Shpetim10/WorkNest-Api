package com.worknest.features.attendance.dto;

import com.worknest.domain.enums.AttendanceDayStatus;
import com.worknest.domain.enums.AttendanceReviewStatus;
import java.time.Instant;
import java.time.LocalDate;

public record MonthlyAttendanceDayDto(
        LocalDate date,
        AttendanceDayStatus dayStatus,
        Instant clockInTime,
        Instant clockOutTime,
        int workedMinutes,
        boolean hasWarnings,
        AttendanceReviewStatus reviewStatus
) {
}
