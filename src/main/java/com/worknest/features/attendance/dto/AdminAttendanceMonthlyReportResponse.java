package com.worknest.features.attendance.dto;

import com.worknest.common.api.PaginationMetadata;
import java.util.List;

public record AdminAttendanceMonthlyReportResponse(
        int year,
        int month,
        List<AdminAttendanceReportRowDto> rows,
        PaginationMetadata pagination
) {
}
