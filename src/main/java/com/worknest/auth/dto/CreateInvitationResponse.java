package com.worknest.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record CreateInvitationResponse(
        UUID invitationId,
        String email,
        Instant expiresAt,
        String message
) {
}
