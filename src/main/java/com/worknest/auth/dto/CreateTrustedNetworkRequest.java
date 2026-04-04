package com.worknest.auth.dto;

import com.worknest.auth.domain.NetworkIpVersion;
import com.worknest.auth.domain.NetworkType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to register a trusted network/IP range")
public record CreateTrustedNetworkRequest(
        @NotBlank
        @Size(max = 100)
        @Schema(description = "Friendly name for the network", example = "Office Main WiFi")
        String name,

        @NotNull
        @Schema(description = "The type of network (OFFICE, VPN, etc.)")
        NetworkType networkType,

        @NotBlank
        @Size(max = 100)
        @Schema(description = "CIDR block format for the IP range", example = "192.168.1.0/24")
        String cidrBlock,

        @NotNull
        @Schema(description = "The IP version supported by this block")
        NetworkIpVersion ipVersion,

        @Schema(description = "Whether the network is currently active")
        Boolean isActive,

        @Schema(description = "Priority order for evaluation (lower is evaluated first)")
        Integer priorityOrder,

        @Schema(description = "Internal notes for administrators")
        String notes
) {
}
