package com.worknest.features.attendance.dto;

import java.time.Instant;
import java.util.UUID;

public record QrCurrentTokenResponse(
        UUID terminalId,
        UUID siteId,
        String token,
        Instant issuedAt,
        Instant expiresAt,
        int rotationSeconds
) {
}
