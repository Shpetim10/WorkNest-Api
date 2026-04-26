package com.worknest.features.attendance.dto;

import java.time.Instant;
import java.util.UUID;

public record QrValidateResponse(
        boolean valid,
        String code,
        String message,
        UUID terminalId,
        Instant expiresAt
) {
}
