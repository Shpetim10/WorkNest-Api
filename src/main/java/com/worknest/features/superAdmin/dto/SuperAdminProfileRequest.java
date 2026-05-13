package com.worknest.features.superAdmin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SuperAdminProfileRequest(
        @NotBlank(message = "Display name is required")
        String displayName
) {}