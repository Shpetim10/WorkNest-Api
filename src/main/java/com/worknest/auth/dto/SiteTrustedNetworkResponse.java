package com.worknest.auth.dto;

import com.worknest.auth.domain.NetworkIpVersion;
import com.worknest.auth.domain.NetworkType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Response containing details of a site-specific trusted network")
public record SiteTrustedNetworkResponse(
        @Schema(description = "Unique identifier of the trusted network record")
        UUID id,

        @Schema(description = "ID of the site this network is associated with")
        UUID siteId,

        @Schema(description = "Friendly name for the network", example = "Office Main WiFi")
        String name,

        @Schema(description = "The type of network (OFFICE, VPN, etc.)")
        NetworkType networkType,

        @Schema(description = "Masked CIDR block for security", example = "192.168.1.xxx/24")
        String cidrBlockMasked,

        @Schema(description = "The IP version (IPv4, IPv6)")
        NetworkIpVersion ipVersion,

        @Schema(description = "Whether this network record is currently active")
        Boolean isActive,

        @Schema(description = "Evaluation priority (lower is higher priority)")
        Integer priorityOrder,

        @Schema(description = "Internal administrator notes")
        String notes
) {
}
