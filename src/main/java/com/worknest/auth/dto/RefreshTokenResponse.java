package com.worknest.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record RefreshTokenResponse(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        UUID activeRoleAssignmentId
) {
}
