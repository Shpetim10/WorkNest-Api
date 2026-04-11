package com.worknest.features.companySite.dto;

import com.worknest.domain.enums.NetworkType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateNetworkRequest(
        @NotBlank(message = "Network name is required.")
        @Size(max = 100, message = "Name must not exceed 100 characters.")
        String name,

        @NotNull(message = "Network type is required.")
        NetworkType networkType,

        @NotBlank(message = "CIDR block is required.")
        @Size(max = 100, message = "CIDR block must not exceed 100 characters.")
        String cidrBlock,

        String notes,

        @Future(message = "Expiration time must be in the future.")
        Instant expiresAt
) {}
