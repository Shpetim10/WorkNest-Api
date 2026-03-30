package com.worknest.auth.dto;

import com.worknest.auth.domain.PlatformRole;
import java.time.Instant;
import java.util.UUID;

public record CreateInvitationResponse(
        UUID invitationId,
        UUID companyId,
        String email,
        PlatformRole platformRole,
        Instant expiresAt
) {
}
