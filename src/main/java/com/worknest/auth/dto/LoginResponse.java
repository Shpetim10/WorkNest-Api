package com.worknest.auth.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LoginResponse(
        UUID userId,
        UUID companyId,
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        UUID activeRoleAssignmentId,
        List<UUID> availableRoleAssignmentIds,
        boolean roleSelectionRequired
) {
}
