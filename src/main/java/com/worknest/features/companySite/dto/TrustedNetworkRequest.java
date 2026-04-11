package com.worknest.features.companySite.dto;

import com.worknest.domain.enums.NetworkIpVersion;
import com.worknest.domain.enums.NetworkType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * A single trusted-network rule to be created together with a new site.
 *
 * <p>The {@code cidrBlock} is validated server-side using proper CIDR parsing.
 * The frontend MUST NOT pre-classify the network type or IP version — those values
 * provided here are the admin's intentional choices; the server cross-validates them
 * against the actual CIDR to catch mismatches.
 */
@Schema(description = "A trusted-network rule to attach to the newly created site.")
public record TrustedNetworkRequest(

        @NotBlank(message = "Network rule name is required")
        @Size(max = 100, message = "Network rule name must not exceed 100 characters")
        @Schema(description = "Human-friendly label for this rule.", example = "Head Office LAN")
        String name,

        @NotNull(message = "Network type is required")
        @Schema(description = "Admin-selected classification for this rule.", example = "OFFICE_NETWORK")
        NetworkType networkType,

        @NotBlank(message = "CIDR block is required")
        @Size(max = 100, message = "CIDR block must not exceed 100 characters")
        @Schema(description = "CIDR block for this rule e.g. 203.0.113.0/24 or 203.0.113.42/32.", example = "203.0.113.0/24")
        String cidrBlock,

        @NotNull(message = "IP version is required")
        @Schema(description = "IP version of the CIDR block. Server will cross-validate against the actual block.", example = "IPV4")
        NetworkIpVersion ipVersion,

        @Min(value = 1, message = "Priority order must be at least 1")
        @Max(value = 999, message = "Priority order must not exceed 999")
        @Schema(description = "Evaluation priority (1 = highest). Lower number wins when multiple rules match.", example = "1")
        int priorityOrder,

        @Schema(description = "Optional notes or description for this rule.", example = "Main office static IP range")
        String notes,

        @Schema(description = "Optional UTC timestamp after which this rule is considered expired. Null means the rule never expires.")
        Instant expiresAt
) {}
