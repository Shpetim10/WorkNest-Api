package com.worknest.features.company.web;

import com.worknest.common.api.ApiErrorResponse;
import com.worknest.common.api.ApiResponse;
import com.worknest.features.company.application.CompanySiteSetupService;
import com.worknest.features.company.application.SiteDetectionService;
import com.worknest.features.company.dto.CompanySiteResponse;
import com.worknest.features.company.dto.CreateSiteDraftRequest;
import com.worknest.features.company.dto.LocationDetectionRequest;
import com.worknest.features.company.dto.LocationDetectionResult;
import com.worknest.features.company.dto.NetworkDetectionResult;
import com.worknest.features.company.dto.SiteActivationResponse;
import com.worknest.features.company.dto.SiteBasicInfoRequest;
import com.worknest.features.company.dto.SiteLocationRequest;
import com.worknest.features.company.dto.SiteSetupStatusResponse;
import com.worknest.features.company.dto.TrustedNetworkResponse;
import com.worknest.features.company.dto.TrustedNetworkUpsertRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Company Sites", description = "Draft-to-activation company site setup API")
public class CompanySiteController {

    private final CompanySiteSetupService companySiteSetupService;
    private final SiteDetectionService siteDetectionService;

    @PostMapping("/companies/{companyId}/sites")
    @PreAuthorize("@companySecurity.hasCompanyRole(#companyId, 'ADMIN', 'SUPERADMIN')")
    @Operation(
            summary = "Create a site draft",
            description = "Creates a new DRAFT site under the company. This is the only create operation; subsequent setup steps are idempotent PUT saves."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Site draft created")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Duplicate site code or concurrent conflict",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
    )
    public ResponseEntity<ApiResponse<CompanySiteResponse>> createDraft(
            @PathVariable UUID companyId,
            @Valid @RequestBody CreateSiteDraftRequest request
    ) {
        CompanySiteResponse response = companySiteSetupService.createDraft(companyId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Site draft created successfully", response));
    }

    @PutMapping("/sites/{siteId}/basic-info")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
            summary = "Save basic site info",
            description = "Idempotently saves the site basic-information step. Repeating the same payload produces the same persisted state."
    )
    public ApiResponse<CompanySiteResponse> saveBasicInfo(
            @PathVariable UUID siteId,
            @Valid @RequestBody SiteBasicInfoRequest request
    ) {
        return ApiResponse.success(
                "Site basic information saved successfully",
                companySiteSetupService.saveBasicInfo(siteId, request)
        );
    }

    @PutMapping("/sites/{siteId}/location")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
            summary = "Save site location",
            description = "Idempotently saves the location and geofence step. Draft saves accept partial location data; strict completeness is checked only during activation."
    )
    public ApiResponse<CompanySiteResponse> saveLocation(
            @PathVariable UUID siteId,
            @Valid @RequestBody SiteLocationRequest request
    ) {
        return ApiResponse.success(
                "Site location saved successfully",
                companySiteSetupService.saveLocation(siteId, request)
        );
    }

    @PutMapping("/sites/{siteId}/trusted-networks/{trustedNetworkId}")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
            summary = "Upsert a trusted network rule",
            description = "Idempotently creates or updates a site trusted-network rule identified by the path UUID."
    )
    public ApiResponse<TrustedNetworkResponse> upsertTrustedNetwork(
            @PathVariable UUID siteId,
            @PathVariable UUID trustedNetworkId,
            @Valid @RequestBody TrustedNetworkUpsertRequest request
    ) {
        return ApiResponse.success(
                "Trusted network saved successfully",
                companySiteSetupService.upsertTrustedNetwork(siteId, trustedNetworkId, request)
        );
    }

    @GetMapping("/sites/{siteId}/setup-status")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
            summary = "Get setup status",
            description = "Returns resumability state, computed completeness flags, blocking issues, warnings, and the current site/network snapshot."
    )
    public ApiResponse<SiteSetupStatusResponse> getSetupStatus(@PathVariable UUID siteId) {
        return ApiResponse.success(
                "Site setup status retrieved successfully",
                companySiteSetupService.getSetupStatus(siteId)
        );
    }

    @PostMapping("/sites/{siteId}/activate")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
            summary = "Dry-run or activate a site",
            description = "When dryRun=true, validates activation readiness without mutating status. When omitted or false, transitions the site to ACTIVE if validation passes."
    )
    public ApiResponse<SiteActivationResponse> activate(
            @PathVariable UUID siteId,
            @RequestParam(name = "dryRun", defaultValue = "false") boolean dryRun
    ) {
        return ApiResponse.success(
                dryRun ? "Site activation validated successfully" : "Site activated successfully",
                companySiteSetupService.activate(siteId, dryRun)
        );
    }

    // ─────────────────────────────────────────────────────────────────────
    // Detection endpoints
    // ─────────────────────────────────────────────────────────────────────

    @PostMapping("/sites/{siteId}/detect-location")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
            summary = "Assess browser geolocation coordinates",
            description = """
                    Accepts raw browser Geolocation API output and performs server-side assessment:
                    - Staleness check: warns if |now - browserTimestamp| > 30 s
                    - Accuracy check: warns if reported accuracy exceeds the site threshold (or 100 m default)
                    - Suggests a geofence radius: (maxAccuracy × 2) + 30 m

                    **Result is advisory only** — coordinates are NOT persisted by this call.
                    Commit confirmed values via PUT /api/v1/sites/{siteId}/location.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Assessment completed — inspect warnings before confirming")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Caller is not an ADMIN/SUPERADMIN of the owning company",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Site not found or not owned by the caller's company",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
    )
    public ApiResponse<LocationDetectionResult> detectLocation(
            @PathVariable UUID siteId,
            @Valid @RequestBody LocationDetectionRequest request
    ) {
        return ApiResponse.success(
                "Location assessment completed",
                siteDetectionService.detectLocation(siteId, request)
        );
    }

    @PostMapping("/sites/{siteId}/detect-network")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
            summary = "Detect server-observed network and assess reliability",
            description = """
                    Captures the IP address seen by the server from the authenticated request
                    (never trusting client-supplied IPs) and performs:
                    - CGNAT / RFC 1918 detection (amber warning)
                    - IPv6 privacy-address detection (amber warning)
                    - VPN / datacenter ASN heuristic (amber warning; Phase 2 ASN integration)
                    - Tor exit-node detection (blocking; Phase 2 list integration)
                    - CIDR overlap check against all active trusted-network rules on the site

                    Use `excludeNetworkId` when updating an existing rule to suppress
                    false self-overlap alerts.

                    **Result is advisory only** — rules are NOT persisted by this call.
                    Commit the confirmed rule via PUT /api/v1/sites/{siteId}/trusted-networks/{id}.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Detection completed — inspect confidence and warnings before confirming")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Caller is not an ADMIN/SUPERADMIN of the owning company",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Site not found or not owned by the caller's company",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
    )
    public ApiResponse<NetworkDetectionResult> detectNetwork(
            @PathVariable UUID siteId,
            @RequestParam(name = "excludeNetworkId", required = false) UUID excludeNetworkId,
            HttpServletRequest httpRequest
    ) {
        String clientIp = resolveClientIp(httpRequest);
        return ApiResponse.success(
                "Network detection completed",
                siteDetectionService.detectNetwork(siteId, clientIp, excludeNetworkId)
        );
    }

    // ─────────────────────────────────────────────────────────────────────
    // IP resolution helper (mirrors UserInvitationController pattern)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Resolves the real client IP, respecting common reverse-proxy headers.
     * The first address in the X-Forwarded-For chain is the originating client.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
