package com.worknest.features.companySite.dto;

import com.worknest.domain.enums.NetworkIpVersion;
import com.worknest.domain.enums.NetworkType;
import com.worknest.domain.entities.SiteTrustedNetwork;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * Summary of a {@link SiteTrustedNetwork} returned inside {@link CreateSiteResponse}.
 */
@Schema(description = "Created trusted-network rule summary.")
public record TrustedNetworkResponse(

        @Schema(description = "Unique identifier of the created rule.")
        UUID id,

        @Schema(description = "Human-friendly label.", example = "Head Office LAN")
        String name,

        @Schema(description = "Network classification.", example = "OFFICE_NETWORK")
        NetworkType networkType,

        @Schema(description = "Stored CIDR block.", example = "203.0.113.0/24")
        String cidrBlock,

        @Schema(description = "IP version.", example = "IPV4")
        NetworkIpVersion ipVersion,

        @Schema(description = "Evaluation priority.", example = "1")
        int priorityOrder,

        @Schema(description = "Whether the rule is currently active.", example = "true")
        boolean isActive,

        @Schema(description = "Expiry timestamp, or null if the rule never expires.")
        Instant expiresAt,

        @Schema(description = "Optimistic-lock version of the rule.")
        long version
) {
    public static TrustedNetworkResponse fromEntity(SiteTrustedNetwork n) {
        return new TrustedNetworkResponse(
                n.getId(),
                n.getName(),
                n.getNetworkType(),
                n.getCidrBlock(),
                n.getIpVersion(),
                n.getPriorityOrder() != null ? n.getPriorityOrder() : 0,
                Boolean.TRUE.equals(n.getIsActive()),
                n.getExpiresAt(),
                n.getVersion() != null ? n.getVersion() : 0L
        );
    }
}
