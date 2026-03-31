package com.worknest.auth.dto;

import com.worknest.auth.domain.PlatformAccess;
import com.worknest.auth.domain.PlatformRole;
import com.worknest.auth.domain.UserStatus;
import java.util.UUID;

public record ActivateInvitationResponse(
        UUID userId,
        UUID roleAssignmentId,
        PlatformRole role,
        PlatformAccess platformAccess,
        UserStatus status,
        String message
) {
}
