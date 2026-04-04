package com.worknest.auth.dto;

import com.worknest.auth.domain.PlatformAccess;
import com.worknest.auth.domain.PlatformRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Response containing details of the created invitation")
public record CreateInvitationResponse(
        @Schema(description = "Unique ID of the created invitation")
        UUID invitationId,

        @Schema(description = "Unique ID of the created/associated user")
        UUID userId,

        @Schema(description = "The role assignment ID linked to this invitation")
        UUID roleAssignmentId,

        @Schema(description = "The e-mail address the invitation was sent to", example = "jane.smith@example.com")
        String email,

        @Schema(description = "The platform role assigned to the user")
        PlatformRole platformRole,

        @Schema(description = "The platform access level authorized")
        PlatformAccess platformAccess,

        @Schema(description = "Expiration timestamp of the invitation token")
        Instant expiresAt,

        @Schema(description = "The raw activation token (only returned in test/dev environments, usually sent via e-mail)")
        String activationToken,

        @Schema(description = "Success message", example = "Invitation created and dispatched successfully")
        String message
) {
}
