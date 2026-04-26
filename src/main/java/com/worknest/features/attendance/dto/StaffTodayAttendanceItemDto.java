package com.worknest.features.attendance.dto;

import com.worknest.domain.enums.AttendanceState;
import java.time.Instant;
import java.util.UUID;

public record StaffTodayAttendanceItemDto(
        UUID employeeId,
        UUID userId,
        String employeeName,
        UUID siteId,
        AttendanceState state,
        Instant clockInTime,
        Instant clockOutTime,
        int workedMinutes
) {
}
