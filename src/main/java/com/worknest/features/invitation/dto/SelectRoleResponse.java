package com.worknest.features.invitation.dto;

import com.worknest.domain.enums.PlatformRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Response containing the new session tokens and active context")
public record SelectRoleResponse(
        @Schema(description = "The newly activated role assignment ID")
        UUID activeRoleAssignmentId,

        @Schema(description = "The active platform role for the new session")
        PlatformRole platformRole,

        @Schema(description = "New short-lived JWT access token")
        String accessToken,

        @Schema(description = "Expiration timestamp of the access token")
        Instant accessTokenExpiresAt,

        @Schema(description = "New long-lived refresh token")
        String refreshToken,

        @Schema(description = "Expiration timestamp of the refresh token")
        Instant refreshTokenExpiresAt
) {
}
