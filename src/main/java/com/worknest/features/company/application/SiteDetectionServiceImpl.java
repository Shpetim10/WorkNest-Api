package com.worknest.features.company.application;

import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.entities.SiteTrustedNetwork;
import com.worknest.domain.enums.NetworkIpVersion;
import com.worknest.features.company.dto.LocationDetectionRequest;
import com.worknest.features.company.dto.LocationDetectionResult;
import com.worknest.features.company.dto.NetworkDetectionResult;
import com.worknest.features.company.dto.SiteSetupIssueResponse;
import com.worknest.features.company.exception.CompanySiteNotFoundException;
import com.worknest.features.company.repository.CompanySiteRepository;
import com.worknest.features.company.repository.SiteTrustedNetworkRepository;
import com.worknest.tenant.TenantContextHolder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of server-side detection flows for the site setup wizard.
 *
 * <h2>Location detection</h2>
 * <p>Validates browser-supplied Geolocation API coordinates for:
 * <ul>
 *   <li>Staleness: rejects / warns when {@code |now - browserTimestamp| > 30 s}</li>
 *   <li>Accuracy: warns when reported accuracy exceeds the site's
 *       {@code maxLocationAccuracyMeters} threshold (defaulting to 100 m)</li>
 *   <li>Suggests a geofence radius using the formula:
 *       {@code (effectiveMaxAccuracy × 2) + 30}</li>
 * </ul>
 * Coordinates are NOT persisted by this call — the result is advisory only.
 *
 * <h2>Network detection</h2>
 * <p>Captures the IP seen by the server from the authenticated request
 * (client-supplied IP claims are never trusted). Assesses:
 * <ul>
 *   <li>CGNAT / private-range detection (RFC 6598, RFC 1918)</li>
 *   <li>IPv6 privacy / temporary-address detection</li>
 *   <li>VPN / datacenter heuristic (Phase 2 ASN integration stub)</li>
 *   <li>Tor exit-node detection (Phase 2 bulk-list stub)</li>
 *   <li>CIDR overlap against all active rules on the site</li>
 * </ul>
 *
 * <h2>Assumptions / open decisions</h2>
 * <ol>
 *   <li>Staleness threshold is hardcoded to 30 000 ms per the plan.
 *       Configuring it per-site is a Phase 2 concern.</li>
 *   <li>Default accuracy threshold is 100 m when the site has no override.
 *       The plan mentions "50 m" in one section and "per-site configurable"
 *       in another — we use the value stored on the entity and fall back to 100 m.</li>
 *   <li>The suggested radius formula is {@code (maxAccuracy × 2) + 30} as stated in the plan.</li>
 *   <li>VPN/datacenter and Tor detection return {@code false} in Phase 1 —
 *       stubs are in {@link CidrValidator} and must be wired to ASN/Tor APIs in Phase 2.</li>
 *   <li>CGNAT detection covers RFC 1918 private ranges as well, since both
 *       produce enforcement-unreliability issues for the same reason.</li>
 *   <li>Confidence is AUTO_HIGH unless any concern flag is true, in which case
 *       it degrades to AUTO_LOW. UNRESOLVABLE is used for blank/null IPs only.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiteDetectionServiceImpl implements SiteDetectionService {

    /** Staleness threshold from the plan: 30 seconds. */
    private static final long STALENESS_THRESHOLD_MS = 30_000L;

    /** Default accuracy threshold when the site provides no override (100 m). */
    private static final int DEFAULT_MAX_ACCURACY_METERS = 100;

    private final CompanySiteRepository companySiteRepository;
    private final SiteTrustedNetworkRepository siteTrustedNetworkRepository;

    // ─────────────────────────────────────────────────────────────────────
    // Location Detection
    // ─────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public LocationDetectionResult detectLocation(UUID siteId, LocationDetectionRequest request) {
        CompanySite site = getOwnedSite(siteId);

        List<SiteSetupIssueResponse> warnings = new ArrayList<>();
        boolean stale = false;
        boolean lowAccuracy = false;
        Long coordinateAgeMs = null;

        // ── 1. Null-safety guards ──────────────────────────────────────────
        if (request.latitude() == null || request.longitude() == null) {
            warnings.add(issue(
                    "LOCATION_MISSING_COORDINATES",
                    "Latitude and longitude are required for location detection.",
                    "location"
            ));
            return new LocationDetectionResult(
                    false, request.latitude(), request.longitude(),
                    request.accuracyMeters(), null, false, false, null, warnings
            );
        }

        // ── 2. Staleness check ────────────────────────────────────────────
        if (request.browserTimestampMs() != null) {
            long nowMs = Instant.now().toEpochMilli();
            coordinateAgeMs = nowMs - request.browserTimestampMs();
            if (coordinateAgeMs > STALENESS_THRESHOLD_MS) {
                stale = true;
                warnings.add(issue(
                        "STALE_COORDINATES",
                        "Browser coordinates are " + (coordinateAgeMs / 1000) + " seconds old. "
                                + "Please re-prompt geolocation for a fresh fix.",
                        "location"
                ));
            }
        } else {
            // No timestamp supplied — we cannot verify freshness; warn
            warnings.add(issue(
                    "LOCATION_TIMESTAMP_MISSING",
                    "No browser timestamp was supplied. Coordinate freshness cannot be verified.",
                    "location"
            ));
        }

        // ── 3. Accuracy check ─────────────────────────────────────────────
        int effectiveMaxAccuracy = (site.getMaxLocationAccuracyMeters() != null)
                ? site.getMaxLocationAccuracyMeters()
                : DEFAULT_MAX_ACCURACY_METERS;

        if (request.accuracyMeters() != null) {
            if (request.accuracyMeters() > effectiveMaxAccuracy) {
                lowAccuracy = true;
                warnings.add(issue(
                        "LOW_LOCATION_ACCURACY",
                        String.format(
                                "Reported GPS accuracy (%.0f m) exceeds the configured threshold (%d m). "
                                        + "Move to an open area or wait for a better fix.",
                                request.accuracyMeters(), effectiveMaxAccuracy
                        ),
                        "location"
                ));
            }
        } else {
            warnings.add(issue(
                    "LOCATION_ACCURACY_UNKNOWN",
                    "Browser did not report an accuracy value. Proceed with caution.",
                    "location"
            ));
        }

        // ── 4. Suggested radius ───────────────────────────────────────────
        Integer suggestedRadius = null;
        if (request.accuracyMeters() != null) {
            // Formula: (effectiveMaxAccuracy × 2) + 30  (per plan section 15 / table)
            suggestedRadius = (effectiveMaxAccuracy * 2) + 30;
        }

        // Coordinates are usable if not stale and not egregiously inaccurate
        boolean usable = !stale && !lowAccuracy;

        log.debug(
                "Location detection for site {}: usable={}, stale={}, lowAccuracy={}, ageMs={}",
                siteId, usable, stale, lowAccuracy, coordinateAgeMs
        );

        return new LocationDetectionResult(
                usable,
                request.latitude(),
                request.longitude(),
                request.accuracyMeters(),
                coordinateAgeMs,
                stale,
                lowAccuracy,
                suggestedRadius,
                warnings
        );
    }

    // ─────────────────────────────────────────────────────────────────────
    // Network Detection
    // ─────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public NetworkDetectionResult detectNetwork(UUID siteId, String clientIp, UUID excludeNetworkId) {
        // Ownership check — ensures cross-company access is blocked
        getOwnedSite(siteId);

        List<SiteSetupIssueResponse> warnings = new ArrayList<>();
        List<SiteSetupIssueResponse> blockingIssues = new ArrayList<>();

        // ── 1. Validate we have an IP at all ──────────────────────────────
        if (clientIp == null || clientIp.isBlank()) {
            blockingIssues.add(issue(
                    "NETWORK_IP_UNRESOLVABLE",
                    "Server could not determine your public IP address. "
                            + "Try entering the CIDR block manually.",
                    "trustedNetworks"
            ));
            return unresolvableResult(warnings, blockingIssues);
        }

        String ip = clientIp.trim();

        // ── 2. Derive IP version from the address ─────────────────────────
        NetworkIpVersion ipVersion = ip.contains(":") ? NetworkIpVersion.IPV6 : NetworkIpVersion.IPV4;

        // Build the suggested host CIDR (/32 or /128)
        String suggestedCidr = CidrValidator.toHostCidr(ip);

        // ── 3. Special-address classification ──────────────────────────────
        boolean cgnat = CidrValidator.isCgnatOrPrivate(ip);
        boolean ipv6Privacy = ipVersion == NetworkIpVersion.IPV6 && CidrValidator.isIpv6PrivacyAddress(ip);
        boolean vpnOrDc = CidrValidator.isVpnOrDatacenter(ip);     // Phase 2 stub → false
        boolean tor = CidrValidator.isTorExitNode(ip);              // Phase 2 stub → false

        if (cgnat) {
            warnings.add(issue(
                    "CGNAT_DETECTED",
                    "Your public IP (" + ip + ") is in a CGNAT or private range. "
                            + "Attendance enforcement using this IP will be unreliable if "
                            + "multiple locations share the same NAT address. "
                            + "Consider using a manually entered CIDR block instead.",
                    "trustedNetworks"
            ));
        }

        if (ipv6Privacy) {
            warnings.add(issue(
                    "IPV6_PRIVACY_ADDRESS",
                    "The detected IPv6 address (" + ip + ") appears to be a privacy / temporary address. "
                            + "These addresses rotate and are unsuitable for stable network enforcement. "
                            + "Consider using the stable /64 prefix instead.",
                    "trustedNetworks"
            ));
        }

        if (vpnOrDc) {
            warnings.add(issue(
                    "VPN_OR_DATACENTER_IP",
                    "The detected IP (" + ip + ") appears to be associated with a VPN or datacenter. "
                            + "Enforcement accuracy may be reduced. Confirm only if this is intentional.",
                    "trustedNetworks"
            ));
        }

        if (tor) {
            // Tor exit nodes are a hard block — cannot be confirmed per the plan
            blockingIssues.add(issue(
                    "TOR_EXIT_NODE_DETECTED",
                    "The detected IP (" + ip + ") is a known Tor exit node. "
                            + "Tor exit nodes cannot be used as trusted networks.",
                    "trustedNetworks"
            ));
        }

        // ── 4. CIDR overlap against existing active rules on this site ─────
        List<SiteTrustedNetwork> activeRules =
                siteTrustedNetworkRepository.findAllBySiteIdAndIsActiveTrue(siteId);

        List<String> existingCidrs = activeRules.stream()
                .filter(r -> excludeNetworkId == null || !excludeNetworkId.equals(r.getId()))
                .map(SiteTrustedNetwork::getCidrBlock)
                .toList();

        List<String> overlaps = CidrValidator.findOverlaps(suggestedCidr, existingCidrs);
        boolean overlapDetected = !overlaps.isEmpty();

        if (overlapDetected) {
            warnings.add(issue(
                    "CIDR_OVERLAP_DETECTED",
                    "The detected CIDR (" + suggestedCidr + ") overlaps existing trusted-network rules: "
                            + String.join(", ", overlaps) + ". "
                            + "Resolve the conflict before saving to avoid duplicate enforcement.",
                    "trustedNetworks"
            ));
        }

        // ── 5. Confidence scoring ──────────────────────────────────────────
        String confidence;
        if (!blockingIssues.isEmpty()) {
            confidence = "UNRESOLVABLE";
        } else if (cgnat || ipv6Privacy || vpnOrDc || overlapDetected) {
            confidence = "AUTO_LOW";
        } else {
            confidence = "AUTO_HIGH";
        }

        log.debug(
                "Network detection for site {}: ip={}, confidence={}, cgnat={}, overlap={}",
                siteId, ip, confidence, cgnat, overlapDetected
        );

        return new NetworkDetectionResult(
                ip,
                suggestedCidr,
                ipVersion,
                confidence,
                cgnat,
                ipv6Privacy,
                vpnOrDc,
                tor,
                overlapDetected,
                overlaps,
                warnings,
                blockingIssues
        );
    }

    // ─────────────────────────────────────────────────────────────────────
    // Upsert-time CIDR validation (shared with CompanySiteSetupServiceImpl)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Validates a CIDR block being saved on a trusted-network rule.
     *
     * <p>This method is intentionally package-accessible so
     * {@link CompanySiteSetupServiceImpl} can call it during upsert without
     * duplicating the logic.
     *
     * @param siteId            site being configured
     * @param cidrBlock         CIDR from the upsert request
     * @param excludeNetworkId  UUID of the rule being updated (excluded from overlap check)
     * @return list of blocking issues; empty means the CIDR is safe to save
     */
    List<SiteSetupIssueResponse> validateCidrForUpsert(UUID siteId, String cidrBlock, UUID excludeNetworkId) {
        List<SiteSetupIssueResponse> issues = new ArrayList<>();

        String validationError = CidrValidator.validate(cidrBlock);
        if (validationError != null) {
            issues.add(issue("INVALID_CIDR", validationError, "cidrBlock"));
            return issues; // no point checking overlap on an invalid CIDR
        }

        List<SiteTrustedNetwork> activeRules =
                siteTrustedNetworkRepository.findAllBySiteIdAndIsActiveTrue(siteId);

        List<String> existingCidrs = activeRules.stream()
                .filter(r -> excludeNetworkId == null || !excludeNetworkId.equals(r.getId()))
                .map(SiteTrustedNetwork::getCidrBlock)
                .toList();

        List<String> overlaps = CidrValidator.findOverlaps(cidrBlock, existingCidrs);
        if (!overlaps.isEmpty()) {
            issues.add(issue(
                    "CIDR_OVERLAP",
                    "CIDR " + cidrBlock + " overlaps existing rule(s): " + String.join(", ", overlaps)
                            + ". Resolve the conflict before saving.",
                    "cidrBlock"
            ));
        }

        return issues;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    private CompanySite getOwnedSite(UUID siteId) {
        UUID companyId = TenantContextHolder.get()
                .orElseThrow(() -> new IllegalStateException("No tenant context found"))
                .companyId();
        return companySiteRepository.findByIdAndCompanyId(siteId, companyId)
                .orElseThrow(() -> new CompanySiteNotFoundException(siteId));
    }

    private SiteSetupIssueResponse issue(String code, String message, String field) {
        return new SiteSetupIssueResponse(code, message, field);
    }

    private NetworkDetectionResult unresolvableResult(
            List<SiteSetupIssueResponse> warnings,
            List<SiteSetupIssueResponse> blockingIssues
    ) {
        return new NetworkDetectionResult(
                null, null, null, "UNRESOLVABLE",
                false, false, false, false,
                false, List.of(),
                warnings, blockingIssues
        );
    }
}
