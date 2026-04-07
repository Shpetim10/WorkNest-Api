package com.worknest.domain.enums;

/**
 * Classification of how a trusted network rule was defined.
 *
 * <ul>
 *   <li>{@code PUBLIC_IP}  – a single /32 (IPv4) or /128 (IPv6) host address.</li>
 *   <li>{@code CIDR_RANGE} – an explicit CIDR block entered manually by the admin.</li>
 *   <li>{@code VPN}        – a known VPN egress range; amber-warning shown on UI.</li>
 *   <li>{@code AUTO_DETECTED} – rule was produced by server-side IP detection and has
 *       not yet been confirmed by a human. Blocks activation unless confidence is HIGH
 *       or the admin explicitly confirms it.</li>
 * </ul>
 */
public enum NetworkType {
    PUBLIC_IP, CIDR_RANGE, VPN, AUTO_DETECTED;
}