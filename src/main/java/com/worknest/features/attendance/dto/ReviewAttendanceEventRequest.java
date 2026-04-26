package com.worknest.features.attendance.dto;

import com.worknest.domain.enums.AttendanceReviewStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewAttendanceEventRequest(
        @NotNull AttendanceReviewStatus reviewStatus,
        @Size(max = 400) String note
) {
}
