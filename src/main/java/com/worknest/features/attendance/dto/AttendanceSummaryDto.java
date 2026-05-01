package com.worknest.features.attendance.dto;

public record AttendanceSummaryDto(
        int total,
        int present,
        int absent,
        int late,
        int onLeave,
        int withWarnings
) {
}
