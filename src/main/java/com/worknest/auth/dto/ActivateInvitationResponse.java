package com.worknest.auth.dto;

import com.worknest.auth.domain.PlatformAccess;
import com.worknest.auth.domain.PlatformRole;
import com.worknest.auth.domain.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Response containing user and session details after activation")
public record ActivateInvitationResponse(
        @Schema(description = "Unique identifier of the activated user")
        UUID userId,

        @Schema(description = "The role assignment ID activated for this session")
        UUID roleAssignmentId,

        @Schema(description = "The platform role the user now holds")
        PlatformRole role,

        @Schema(description = "The primary platform access authorized for this user")
        PlatformAccess platformAccess,

        @Schema(description = "The current status of the user account")
        UserStatus status,

        @Schema(description = "Success message", example = "Invitation activated successfully. Your account is now ACTIVE.")
        String message
) {
}
