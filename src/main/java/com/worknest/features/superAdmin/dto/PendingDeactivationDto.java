package com.worknest.features.superAdmin.dto;

public record PendingDeactivationDto(
        String id,
        String companyName,
        String email,
        String deactivationRequestedAt,
        String deletionScheduledAt,
        long daysUntilDeletion
) {}
