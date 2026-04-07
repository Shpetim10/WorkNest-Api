package com.worknest.features.company.dto;

import com.worknest.domain.entities.SiteTrustedNetwork;
import com.worknest.domain.enums.NetworkIpVersion;
import com.worknest.domain.enums.NetworkType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * Full network-rule response including the expiry field added in migration 004.
 * Replaces {@link SiteTrustedNetworkResponse} for wizard-context reads.
 *
 * <p>{@code cidrBlock} is returned in full here (no masking) because this endpoint
 * is only accessible to admins within the owning company.
 */
@Schema(description = "Full trusted-network rule as returned in wizard context")
public record TrustedNetworkResponse(

        @Schema(description = "Unique identifier of the rule")
        UUID id,

        @Schema(description = "Site this rule is scoped to")
        UUID siteId,

        @Schema(description = "Friendly name", example = "Office Main WiFi")
        String name,

        @Schema(description = "Classification of this rule")
        NetworkType networkType,

        @Schema(description = "Full CIDR block", example = "192.168.1.0/24")
        String cidrBlock,

        @Schema(description = "IP version derived from the CIDR (read-only)")
        NetworkIpVersion ipVersion,

        @Schema(description = "Whether the rule is currently active")
        Boolean isActive,

        @Schema(description = "Evaluation priority (lower = higher priority)")
        Integer priorityOrder,

        @Schema(
                description = "Expiry timestamp. Null = rule never expires (amber warning). "
                        + "Past = logically expired."
        )
        Instant expiresAt,

        @Schema(description = "Optimistic-lock token — must be echoed in update requests")
        Long version,

        @Schema(description = "Administrator notes")
        String notes,

        @Schema(description = "When the rule was created")
        Instant createdAt,

        @Schema(description = "When the rule was last updated")
        Instant updatedAt
) {

    public static TrustedNetworkResponse fromEntity(SiteTrustedNetwork network) {
        return new TrustedNetworkResponse(
                network.getId(),
                network.getSite().getId(),
                network.getName(),
                network.getNetworkType(),
                network.getCidrBlock(),
                network.getIpVersion(),
                network.getIsActive(),
                network.getPriorityOrder(),
                network.getExpiresAt(),
                network.getVersion(),
                network.getNotes(),
                network.getCreatedAt(),
                network.getUpdatedAt()
        );
    }
}
