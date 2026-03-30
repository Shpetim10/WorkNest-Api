package com.worknest.auth.dto;

import com.worknest.auth.domain.UserStatus;
import java.util.UUID;

public record ActivateInvitationResponse(
        UUID companyId,
        UUID userId,
        String email,
        UserStatus userStatus
) {
}
