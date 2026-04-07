package com.worknest.features.company.dto;

import com.worknest.domain.enums.NetworkIpVersion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Server-side assessment result for POST /api/v1/sites/{siteId}/detect-network.
 *
 * <p>Detection captures the IP seen by the server from the authenticated
 * request — client-supplied IPs are never trusted as authoritative.
 *
 * <p>This result is advisory. The admin reviews it and either confirms it
 * via PUT /api/v1/sites/{siteId}/trusted-networks/{id} or enters a
 * different CIDR manually.
 */
@Schema(description = "Server-side network detection result for the admin's current connection")
public record NetworkDetectionResult(

        @Schema(
                description = "Detected IP address as seen by the server. "
                        + "When behind a trusted reverse-proxy, this is taken from X-Forwarded-For[0].",
                example = "203.0.113.42"
        )
        String detectedIp,

        @Schema(
                description = "Suggested CIDR block derived from the detected IP. "
                        + "For IPv4 host addresses this is a /32; for IPv6 a /128 is used unless "
                        + "the prefix is extended to /64 for privacy-address ranges.",
                example = "203.0.113.42/32"
        )
        String suggestedCidr,

        @Schema(description = "IP version derived from the detected address")
        NetworkIpVersion ipVersion,

        @Schema(
                description = "Confidence level of this detection result. "
                        + "AUTO_HIGH = detected from a stable, non-problematic address; "
                        + "AUTO_LOW  = detected but the address has known reliability concerns "
                        + "(CGNAT, VPN, Tor, datacenter ASN); "
                        + "UNRESOLVABLE = server could not determine a usable IP.",
                example = "AUTO_HIGH",
                allowableValues = {"AUTO_HIGH", "AUTO_LOW", "UNRESOLVABLE"}
        )
        String confidence,

        @Schema(description = "Whether the detected IP falls within a CGNAT range (100.64.0.0/10 or equivalent)")
        boolean cgnat,

        @Schema(description = "Whether the address is a known IPv6 privacy / temporary address")
        boolean ipv6PrivacyAddress,

        @Schema(description = "Whether the address is from a VPN or datacenter ASN range (heuristic)")
        boolean vpnOrDatacenter,

        @Schema(
                description = "Whether Tor exit-node characteristics were detected. "
                        + "Tor exit nodes always receive a blocking RED warning and cannot be confirmed.",
                example = "false"
        )
        boolean torExitNode,

        @Schema(
                description = "Whether the suggested CIDR overlaps any CIDR already saved "
                        + "across all trusted-network rules on this site.",
                example = "false"
        )
        boolean overlapDetected,

        @Schema(description = "CIDRs on this site that overlap the detected range")
        List<String> overlappingCidrs,

        @Schema(description = "Non-blocking warnings the wizard must surface (amber). "
                + "Includes no-expiry notices and address-type caveats.")
        List<SiteSetupIssueResponse> warnings,

        @Schema(description = "Blocking issues that prevent the admin from confirming this rule (red). "
                + "Includes Tor exit nodes and unresolvable addresses.")
        List<SiteSetupIssueResponse> blockingIssues
) {}
