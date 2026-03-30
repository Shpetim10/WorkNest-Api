package com.worknest.auth.dto;

import com.worknest.auth.domain.PlatformRole;
import java.time.Instant;
import java.util.UUID;

public record SelectRoleResponse(
        UUID activeRoleAssignmentId,
        PlatformRole platformRole,
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
}
