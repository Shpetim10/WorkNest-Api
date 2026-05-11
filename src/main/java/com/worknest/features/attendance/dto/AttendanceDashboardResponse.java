package com.worknest.features.attendance.dto;

import com.worknest.common.api.PaginationMetadata;
import java.time.LocalDate;
import java.util.List;

public record AttendanceDashboardResponse(
        LocalDate workDate,
        String timezone,
        AttendanceSummaryDto summary,
        List<AttendanceDashboardRowDto> employees,
        PaginationMetadata pagination
) {
}
