package com.worknest.auth.dto;

import com.worknest.auth.domain.PlatformAccess;
import com.worknest.auth.domain.PlatformRole;
import java.time.Instant;
import java.util.UUID;

public record CreateInvitationResponse(
        UUID invitationId,
        String email,
        PlatformRole platformRole,
        PlatformAccess platformAccess,
        Instant expiresAt,
        String message
) {
}
