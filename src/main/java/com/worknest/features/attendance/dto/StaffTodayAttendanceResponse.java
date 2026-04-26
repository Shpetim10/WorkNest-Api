package com.worknest.features.attendance.dto;

import java.time.LocalDate;
import java.util.List;

public record StaffTodayAttendanceResponse(
        LocalDate workDate,
        String timezone,
        List<StaffTodayAttendanceItemDto> items
) {
}
