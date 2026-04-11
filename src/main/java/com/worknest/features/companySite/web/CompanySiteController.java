package com.worknest.features.companySite.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.common.web.ClientIpResolver;
import com.worknest.features.companySite.application.CompanySiteCreationService;
import com.worknest.features.companySite.application.CompanySiteQueryService;
import com.worknest.features.companySite.dto.CompanySiteResponse;
import com.worknest.features.companySite.dto.CreateSiteRequest;
import com.worknest.features.companySite.dto.CreateSiteResponse;
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

    @GetMapping
    @PreAuthorize("@companySecurity.hasCompanyRole(#companyId, 'ADMIN', 'SUPERADMIN')")
    @Operation(
            summary = "List company sites",
            description = "Returns all sites for the given company ordered by createdAt descending. " +
                          "The response includes the full site payload needed by the frontend list and future edit-prefill flows."
    )
    public ApiResponse<List<CompanySiteResponse>> listSites(@PathVariable UUID companyId) {
        return ApiResponse.success("Sites retrieved successfully", queryService.listSites(companyId));
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
    @PreAuthorize("@companySecurity.hasCompanyRole(#companyId, 'ADMIN', 'SUPERADMIN')")
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
    @PreAuthorize("@companySecurity.hasCompanyRole(#companyId, 'ADMIN', 'SUPERADMIN')")
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
    @PreAuthorize("@companySecurity.hasCompanyRole(#companyId, 'ADMIN', 'SUPERADMIN')")
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
