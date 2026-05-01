package com.worknest.features.attendance.dto;

import java.time.LocalDate;
import java.util.List;

public record AttendanceDashboardResponse(
        LocalDate workDate,
        String timezone,
        AttendanceSummaryDto summary,
        List<AttendanceDashboardRowDto> employees
) {
}
