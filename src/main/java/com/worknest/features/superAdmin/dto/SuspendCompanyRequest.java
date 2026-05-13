package com.worknest.features.superAdmin.dto;

import jakarta.validation.constraints.NotBlank;

public record SuspendCompanyRequest(
        @NotBlank(message = "Reason is required")
        String reason
) {}