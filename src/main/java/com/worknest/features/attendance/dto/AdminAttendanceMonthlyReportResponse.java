package com.worknest.features.attendance.dto;

import java.util.List;

public record AdminAttendanceMonthlyReportResponse(
        int year,
        int month,
        List<AdminAttendanceReportRowDto> rows
) {
}
