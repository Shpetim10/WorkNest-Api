package com.worknest.features.companySite.application;

import com.worknest.features.companySite.dto.DetectNetworkResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Advisory network-detection service.
 *
 * <p>Reads the trusted client IP from the server-resolved request context,
 * derives network metadata (normalized IP, suggested CIDR, IP version,
 * network type, priority order), and returns the result as a suggestion DTO.
 *
 * <p><strong>Nothing is persisted by this service.</strong> The result is
 * returned to the frontend to pre-fill the trusted-network form only.
 */
public interface SiteNetworkDetectionService {

    /**
     * Resolves client network metadata from the incoming HTTP request.
     *
     * @param request the current HTTP request; the client IP is determined
     *                server-side using the {@link com.worknest.common.web.ClientIpResolver}
     *                priority chain (CF-Connecting-IP → X-Real-IP → X-Forwarded-For → RemoteAddr).
     * @return a suggestion DTO containing the detected IP, suggested CIDR, warnings, and hints.
     *         Never {@code null}; never persisted.
     */
    DetectNetworkResponse detect(HttpServletRequest request);
}
