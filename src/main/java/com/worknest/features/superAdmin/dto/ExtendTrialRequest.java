package com.worknest.features.superAdmin.dto;

import jakarta.validation.constraints.NotBlank;

public record ExtendTrialRequest(
        @NotBlank(message = "Trial end date is required")
        String trialEndDate
) {}
