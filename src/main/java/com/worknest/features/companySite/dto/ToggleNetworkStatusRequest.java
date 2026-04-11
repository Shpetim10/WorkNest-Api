package com.worknest.features.companySite.dto;

import jakarta.validation.constraints.NotNull;

public record ToggleNetworkStatusRequest(
        @NotNull(message = "Active status is required.")
        Boolean isActive,
        
        @NotNull(message = "Version is required for optimistic locking.")
        Long version
) {}
