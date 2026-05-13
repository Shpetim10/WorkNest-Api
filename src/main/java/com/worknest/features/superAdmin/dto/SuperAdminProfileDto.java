package com.worknest.features.superAdmin.dto;

public record SuperAdminProfileDto(
        String displayName,
        String email,
        String role,
        String accountStatus
) {}