package com.worknest.features.companySite.application;

import com.worknest.common.web.ClientIpResolver;
import com.worknest.domain.enums.NetworkIpVersion;
import com.worknest.domain.enums.NetworkType;
import com.worknest.features.companySite.dto.DetectNetworkResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link SiteNetworkDetectionService}.
 *
 * <p>Derives all network metadata server-side from the resolved client IP.
 * The frontend MUST NOT provide any IP or classification in the request —
 * this method reads exclusively from the {@link HttpServletRequest} context.
 *
 * <p>Nothing is persisted. The result is advisory only.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiteNetworkDetectionServiceImpl implements SiteNetworkDetectionService {

    @Override
    public DetectNetworkResponse detect(HttpServletRequest request) {
        // 1. Resolve the trusted client IP from server-side headers only.
        String observedIp = ClientIpResolver.resolve(request);
        log.debug("SiteNetworkDetection: resolvedIp='{}' from request", observedIp);

        // 2. Determine IP version and normalize the address.
        InetAddress address = parseAddress(observedIp);
        boolean isIpv6       = address.getAddress().length == 16;
        NetworkIpVersion ipVersion = isIpv6 ? NetworkIpVersion.IPV6 : NetworkIpVersion.IPV4;

        String normalizedIp   = address.getHostAddress();
        String suggestedCidr  = isIpv6
                ? normalizedIp + "/128"
                : normalizedIp + "/32";

        // 3. Classify the network type based on the IP characteristics.
        NetworkType networkType = classifyNetworkType(address, isIpv6);

        // 4. Default suggested priority — caller adjusts once they know which site this applies to.
        int suggestedPriority = 1;

        // 5. Build warnings and hints for admin UX.
        List<String> warnings = buildWarnings(address, networkType, isIpv6);
        List<String> hints    = buildHints(networkType, isIpv6);

        return new DetectNetworkResponse(
                observedIp,
                normalizedIp,
                suggestedCidr,
                ipVersion,
                networkType,
                suggestedPriority,
                warnings,
                hints
        );
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private InetAddress parseAddress(String ip) {
        try {
            // For X-Forwarded-For chains, the first element is already extracted
            // by ClientIpResolver. We parse it directly here.
            return InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            log.warn("SiteNetworkDetection: could not parse IP '{}', falling back to loopback", ip);
            try {
                return InetAddress.getByName("127.0.0.1");
            } catch (UnknownHostException ex) {
                throw new IllegalStateException("Unexpected: cannot parse loopback address", ex);
            }
        }
    }

    /**
     * Classifies the IP into a {@link NetworkType} based on address characteristics.
     * Uses /32 (IPv4) or /128 (IPv6) — always a single-host suggestion from detect.
     */
    private NetworkType classifyNetworkType(InetAddress address, boolean isIpv6) {
        if (address.isLoopbackAddress() || address.isSiteLocalAddress()) {
            // RFC-1918 private ranges or loopback — likely office LAN
            return NetworkType.OFFICE_NETWORK;
        }
        // Public single-host address — most common case for office router / server
        return NetworkType.DEDICATED_HOST;
    }

    private List<String> buildWarnings(InetAddress address, NetworkType type, boolean isIpv6) {
        List<String> warnings = new ArrayList<>();

        if (address.isLoopbackAddress()) {
            warnings.add("The detected IP is a loopback address (127.0.0.1 / ::1). "
                    + "This typically indicates the request arrived via localhost — not suitable for production.");
        }
        if (isIpv6) {
            warnings.add("An IPv6 /128 rule will only match the exact detected address. "
                    + "Consider widening the prefix if multiple IPv6 addresses are used.");
        }
        return warnings;
    }

    private List<String> buildHints(NetworkType type, boolean isIpv6) {
        List<String> hints = new ArrayList<>();

        hints.add("This is a suggestion only. Review and adjust the CIDR before saving.");
        if (type == NetworkType.DEDICATED_HOST) {
            hints.add("A /32 (or /128) rule matches only this exact IP. "
                    + "If your office router has a dynamic IP, consider widening to a /30 or /29.");
        }
        if (type == NetworkType.OFFICE_NETWORK) {
            hints.add("A private-range IP was detected. Verify that this is the external-facing IP "
                    + "your network uses when accessing WorkNest — not an internal LAN address.");
        }
        return hints;
    }
}
