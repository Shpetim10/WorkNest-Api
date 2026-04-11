package com.worknest.features.companySite.dto;

import com.worknest.domain.enums.NetworkIpVersion;
import com.worknest.domain.enums.NetworkType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Response returned by {@code POST /api/v1/site-network/detect}.
 *
 * <p>This is a suggestion-only payload — nothing is persisted. The frontend
 * shows these values pre-filled in the trusted-network form; the admin can
 * edit any field before submitting the final create-site request.
 *
 * <p>The server derives all values from the request context (resolved client IP)
 * and never trusts any IP or classification sent by the frontend.
 */
@Schema(description = "Server-derived network suggestion for a new trusted-network rule. Read-only; nothing is persisted.")
public record DetectNetworkResponse(

        @Schema(description = "Raw IP resolved from the incoming request using CF-Connecting-IP → X-Real-IP → X-Forwarded-For → RemoteAddr priority chain.",
                example = "203.0.113.42")
        String observedIp,

        @Schema(description = "Normalized IP address (IPv6 addresses are fully expanded; IPv4 addresses are kept as-is).",
                example = "203.0.113.42")
        String normalizedIp,

        @Schema(description = "Suggested CIDR block. /32 for an IPv4 host, /128 for an IPv6 host.",
                example = "203.0.113.42/32")
        String suggestedCidr,

        @Schema(description = "IP version detected from the normalized address.",
                example = "IPV4")
        NetworkIpVersion ipVersion,

        @Schema(description = "Server-classified network type based on the IP characteristics.",
                example = "DEDICATED_HOST")
        NetworkType networkType,

        @Schema(description = "Suggested priority order for this rule (1 = highest). Based on the count of existing rules for the requesting company context.",
                example = "1")
        int suggestedPriorityOrder,

        @Schema(description = "Non-blocking informational warnings the admin should review before saving the rule.")
        List<String> warnings,

        @Schema(description = "UX hints shown alongside the pre-filled form to help the admin understand the detected values.")
        List<String> hints
) {}
