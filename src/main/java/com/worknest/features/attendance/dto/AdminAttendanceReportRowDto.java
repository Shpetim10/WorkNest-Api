package com.worknest.features.attendance.dto;

import com.worknest.domain.enums.AttendanceDayStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AdminAttendanceReportRowDto(
        UUID employeeId,
        UUID userId,
        String employeeName,
        UUID siteId,
        LocalDate workDate,
        AttendanceDayStatus dayStatus,
        Instant firstCheckInAt,
        Instant lastCheckOutAt,
        int workedMinutes
) {
}
