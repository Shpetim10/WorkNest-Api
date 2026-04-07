package com.worknest.features.company.application;

import com.worknest.features.company.dto.LocationDetectionRequest;
import com.worknest.features.company.dto.LocationDetectionResult;
import com.worknest.features.company.dto.NetworkDetectionResult;
import java.util.UUID;

/**
 * Server-side detection contract for the site setup wizard.
 *
 * <p>Both operations are read-only from a persistence perspective —
 * they produce advisory results that the admin reviews before committing
 * values via the idempotent PUT step-save endpoints.
 */
public interface SiteDetectionService {

    /**
     * Assesses browser-supplied geolocation coordinates for staleness and accuracy.
     *
     * <p>The returned result is advisory — coordinates are NOT persisted by this call.
     *
     * @param siteId  the site being configured (used to read maxLocationAccuracyMeters)
     * @param request the raw Geolocation API output from the browser
     * @return server assessment with staleness flag, accuracy flag, suggested radius,
     *         and structured warnings
     */
    LocationDetectionResult detectLocation(UUID siteId, LocationDetectionRequest request);

    /**
     * Detects the IP address seen by the server for the current authenticated request,
     * assesses it for reliability concerns, and checks it against existing trusted-network
     * rules on the site for CIDR overlap.
     *
     * <p>Detection is entirely server-side — the client-observed IP is never trusted
     * as the authoritative source.
     *
     * @param siteId           the site being configured
     * @param clientIp         the IP address resolved from the HTTP request by the controller
     *                         (after X-Forwarded-For processing)
     * @param excludeNetworkId optional UUID of the network rule being updated; its CIDR is
     *                         excluded from the overlap comparison (prevents false self-overlap)
     * @return detection result with IP details, confidence, flags, overlap report,
     *         and structured warnings / blocking issues
     */
    NetworkDetectionResult detectNetwork(UUID siteId, String clientIp, UUID excludeNetworkId);
}
