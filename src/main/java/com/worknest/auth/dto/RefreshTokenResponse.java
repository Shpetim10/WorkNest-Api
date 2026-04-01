package com.worknest.auth.dto;

import com.worknest.auth.domain.PlatformAccess;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a successful refresh token operation.
 */
public record RefreshTokenResponse(
        String accessToken,
        String refreshToken,
        UUID activeRoleAssignmentId,
        PlatformAccess platformAccess,
        Instant expiresAt
) {
}
