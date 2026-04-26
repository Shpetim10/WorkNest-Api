package com.worknest.features.attendance.dto;

import com.worknest.domain.enums.AttendanceEventType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record ManualAttendanceRequest(
        @NotNull UUID employeeId,
        @NotNull UUID siteId,
        @NotNull AttendanceEventType eventType,
        Instant eventAt,
        @Size(max = 400) String reason,
        @Size(max = 400) String note
) {
}
