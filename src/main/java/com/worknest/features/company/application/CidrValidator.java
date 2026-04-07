package com.worknest.features.company.application;

import com.worknest.domain.enums.NetworkIpVersion;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Pure-static utility for CIDR normalization, validation, IP-version derivation,
 * and overlap detection.
 *
 * <p>Design constraints (per the site-setup plan):
 * <ul>
 *   <li>IP version is ALWAYS derived from the CIDR string — never trusted from the client.</li>
 *   <li>CIDR validation is defensive: malformed notation must produce a clear error, not a crash.</li>
 *   <li>Overlap detection uses canonical network address comparison — host bits beyond the prefix
 *       are masked before comparing, so "192.168.1.5/24" and "192.168.1.0/24" are the same network.</li>
 * </ul>
 *
 * <p>No Spring beans — this class is intentionally stateless so it can be used
 * from both the detection service and the upsert validation path.
 */
public final class CidrValidator {

    // RFC 5737 documentation range / CGNAT (RFC 6598) / loopback / link-local / private ranges
    private static final List<CidrRange> IPV4_CGNAT_RANGES = List.of(
            new CidrRange("100.64.0.0", 10),   // CGNAT (RFC 6598)
            new CidrRange("10.0.0.0", 8),       // Private Class A (also used by some VPNs)
            new CidrRange("172.16.0.0", 12),    // Private Class B
            new CidrRange("192.168.0.0", 16),   // Private Class C
            new CidrRange("127.0.0.0", 8),      // Loopback
            new CidrRange("169.254.0.0", 16)    // Link-local
    );

    // IPv6 ranges of concern
    private static final List<CidrRange> IPV6_CONCERN_RANGES = List.of(
            new CidrRange("fc00::", 7),          // Unique Local (fc00::/7)
            new CidrRange("fe80::", 10),          // Link-local (fe80::/10)
            new CidrRange("::1", 128)             // Loopback
    );

    // Regex for basic syntactic validation before attempting InetAddress parsing
    private static final Pattern IPV4_CIDR_PATTERN =
            Pattern.compile("^(\\d{1,3}\\.){3}\\d{1,3}/(\\d|[12]\\d|3[02])$");
    private static final Pattern IPV6_CIDR_PATTERN =
            Pattern.compile("^[0-9a-fA-F:]+/(\\d{1,3})$");

    private CidrValidator() {}

    // ─────────────────────────────────────────────────────────
    // IP Version Derivation
    // ─────────────────────────────────────────────────────────

    /**
     * Derives {@link NetworkIpVersion} from a CIDR string.
     * IPv6 is detected by the presence of a colon in the address part.
     * This is the canonical method — never use a client-supplied IP version.
     */
    public static NetworkIpVersion deriveIpVersion(String cidrBlock) {
        String normalized = cidrBlock.trim();
        String addressPart = normalized.contains("/") ? normalized.substring(0, normalized.indexOf('/')) : normalized;
        return addressPart.contains(":") ? NetworkIpVersion.IPV6 : NetworkIpVersion.IPV4;
    }

    // ─────────────────────────────────────────────────────────
    // CIDR Normalization
    // ─────────────────────────────────────────────────────────

    /**
     * Normalizes a CIDR string: trims whitespace and converts to lowercase
     * (important for IPv6 canonical form).
     */
    public static String normalize(String cidrBlock) {
        return cidrBlock.trim().toLowerCase();
    }

    /**
     * Converts a bare IP address (no prefix) to a host CIDR:
     * /32 for IPv4, /128 for IPv6.
     */
    public static String toHostCidr(String ipAddress) {
        String trimmed = ipAddress.trim();
        boolean isIpv6 = trimmed.contains(":");
        return trimmed + (isIpv6 ? "/128" : "/32");
    }

    // ─────────────────────────────────────────────────────────
    // Validation
    // ─────────────────────────────────────────────────────────

    /**
     * Returns a non-empty error message if the CIDR string is invalid,
     * or {@code null} if it is well-formed.
     *
     * <p>Checks: non-blank, syntactic regex, parseable address part, prefix length in range.
     */
    public static String validate(String cidrBlock) {
        if (cidrBlock == null || cidrBlock.isBlank()) {
            return "CIDR block must not be blank";
        }
        String normalized = cidrBlock.trim();
        if (!normalized.contains("/")) {
            return "CIDR block must include a prefix length (e.g. 192.168.1.0/24)";
        }

        String[] parts = normalized.split("/", 2);
        String addressPart = parts[0];
        String prefixPart = parts[1];

        int prefix;
        try {
            prefix = Integer.parseInt(prefixPart);
        } catch (NumberFormatException e) {
            return "CIDR prefix length is not a valid integer: " + prefixPart;
        }

        boolean isIpv6 = addressPart.contains(":");
        if (isIpv6) {
            if (prefix < 0 || prefix > 128) {
                return "IPv6 prefix length must be between 0 and 128, got: " + prefix;
            }
        } else {
            if (prefix < 0 || prefix > 32) {
                return "IPv4 prefix length must be between 0 and 32, got: " + prefix;
            }
            if (!IPV4_CIDR_PATTERN.matcher(normalized).matches()) {
                return "CIDR block does not match expected IPv4 format (e.g. 192.168.1.0/24)";
            }
            // Validate that each octet is 0–255
            String[] octets = addressPart.split("\\.");
            for (String octet : octets) {
                int val;
                try {
                    val = Integer.parseInt(octet);
                } catch (NumberFormatException e) {
                    return "CIDR address contains non-numeric octet: " + octet;
                }
                if (val < 0 || val > 255) {
                    return "CIDR address octet out of range [0-255]: " + octet;
                }
            }
        }

        // Attempt actual parsing to catch any remaining malformed addresses
        try {
            InetAddress.getByName(addressPart);
        } catch (UnknownHostException e) {
            return "CIDR address part could not be parsed: " + addressPart;
        }

        return null; // valid
    }

    // ─────────────────────────────────────────────────────────
    // Special-Address Classification
    // ─────────────────────────────────────────────────────────

    /**
     * Returns true if the IP address falls within any CGNAT or private range
     * (RFC 1918, RFC 6598, loopback, link-local).
     *
     * <p>Used to surface an amber CGNAT warning when a detected office IP
     * is shared NAT — enforcement will be unreliable.
     */
    public static boolean isCgnatOrPrivate(String ipAddress) {
        return isInAnyRange(ipAddress.trim(), IPV4_CGNAT_RANGES);
    }

    /**
     * Returns true if this is an IPv6 privacy / temporary address.
     * Privacy addresses rotate, making them unsuitable for stable enforcement.
     *
     * <p>Heuristic: checks for fc00::/7 (ULA), fe80::/10 (link-local), or ::1
     * (loopback). Full RFC 4941 temporary address detection would require
     * kernel-level inspection beyond what is available here.
     */
    public static boolean isIpv6PrivacyAddress(String ipAddress) {
        return isInAnyRange(ipAddress.trim(), IPV6_CONCERN_RANGES);
    }

    /**
     * Heuristic VPN / datacenter detection based on well-known CIDR prefixes.
     *
     * <p>This is intentionally conservative — it only flags the most obvious
     * ranges (Cloudflare, common VPN providers). A production-grade implementation
     * would query an ASN API.
     *
     * <p><strong>Assumption:</strong> Full ASN lookup is out of scope for Phase 1.
     * This method returns false for now and serves as the extension point.
     */
    public static boolean isVpnOrDatacenter(String ipAddress) {
        // Reserved for Phase 2: ASN lookup integration
        // Example: query ip-api.com or ip2location for org/ASN fields
        return false;
    }

    /**
     * Returns true if the IP is a known Tor exit node.
     *
     * <p><strong>Assumption:</strong> Real-time Tor exit-node list is out of scope
     * for Phase 1. The Tor Project publishes a bulk exit-node list at
     * https://check.torproject.org/torbulkexitlist which should be cached and
     * queried in a Phase 2 scheduled job. This method always returns false until
     * that integration is wired up.
     */
    public static boolean isTorExitNode(String ipAddress) {
        // Reserved for Phase 2: Tor exit-node list lookup
        return false;
    }

    // ─────────────────────────────────────────────────────────
    // Overlap Detection
    // ─────────────────────────────────────────────────────────

    /**
     * Checks whether {@code candidateCidr} overlaps any CIDR in {@code existingCidrs}.
     *
     * <p>Two CIDRs overlap when one contains an IP that is also contained in the other.
     * The canonical formula: A overlaps B iff networkAddress(A) is in B OR networkAddress(B) is in A.
     *
     * @param candidateCidr the CIDR being saved
     * @param existingCidrs all other CIDRs already saved on this site
     * @return list of CIDRs from {@code existingCidrs} that overlap {@code candidateCidr}
     */
    public static List<String> findOverlaps(String candidateCidr, List<String> existingCidrs) {
        List<String> overlaps = new ArrayList<>();
        ParsedCidr candidate = parseCidr(candidateCidr);
        if (candidate == null) {
            return overlaps; // candidate itself is invalid — caller should have validated first
        }

        for (String existing : existingCidrs) {
            if (existing.equalsIgnoreCase(candidateCidr.trim())) {
                continue; // exact-same entry (upsert of existing rule) — not an overlap with itself
            }
            ParsedCidr other = parseCidr(existing);
            if (other == null) {
                continue; // skip malformed existing entries
            }
            if (cidrsOverlap(candidate, other)) {
                overlaps.add(existing);
            }
        }
        return overlaps;
    }

    /**
     * Convenience: returns true if there is any overlap.
     */
    public static boolean hasOverlap(String candidateCidr, List<String> existingCidrs) {
        return !findOverlaps(candidateCidr, existingCidrs).isEmpty();
    }

    // ─────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────

    private static boolean isInAnyRange(String ipAddress, List<CidrRange> ranges) {
        InetAddress addr;
        try {
            addr = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException e) {
            return false;
        }
        for (CidrRange range : ranges) {
            try {
                InetAddress rangeAddr = InetAddress.getByName(range.address());
                if (addr.getClass() != rangeAddr.getClass()) {
                    continue; // IPv4 vs IPv6 mismatch
                }
                if (isInRange(addr, rangeAddr, range.prefix())) {
                    return true;
                }
            } catch (UnknownHostException e) {
                // skip malformed range definition
            }
        }
        return false;
    }

    private static boolean isInRange(InetAddress addr, InetAddress networkAddr, int prefix) {
        byte[] addrBytes = addr.getAddress();
        byte[] netBytes = networkAddr.getAddress();
        if (addrBytes.length != netBytes.length) {
            return false;
        }
        // Compare the first `prefix` bits
        int fullBytes = prefix / 8;
        int remainingBits = prefix % 8;

        for (int i = 0; i < fullBytes; i++) {
            if (addrBytes[i] != netBytes[i]) {
                return false;
            }
        }
        if (remainingBits > 0 && fullBytes < addrBytes.length) {
            int mask = (0xFF << (8 - remainingBits)) & 0xFF;
            if ((addrBytes[fullBytes] & mask) != (netBytes[fullBytes] & mask)) {
                return false;
            }
        }
        return true;
    }

    private static ParsedCidr parseCidr(String cidr) {
        String normalized = cidr.trim();
        if (!normalized.contains("/")) {
            return null;
        }
        String[] parts = normalized.split("/", 2);
        int prefix;
        try {
            prefix = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
        InetAddress addr;
        try {
            addr = InetAddress.getByName(parts[0]);
        } catch (UnknownHostException e) {
            return null;
        }
        // Mask the address to network address (clear host bits)
        byte[] bytes = addr.getAddress();
        maskHostBits(bytes, prefix);
        try {
            InetAddress networkAddr = InetAddress.getByAddress(bytes);
            return new ParsedCidr(networkAddr, prefix);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static void maskHostBits(byte[] bytes, int prefix) {
        int totalBits = bytes.length * 8;
        for (int bit = prefix; bit < totalBits; bit++) {
            int byteIdx = bit / 8;
            int bitIdx = 7 - (bit % 8);
            bytes[byteIdx] &= ~(1 << bitIdx);
        }
    }

    private static boolean cidrsOverlap(ParsedCidr a, ParsedCidr b) {
        // Skip comparison between IPv4 and IPv6
        if (a.address().getClass() != b.address().getClass()) {
            return false;
        }
        // A overlaps B if A's network address is contained in B, OR B's network address is in A
        return isInRange(a.address(), b.address(), b.prefix())
                || isInRange(b.address(), a.address(), a.prefix());
    }

    // ─────────────────────────────────────────────────────────
    // Internal value types
    // ─────────────────────────────────────────────────────────

    private record CidrRange(String address, int prefix) {}

    private record ParsedCidr(InetAddress address, int prefix) {}
}
