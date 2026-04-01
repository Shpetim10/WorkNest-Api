package com.worknest.auth.dto;

import com.worknest.auth.domain.PlatformAccess;
import com.worknest.auth.domain.PlatformRole;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object for login response.
 */
public record LoginResponse(
        boolean authenticated,
        boolean roleSelectionRequired,
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        UUID activeRoleAssignmentId,
        PlatformRole role,
        PlatformAccess platformAccess,
        List<AvailableLoginContextDto> availableContexts,
        String message
) {
}
