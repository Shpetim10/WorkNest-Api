package com.worknest.features.attendance.dto;

import jakarta.validation.constraints.Size;
import java.time.Instant;

public record ManualCheckInRequest(
        Instant eventAt,
        @Size(max = 400) String reason
) {
}
