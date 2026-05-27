package com.worknest.features.companySite.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.common.api.PaginatedResponse;
import com.worknest.common.api.PaginationSupport;
import com.worknest.common.web.ClientIpResolver;
import com.worknest.features.companySite.application.CompanySiteCreationService;
import com.worknest.features.companySite.application.CompanySiteLifecycleService;
import com.worknest.features.companySite.application.CompanySiteQueryService;
import com.worknest.features.companySite.dto.CompanySiteDetailsResponse;
import com.worknest.features.companySite.dto.CompanySiteLookup;
import com.worknest.features.companySite.dto.CompanySiteResponse;
import com.worknest.features.companySite.dto.CreateSiteRequest;
import com.worknest.features.companySite.dto.CreateSiteResponse;
import com.worknest.features.companySite.dto.DetectLocationRequest;
import com.worknest.features.companySite.dto.DetectLocationResponse;
import com.worknest.features.companySite.dto.LocationDetailsReadDto;
import com.worknest.features.companySite.dto.LocationDetailsUpdateRequest;
import com.worknest.features.companySite.dto.MainDetailsReadDto;
import com.worknest.features.companySite.dto.MainDetailsUpdateRequest;
import com.worknest.features.companySite.dto.UpdateNetworkRequest;
import com.worknest.features.companySite.dto.TrustedNetworkResponse;
import com.worknest.features.companySite.application.CompanySiteLocationService;
import com.worknest.features.companySite.application.CompanySiteUpdateService;
import com.worknest.features.companySite.application.SiteTrustedNetworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * Company site creation endpoint.
 *
 * <p>Exposes a single one-shot create endpoint:
 * <pre>POST /api/v1/companies/{companyId}/sites</pre>
 *
 * <p>The request payload contains all site business data, the finalized
 * location fields (lat/lng + address + geofence), and optionally a list of
 * trusted-network rules. Everything is validated and persisted atomically.
 *
 * <p>No draft state is created. The resulting site status is {@code PENDING_REVIEW}.
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/sites")
@RequiredArgsConstructor
@Tag(name = "Company Sites", description = "One-shot company site creation API")
public class CompanySiteController {

    private final CompanySiteCreationService creationService;
    private final CompanySiteQueryService    queryService;
    private final CompanySiteLifecycleService lifecycleService;
    private final CompanySiteUpdateService updateService;
    private final CompanySiteLocationService locationService;
    private final SiteTrustedNetworkService networkService;

    @GetMapping
    @PreAuthorize("@teamSecurity.hasPermission(#companyId, 'COMPANY_SITE_VIEW')")
    @Operation(
            summary = "List company sites",
            description = "Returns all sites for the given company ordered by createdAt descending. " +
                          "The response includes the full site payload needed by the frontend list and future edit-prefill flows."
    )
    public ApiResponse<PaginatedResponse<CompanySiteResponse>> listSites(
            @PathVariable UUID companyId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success(
                "Sites retrieved successfully",
                PaginatedResponse.from(queryService.listSites(companyId, PaginationSupport.pageable(page, size)))
        );
    }

    @GetMapping("/{siteId}")
    @PreAuthorize("@teamSecurity.hasPermission(#companyId, 'COMPANY_SITE_VIEW')")
    @Operation(
            summary = "Get company site details",
            description = "Returns comprehensive details for a specific site, including basic info, location, " +
                          "and all configured trusted-network rules."
    )
    public ApiResponse<CompanySiteDetailsResponse> getSiteDetails(
            @PathVariable UUID companyId,
            @PathVariable UUID siteId
    ) {
        return ApiResponse.success("Site details retrieved successfully", queryService.getSiteDetails(companyId, siteId));
    }

    @GetMapping("/lookup")
    @PreAuthorize("@teamSecurity.hasPermission(#companyId, 'COMPANY_SITE_VIEW')")
    @Operation(
            summary = "Lookup company sites",
            description = "Returns a lightweight id/code/name list of all ACTIVE sites for the given company (e.g. for dropdowns)."
    )
    public ApiResponse<List<CompanySiteLookup>> lookupSites(@PathVariable UUID companyId) {
        return ApiResponse.success("Sites lookup successful", queryService.lookupSites(companyId));
    }

    @GetMapping("/{siteId}/main-details")
    @PreAuthorize("@teamSecurity.hasPermission(#companyId, 'COMPANY_SITE_VIEW')")
    @Operation(
            summary = "Get main details of a site",
            description = "Returns only the main details required for prefilling the Update Main Details modal, minimizing payload."
    )
    public ApiResponse<MainDetailsReadDto> getSiteMainDetails(
            @PathVariable UUID companyId,
            @PathVariable UUID siteId
    ) {
        return ApiResponse.success("Site main details retrieved successfully", updateService.getMainDetails(companyId, siteId));
    }

    @PutMapping("/{siteId}/main-details")
    @PreAuthorize("@teamSecurity.hasPermission(#companyId, 'COMPANY_SITE_UPDATE')")
    @Operation(
            summary = "Update site main details",
            description = "Updates only the main details of an existing site. Strict optimistic locking and business rules apply."
    )
    public ApiResponse<MainDetailsReadDto> updateSiteMainDetails(
            @PathVariable UUID companyId,
            @PathVariable UUID siteId,
            @Valid @RequestBody MainDetailsUpdateRequest request
    ) {
        MainDetailsReadDto updated = updateService.updateMainDetails(companyId, siteId, request);
        return ApiResponse.success("Site main details updated successfully", updated);
    }

    @GetMapping("/{siteId}/location")
    @PreAuthorize("@teamSecurity.hasPermission(#companyId, 'COMPANY_SITE_VIEW')")
    @Operation(
            summary = "Get location details of a site",
            description = "Returns the location details to prefill the Update Location modal."
    )
    public ApiResponse<LocationDetailsReadDto> getSiteLocation(
            @PathVariable UUID companyId,
            @PathVariable UUID siteId
    ) {
        return ApiResponse.success("Site location details retrieved successfully", locationService.getLocation(companyId, siteId));
    }

    @PutMapping("/{siteId}/location")
    @PreAuthorize("@teamSecurity.hasPermission(#companyId, 'COMPANY_SITE_UPDATE')")
    @Operation(
            summary = "Update site location",
            description = "Replaces the site's location and geofence state. Strict normalization rules override client mismatches."
    )
    public ApiResponse<LocationDetailsReadDto> updateSiteLocation(
            @PathVariable UUID companyId,
            @PathVariable UUID siteId,
            @Valid @RequestBody LocationDetailsUpdateRequest request
    ) {
        LocationDetailsReadDto updated = locationService.updateLocation(companyId, siteId, request);
        return ApiResponse.success("Site location updated successfully", updated);
    }

    @PostMapping("/{siteId}/detect-location")
    @PreAuthorize("@teamSecurity.hasPermission(#companyId, 'COMPANY_SITE_UPDATE')")
    @Operation(
            summary = "Assess browser coordinates against geofence",
            description = "Assesses transient coordinates from the frontend map against the current site boundaries to provide UI feedback."
    )
    public ApiResponse<DetectLocationResponse> detectLocation(
             @PathVariable UUID companyId,
             @PathVariable UUID siteId,
             @Valid @RequestBody DetectLocationRequest request) {
        return ApiResponse.success("Location assessed", locationService.assessLocation(companyId, siteId, request));
    }

    // ── Network Management Endpoints ──────────────────────────────────────────

    @GetMapping("/{siteId}/networks")
    @PreAuthorize("@teamSecurity.hasPermission(#companyId, 'COMPANY_SITE_VIEW')")
    @Operation(summary = "List site trusted networks", description = "Returns all trusted networks for a specific site.")
    public ApiResponse<PaginatedResponse<TrustedNetworkResponse>> listNetworks(
            @PathVariable UUID companyId,
            @PathVariable UUID siteId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success(
                "Networks retrieved successfully",
                PaginatedResponse.from(networkService.listNetworks(companyId, siteId, PaginationSupport.pageable(page, size)))
        );
    }

    @PutMapping("/{siteId}/networks/{networkId}")
    @PreAuthorize("@teamSecurity.hasPermission(#companyId, 'COMPANY_SITE_UPDATE')")
    @Operation(summary = "Update trusted network", description = "Updates an existing trusted network rule.")
    public ApiResponse<TrustedNetworkResponse> updateNetwork(
            @PathVariable UUID companyId,
            @PathVariable UUID siteId,
            @PathVariable UUID networkId,
            @Valid @RequestBody UpdateNetworkRequest request
    ) {
        return ApiResponse.success("Network updated successfully", networkService.updateNetwork(companyId, siteId, networkId, request));
    }

    /**
     * POST /api/v1/companies/{companyId}/sites
     *
     * <p>Creates a new company site together with its optional trusted-network rules
     * in a single server-side transaction. Strict validation is performed before
     * any persistence. Returns the full site snapshot on success (HTTP 201).
     *
     * <p>Security: only ADMIN or SUPERADMIN of the owning company may call this.
     * The {@code companyId} path variable is cross-validated against the authenticated
     * tenant context in the service layer.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@teamSecurity.hasPermission(#companyId, 'COMPANY_SITE_CREATE')")
    @Operation(
            summary = "Create company site",
            description = "One-shot endpoint that creates a site, its geofence, and optional trusted-network rules " +
                          "atomically. No draft state is created. The site status will be PENDING_REVIEW after creation."
    )
    public ApiResponse<CreateSiteResponse> createSite(
            @PathVariable UUID companyId,
            @Valid @RequestBody CreateSiteRequest request,
            HttpServletRequest httpRequest
    ) {
        String actorIp = ClientIpResolver.resolve(httpRequest);
        CreateSiteResponse response = creationService.createSite(companyId, request, actorIp);
        return ApiResponse.success("Site created successfully", response);
    }

    @PostMapping("/{siteId}/activate")
    @PreAuthorize("@teamSecurity.hasPermission(#companyId, 'COMPANY_SITE_UPDATE')")
    @Operation(
            summary = "Activate company site",
            description = "Transitions a site from PENDING_REVIEW or DISABLED to ACTIVE. " +
                          "Once active, attendance verification and network enforcement are applied."
    )
    public ApiResponse<CompanySiteResponse> activateSite(
            @PathVariable UUID companyId,
            @PathVariable UUID siteId,
            HttpServletRequest httpRequest
    ) {
        String actorIp = ClientIpResolver.resolve(httpRequest);
        CompanySiteResponse response = lifecycleService.activateSite(companyId, siteId, actorIp);
        return ApiResponse.success("Site activated successfully", response);
    }

    @PostMapping("/{siteId}/disable")
    @PreAuthorize("@teamSecurity.hasPermission(#companyId, 'COMPANY_SITE_UPDATE')")
    @Operation(
            summary = "Disable company site",
            description = "Administratively disables an ACTIVE site. Geofence and network checks will no longer permit " +
                          "attendance actions for this site until it is re-activated."
    )
    public ApiResponse<CompanySiteResponse> disableSite(
            @PathVariable UUID companyId,
            @PathVariable UUID siteId,
            HttpServletRequest httpRequest
    ) {
        String actorIp = ClientIpResolver.resolve(httpRequest);
        CompanySiteResponse response = lifecycleService.disableSite(companyId, siteId, actorIp);
        return ApiResponse.success("Site disabled successfully", response);
    }
}
