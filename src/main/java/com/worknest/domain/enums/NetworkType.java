package com.worknest.domain.enums;

/**
 * Production-friendly classification for a trusted network rule stored against a
 * {@link com.worknest.domain.entities.SiteTrustedNetwork}.
 *
 * <ul>
 *   <li>{@code OFFICE_NETWORK} – a known, fixed corporate LAN/WAN range. Requires a CIDR
 *       wider than /32 (IPv4) or /128 (IPv6).</li>
 *   <li>{@code DEDICATED_HOST} – a single, static public IP (exactly /32 or /128).
 *       Typical for a single office router or on-premise server.</li>
 *   <li>{@code VPN_GATEWAY} – a known VPN egress range; an amber warning is shown in the
 *       admin UI because VPN IPs can be shared with users outside the company.</li>
 *   <li>{@code MOBILE_CARRIER} – a carrier-grade NAT range shared by mobile devices
 *       on-site; carries an amber warning because the block may overlap with non-company
 *       devices.</li>
 *   <li>{@code MANUAL_CIDR} – an explicit CIDR block entered manually by the admin with
 *       full awareness; no additional classification is applied.</li>
 * </ul>
 */
public enum NetworkType {

    /**
     * A named corporate LAN or WAN range, wider than a single host.
     */
    OFFICE_NETWORK,

    /**
     * A single static host address (/32 IPv4 or /128 IPv6).
     */
    DEDICATED_HOST,

    /**
     * A VPN egress range. Admin must acknowledge the shared-IP risk.
     */
    VPN_GATEWAY,

    /**
     * A carrier-grade NAT or mobile network range. Admin must acknowledge overlapping risk.
     */
    MOBILE_CARRIER,

    /**
     * Explicit CIDR entered by an admin without further automatic classification.
     */
    MANUAL_CIDR
}