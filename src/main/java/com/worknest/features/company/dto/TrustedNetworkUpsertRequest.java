package com.worknest.features.company.dto;

import com.worknest.domain.enums.NetworkType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Payload for PUT /api/v1/sites/{siteId}/trusted-networks/{id} (wizard Step 3).
 *
 * <p>{@code ipVersion} is intentionally absent — it is derived server-side from the
 * CIDR notation and is therefore read-only from the client perspective.
 *
 * <p>{@code expiresAt} is nullable. Saving without an expiry is always permitted;
 * the service layer will attach an amber warning to the setup-status response.
 */
@Schema(description = "Payload for upserting a trusted network rule on a site")
public record TrustedNetworkUpsertRequest(

        @NotBlank
        @Size(max = 100)
        @Schema(description = "Friendly name for the network rule", example = "Office Main WiFi")
        String name,

        @NotNull
        @Schema(description = "Classification of this network rule")
        NetworkType networkType,

        @NotBlank
        @Size(max = 100)
        @Schema(
                description = "CIDR notation for the IP range. "
                        + "IPv4 example: 192.168.1.0/24  IPv6 example: 2001:db8::/32",
                example = "192.168.1.0/24"
        )
        String cidrBlock,

        @Schema(description = "Whether this rule is currently active", example = "true")
        Boolean isActive,

        @Schema(description = "Evaluation order — lower value means higher priority", example = "10")
        Integer priorityOrder,

        @Schema(
                description = "Optional expiry timestamp. "
                        + "Null means the rule never expires (an amber warning is shown in the wizard). "
                        + "A past timestamp means the rule has logically expired.",
                example = "2027-01-01T00:00:00Z"
        )
        Instant expiresAt,

        @Schema(description = "Optimistic-lock token from the last read of the network rule", example = "2")
        Long version,

        @Schema(description = "Optional administrator notes")
        String notes
) {}
