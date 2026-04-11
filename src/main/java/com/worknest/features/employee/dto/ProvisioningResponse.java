package com.worknest.features.employee.dto;

import java.time.Instant;
import java.util.UUID;

public record ProvisioningResponse(
        UUID employeeId,
        UUID userId,
        UUID roleAssignmentId,
        UUID invitationId,
        String email,
        String rawActivationToken,
        Instant invitationExpiresAt,
        String message
) {}
