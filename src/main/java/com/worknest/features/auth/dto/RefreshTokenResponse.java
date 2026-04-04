package com.worknest.features.auth.dto;

import com.worknest.domain.enums.PlatformAccess;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a successful refresh token operation.
 */
@Schema(description = "Response containing newly rotated authentication tokens")
public record RefreshTokenResponse(
        @Schema(description = "New short-lived JWT access token")
        String accessToken,

        @Schema(description = "New long-lived refresh token")
        String refreshToken,

        @Schema(description = "The session's active role assignment ID")
        UUID activeRoleAssignmentId,

        @Schema(description = "The session's authorized platform access")
        PlatformAccess platformAccess,

        @Schema(description = "Resolved tenant context for the rotated session")
        TenantContextDto tenantContext,

        @Schema(description = "Expiration timestamp of the new access token")
        Instant expiresAt
) {
}
