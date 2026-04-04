package com.worknest.features.auth.dto;

import com.worknest.domain.enums.PlatformAccess;
import com.worknest.domain.enums.PlatformRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object for login response.
 */
@Schema(description = "Response containing authentication tokens and session state")
public record LoginResponse(
        @Schema(description = "Main authentication flag. True if valid credentials provided.")
        boolean authenticated,

        @Schema(description = "Flag indicating if the user has multiple organizations or roles and must call /select-role to continue.")
        boolean roleSelectionRequired,

        @Schema(description = "Short-lived JWT access token.")
        String accessToken,

        @Schema(description = "Expiration timestamp of the access token.")
        Instant accessTokenExpiresAt,

        @Schema(description = "Long-lived refresh token for token rotation.")
        String refreshToken,

        @Schema(description = "Expiration timestamp of the refresh token.")
        Instant refreshTokenExpiresAt,

        @Schema(description = "The UUID of the currently active role assignment (if selection was automatic or previously made).")
        UUID activeRoleAssignmentId,

        @Schema(description = "The platform role associated with the current session context.")
        PlatformRole role,

        @Schema(description = "The platform access type (WEB_APP, MOBILE_APP, etc.) authorized for this session.")
        PlatformAccess platformAccess,

        @Schema(description = "Resolved tenant context for the authenticated session.")
        TenantContextDto tenantContext,

        @Schema(description = "List of all organizations and roles the user is associated with.")
        List<AvailableLoginContextDto> availableContexts,

        @Schema(description = "Informative status message from the auth server.")
        String message
) {
}
