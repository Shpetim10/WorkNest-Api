package com.worknest.features.companySite.validation;

import com.worknest.domain.enums.NetworkIpVersion;
import com.worknest.features.companySite.exception.InvalidCidrException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Stateless CIDR validation utility.
 *
 * <p>Validates that a CIDR string is syntactically correct, that the prefix
 * length is within range for the address family, and that the declared
 * {@link NetworkIpVersion} matches the actual address family of the CIDR.
 *
 * <p>All methods throw {@link InvalidCidrException} on failure. Call sites can
 * catch this exception and surface it to the user without additional wrapping.
 */
public final class CidrValidator {

    private CidrValidator() {
        // utility class
    }

    /**
     * Validates the CIDR string syntax and cross-validates it against the
     * declared {@code ipVersion}.
     *
     * @param cidr      CIDR block to validate, e.g. {@code 203.0.113.0/24}
     * @param declared  IP version declared by the admin in the request
     * @throws InvalidCidrException if the CIDR is invalid or the version mismatches
     */
    public static void validate(String cidr, NetworkIpVersion declared) {
        if (cidr == null || cidr.isBlank()) {
            throw new InvalidCidrException(cidr, "CIDR block must not be blank");
        }

        String[] parts = cidr.split("/", 2);
        if (parts.length != 2) {
            throw new InvalidCidrException(cidr, "must be in CIDR notation (e.g. 203.0.113.0/24)");
        }

        String addressPart = parts[0].trim();
        String prefixPart  = parts[1].trim();

        // -- Prefix length must be a non-negative integer --
        int prefixLen;
        try {
            prefixLen = Integer.parseInt(prefixPart);
        } catch (NumberFormatException e) {
            throw new InvalidCidrException(cidr, "prefix length '" + prefixPart + "' is not a valid integer");
        }

        // -- Parse the address to determine the real address family --
        InetAddress address;
        try {
            address = InetAddress.getByName(addressPart);
        } catch (UnknownHostException e) {
            throw new InvalidCidrException(cidr, "'" + addressPart + "' is not a valid IP address");
        }

        boolean isIpv6 = address.getAddress().length == 16;
        int maxPrefix   = isIpv6 ? 128 : 32;

        if (prefixLen < 0 || prefixLen > maxPrefix) {
            throw new InvalidCidrException(cidr,
                    "prefix length " + prefixLen + " is out of range (0–" + maxPrefix + " for " +
                    (isIpv6 ? "IPv6" : "IPv4") + ")");
        }

        // -- Cross-validate declared IP version --
        NetworkIpVersion actual = isIpv6 ? NetworkIpVersion.IPV6 : NetworkIpVersion.IPV4;
        if (declared != null && declared != actual) {
            throw new InvalidCidrException(cidr,
                    "declared IP version '" + declared + "' does not match the actual address family '" + actual + "'");
        }
    }

    /**
     * Normalizes the CIDR to its canonical form (lower-case, trimmed).
     * Does NOT validate — call {@link #validate} first.
     */
    public static String normalize(String cidr) {
        return cidr.trim().toLowerCase();
    }

    /**
     * Derives the correct {@link NetworkIpVersion} for an already-validated CIDR.
     */
    public static NetworkIpVersion resolveIpVersion(String cidr) {
        try {
            String address = cidr.split("/")[0].trim();
            InetAddress addr = InetAddress.getByName(address);
            return addr.getAddress().length == 16 ? NetworkIpVersion.IPV6 : NetworkIpVersion.IPV4;
        } catch (UnknownHostException e) {
            throw new InvalidCidrException(cidr, "cannot determine address family");
        }
    }

    /**
     * Returns {@code true} if the CIDR represents a single host (/32 for IPv4 or /128 for IPv6).
     * Must be called after {@link #validate}.
     */
    public static boolean isSingleHost(String cidr) {
        String[] parts = cidr.split("/");
        int prefix = Integer.parseInt(parts[1].trim());
        NetworkIpVersion version = resolveIpVersion(cidr);
        return (version == NetworkIpVersion.IPV4 && prefix == 32)
            || (version == NetworkIpVersion.IPV6 && prefix == 128);
    }
}
