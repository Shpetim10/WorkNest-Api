package com.worknest.features.attendance.dto;

import java.util.List;

public record MonthlyAttendanceResponse(
        int year,
        int month,
        String timezone,
        List<MonthlyAttendanceDayDto> days
) {
}
