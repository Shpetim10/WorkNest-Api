package com.worknest.features.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record ClockAttendanceRequest(
        @NotBlank @Size(max = 100) String clientRequestId,
        String qrToken,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal accuracyMeters,
        Instant clientCapturedAt,
        @Size(max = 120) String devicePublicId,
        @Size(max = 30) String platform,
        @Size(max = 30) String appVersion,
        @Size(max = 400) String employeeNote
) {
}
