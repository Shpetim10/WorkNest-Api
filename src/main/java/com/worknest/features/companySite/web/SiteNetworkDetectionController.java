package com.worknest.features.companySite.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.companySite.application.SiteNetworkDetectionService;
import com.worknest.features.companySite.dto.DetectNetworkResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Advisory network-detection endpoint.
 *
 * <p>The frontend calls this endpoint once per setup session to pre-fill the
 * trusted-network form. The server reads the client IP from the request context
 * (never from the request body) and returns a suggestion DTO.
 *
 * <p><strong>Nothing is persisted.</strong>
 */
@RestController
@RequestMapping("/api/v1/site-network")
@RequiredArgsConstructor
@Tag(name = "Site Network Detection", description = "Advisory endpoint to detect the caller's network and suggest trusted-network rule fields. Read-only; never persists.")
public class SiteNetworkDetectionController {

    private final SiteNetworkDetectionService detectionService;

    /**
     * POST /api/v1/site-network/detect
     *
     * <p>Resolves the client IP server-side and returns a pre-filled suggestion DTO.
     * The frontend shows these values in the trusted-network form; the admin can
     * edit them before submitting the final create-site request.
     */
    @PostMapping("/detect")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
            summary = "Detect network",
            description = "Resolves the caller's IP server-side and returns a suggested trusted-network rule " +
                          "(observedIp, normalizedIp, suggestedCidr, ipVersion, networkType, priorityOrder, warnings, hints). " +
                          "Nothing is persisted. The result is advisory only."
    )
    public ApiResponse<DetectNetworkResponse> detect(HttpServletRequest request) {
        DetectNetworkResponse response = detectionService.detect(request);
        return ApiResponse.success("Network detected successfully", response);
    }
}
