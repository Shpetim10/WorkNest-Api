package com.worknest.auth.dto;

import com.worknest.auth.domain.UserStatus;
import java.util.UUID;

public record ActivateInvitationResponse(
        UUID userId,
        UUID roleAssignmentId,
        UserStatus status,
        String message
) {
}
