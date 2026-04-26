package com.worknest.features.attendance.dto;

public record AttendanceWarningDto(
        String code,
        String severity,
        String message
) {
}
