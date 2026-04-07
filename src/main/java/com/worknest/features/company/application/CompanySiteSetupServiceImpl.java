package com.worknest.features.company.application;

import com.worknest.audit.service.SiteSetupAuditService;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.entities.SiteTrustedNetwork;
import com.worknest.domain.enums.GeofenceShapeType;
import com.worknest.domain.enums.NetworkIpVersion;
import com.worknest.domain.enums.SiteStatus;
import com.worknest.features.company.dto.CompanySiteResponse;
import com.worknest.features.company.dto.CreateSiteDraftRequest;
import com.worknest.features.company.dto.SiteActivationResponse;
import com.worknest.features.company.dto.SiteBasicInfoRequest;
import com.worknest.features.company.dto.SiteLocationRequest;
import com.worknest.features.company.dto.SiteSetupIssueResponse;
import com.worknest.features.company.dto.SiteSetupStatusResponse;
import com.worknest.features.company.dto.TrustedNetworkResponse;
import com.worknest.features.company.dto.TrustedNetworkUpsertRequest;
import com.worknest.features.company.exception.CompanySiteActivationBlockedException;
import com.worknest.features.company.exception.CompanySiteCodeAlreadyExistsException;
import com.worknest.features.company.exception.CompanySiteConflictException;
import com.worknest.features.company.exception.CompanySiteNotFoundException;
import com.worknest.features.company.exception.CompanySiteParentCompanyNotFoundException;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.company.repository.CompanySiteRepository;
import com.worknest.features.company.repository.SiteTrustedNetworkRepository;
import com.worknest.security.AuthSessionPrincipal;
import com.worknest.tenant.TenantContextHolder;
import com.worknest.tenant.TenantSessionContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CompanySiteSetupServiceImpl implements CompanySiteSetupService {

    private final CompanyRepository companyRepository;
    private final CompanySiteRepository companySiteRepository;
    private final SiteTrustedNetworkRepository siteTrustedNetworkRepository;
    private final SiteSetupAuditService siteSetupAuditService;
    // Used for CIDR validation at upsert time. Same package, so package-private method is accessible.
    private final SiteDetectionServiceImpl siteDetectionService;

    @Override
    @Transactional
    public CompanySiteResponse createDraft(UUID companyId, CreateSiteDraftRequest request) {
        requireTenantCompany(companyId);
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanySiteParentCompanyNotFoundException(companyId));

        String normalizedCode = normalizeCode(request.code());
        if (companySiteRepository.existsByCompanyIdAndCodeIgnoreCase(companyId, normalizedCode)) {
            throw new CompanySiteCodeAlreadyExistsException(normalizedCode);
        }

        CompanySite site = new CompanySite();
        site.setCompany(company);
        site.setCode(normalizedCode);
        site.setName(request.name().trim());
        site.setType(request.type());
        site.setCountryCode(request.countryCode().trim().toUpperCase(Locale.ROOT));
        site.setTimezone(request.timezone().trim());
        site.setNotes(trimToNull(request.notes()));
        site.setStatus(SiteStatus.DRAFT);

        CompanySite saved = companySiteRepository.save(site);

        // Audit: site draft created
        TenantSessionContext ctx = tenantContext();
        siteSetupAuditService.appendSiteDraftCreated(
                companyId,
                saved.getId(),
                saved.getCode(),
                currentUserId(),
                ctx.roleAssignmentId(),
                null
        );

        return CompanySiteResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public CompanySiteResponse saveBasicInfo(UUID siteId, SiteBasicInfoRequest request) {
        CompanySite site = getOwnedSite(siteId);
        assertSiteVersion(site, request.version());

        String normalizedCode = normalizeCode(request.code());
        if (companySiteRepository.existsByCompanyIdAndCodeIgnoreCaseAndIdNot(currentCompanyId(), normalizedCode, siteId)) {
            throw new CompanySiteCodeAlreadyExistsException(normalizedCode);
        }

        site.setCode(normalizedCode);
        site.setName(request.name().trim());
        site.setType(request.type());
        site.setAddressLine1(trimToNull(request.addressLine1()));
        site.setAddressLine2(trimToNull(request.addressLine2()));
        site.setCity(trimToNull(request.city()));
        site.setStateRegion(trimToNull(request.stateRegion()));
        site.setPostalCode(trimToNull(request.postalCode()));
        site.setCountryCode(request.countryCode().trim().toUpperCase(Locale.ROOT));
        site.setTimezone(request.timezone().trim());
        site.setCheckInEnabled(defaultIfNull(request.checkInEnabled(), site.getCheckInEnabled(), true));
        site.setCheckOutEnabled(defaultIfNull(request.checkOutEnabled(), site.getCheckOutEnabled(), true));
        site.setQrEnabled(defaultIfNull(request.qrEnabled(), site.getQrEnabled(), true));
        site.setNotes(trimToNull(request.notes()));

        CompanySite saved = companySiteRepository.save(site);

        // Audit: basic info step saved
        TenantSessionContext ctx = tenantContext();
        siteSetupAuditService.appendSiteBasicInfoSaved(
                ctx.companyId(),
                siteId,
                saved.getCode(),
                currentUserId(),
                ctx.roleAssignmentId(),
                null
        );

        return CompanySiteResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public CompanySiteResponse saveLocation(UUID siteId, SiteLocationRequest request) {
        CompanySite site = getOwnedSite(siteId);
        assertSiteVersion(site, request.version());

        boolean locationRequired = defaultIfNull(request.locationRequired(), site.getLocationRequired(), true);
        site.setLocationRequired(locationRequired);
        site.setGeofenceShapeType(request.geofenceShapeType());
        site.setLatitude(request.latitude());
        site.setLongitude(request.longitude());
        site.setGeofenceRadiusMeters(request.geofenceRadiusMeters());
        site.setGeofencePolygonGeoJson(trimToNull(request.geofencePolygonGeoJson()));
        site.setEntryBufferMeters(request.entryBufferMeters());
        site.setExitBufferMeters(request.exitBufferMeters());
        site.setMaxLocationAccuracyMeters(request.maxLocationAccuracyMeters());

        if (!locationRequired) {
            clearLocationFields(site);
        }

        CompanySite saved = companySiteRepository.save(site);

        // Audit: location step saved
        TenantSessionContext ctx = tenantContext();
        siteSetupAuditService.appendSiteLocationSaved(
                ctx.companyId(),
                siteId,
                locationRequired,
                currentUserId(),
                ctx.roleAssignmentId(),
                null
        );

        return CompanySiteResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public TrustedNetworkResponse upsertTrustedNetwork(UUID siteId, UUID trustedNetworkId, TrustedNetworkUpsertRequest request) {
        CompanySite site = getOwnedSite(siteId);
        SiteTrustedNetwork network = siteTrustedNetworkRepository.findByIdAndSiteId(trustedNetworkId, siteId)
                .orElseGet(() -> createNewTrustedNetwork(site, trustedNetworkId));

        if (network.getId() != null && request.version() != null && !request.version().equals(network.getVersion())) {
            throw new CompanySiteConflictException("Trusted network was updated in another session. Refresh and try again.");
        }

        // ── CIDR validation (format + overlap) ──────────────────────────
        String cidrValidationError = CidrValidator.validate(request.cidrBlock());
        if (cidrValidationError != null) {
            throw new CompanySiteConflictException("Invalid CIDR block: " + cidrValidationError);
        }
        List<SiteSetupIssueResponse> cidrIssues =
                siteDetectionService.validateCidrForUpsert(siteId, request.cidrBlock().trim(), trustedNetworkId);
        if (!cidrIssues.isEmpty()) {
            // Surface only the first issue as a conflict message to keep the response clean
            throw new CompanySiteConflictException(cidrIssues.get(0).message());
        }

        boolean wasCreated = (network.getVersion() == null || network.getVersion() == 0L)
                && network.getName() == null;

        network.setName(request.name().trim());
        network.setNetworkType(request.networkType());
        network.setCidrBlock(CidrValidator.normalize(request.cidrBlock()));
        network.setIpVersion(CidrValidator.deriveIpVersion(request.cidrBlock()));
        network.setIsActive(defaultIfNull(request.isActive(), network.getIsActive(), true));
        network.setPriorityOrder(request.priorityOrder());
        network.setExpiresAt(request.expiresAt());
        network.setNotes(trimToNull(request.notes()));

        SiteTrustedNetwork savedNetwork = siteTrustedNetworkRepository.save(network);

        // Audit: trusted network upserted
        TenantSessionContext ctx = tenantContext();
        siteSetupAuditService.appendTrustedNetworkUpserted(
                ctx.companyId(),
                siteId,
                savedNetwork.getId(),
                savedNetwork.getCidrBlock(),
                wasCreated,
                currentUserId(),
                ctx.roleAssignmentId(),
                null
        );

        return TrustedNetworkResponse.fromEntity(savedNetwork);
    }

    @Override
    @Transactional(readOnly = true)
    public SiteSetupStatusResponse getSetupStatus(UUID siteId) {
        CompanySite site = getOwnedSite(siteId);
        List<TrustedNetworkResponse> trustedNetworks = siteTrustedNetworkRepository.findAllBySiteIdOrderByPriorityOrderAscIdAsc(siteId)
                .stream()
                .map(TrustedNetworkResponse::fromEntity)
                .toList();

        ValidationResult validation = validateSiteSetup(site, trustedNetworks);
        return new SiteSetupStatusResponse(
                site.getId(),
                site.getStatus(),
                site.getVersion(),
                validation.basicInfoComplete(),
                validation.locationComplete(),
                validation.networkComplete(),
                validation.readyToActivate(),
                validation.blockingIssues(),
                validation.warnings(),
                CompanySiteResponse.fromEntity(site),
                trustedNetworks
        );
    }

    @Override
    @Transactional
    public SiteActivationResponse activate(UUID siteId, boolean dryRun) {
        CompanySite site = getOwnedSite(siteId);
        List<TrustedNetworkResponse> trustedNetworks = siteTrustedNetworkRepository.findAllBySiteIdOrderByPriorityOrderAscIdAsc(siteId)
                .stream()
                .map(TrustedNetworkResponse::fromEntity)
                .toList();

        ValidationResult validation = validateSiteSetup(site, trustedNetworks);
        if (!validation.readyToActivate()) {
            throw new CompanySiteActivationBlockedException(validation.blockingIssues());
        }

        boolean activated = false;
        if (!dryRun && site.getStatus() != SiteStatus.ACTIVE) {
            site.setStatus(SiteStatus.ACTIVE);
            site = companySiteRepository.save(site);
            activated = true;
        }

        // Audit: activation attempted (dry-run or real)
        TenantSessionContext ctx = tenantContext();
        siteSetupAuditService.appendSiteActivationAttempted(
                ctx.companyId(),
                siteId,
                dryRun,
                activated,
                currentUserId(),
                ctx.roleAssignmentId(),
                null
        );

        return new SiteActivationResponse(
                site.getId(),
                dryRun,
                activated,
                site.getStatus(),
                validation.readyToActivate(),
                validation.blockingIssues(),
                validation.warnings(),
                CompanySiteResponse.fromEntity(site)
        );
    }

    private ValidationResult validateSiteSetup(CompanySite site, List<TrustedNetworkResponse> trustedNetworks) {
        List<SiteSetupIssueResponse> blockingIssues = new ArrayList<>();
        List<SiteSetupIssueResponse> warnings = new ArrayList<>();

        boolean basicInfoComplete = hasText(site.getCode())
                && hasText(site.getName())
                && site.getType() != null
                && hasText(site.getCountryCode())
                && hasText(site.getTimezone());
        if (!basicInfoComplete) {
            blockingIssues.add(issue("SITE_BASIC_INFO_INCOMPLETE", "Basic site details are incomplete.", "basicInfo"));
        }

        boolean locationComplete = isLocationComplete(site);
        if (!locationComplete) {
            blockingIssues.add(issue("SITE_LOCATION_INCOMPLETE", "Location and geofence configuration is incomplete.", "location"));
        }

        boolean hasActiveNetwork = trustedNetworks.stream().anyMatch(network -> Boolean.TRUE.equals(network.isActive()));
        boolean networkComplete = hasActiveNetwork;
        if (!networkComplete) {
            blockingIssues.add(issue("SITE_NETWORK_INCOMPLETE", "At least one active trusted network is required.", "trustedNetworks"));
        }

        Instant now = Instant.now();
        for (TrustedNetworkResponse network : trustedNetworks) {
            if (network.expiresAt() == null) {
                warnings.add(issue(
                        "TRUSTED_NETWORK_NO_EXPIRY",
                        "Trusted network '" + network.name() + "' has no expiry and will remain permanent until edited.",
                        "trustedNetworks"
                ));
            } else if (network.expiresAt().isBefore(now)) {
                blockingIssues.add(issue(
                        "TRUSTED_NETWORK_EXPIRED",
                        "Trusted network '" + network.name() + "' is expired and cannot be used for activation.",
                        "trustedNetworks"
                ));
            }
        }

        boolean readyToActivate = basicInfoComplete && locationComplete && networkComplete && blockingIssues.isEmpty();
        return new ValidationResult(basicInfoComplete, locationComplete, networkComplete, readyToActivate, blockingIssues, warnings);
    }

    private boolean isLocationComplete(CompanySite site) {
        if (!Boolean.TRUE.equals(site.getLocationRequired())) {
            return true;
        }
        if (site.getGeofenceShapeType() == null) {
            return false;
        }
        if (site.getGeofenceShapeType() == GeofenceShapeType.CIRCLE) {
            return site.getLatitude() != null && site.getLongitude() != null && site.getGeofenceRadiusMeters() != null;
        }
        if (site.getGeofenceShapeType() == GeofenceShapeType.POLYGON) {
            return hasText(site.getGeofencePolygonGeoJson());
        }
        return false;
    }

    private void clearLocationFields(CompanySite site) {
        site.setGeofenceShapeType(null);
        site.setLatitude(null);
        site.setLongitude(null);
        site.setGeofenceRadiusMeters(null);
        site.setGeofencePolygonGeoJson(null);
        site.setEntryBufferMeters(null);
        site.setExitBufferMeters(null);
        site.setMaxLocationAccuracyMeters(null);
    }

    private SiteTrustedNetwork createNewTrustedNetwork(CompanySite site, UUID trustedNetworkId) {
        SiteTrustedNetwork network = new SiteTrustedNetwork();
        network.setId(trustedNetworkId);
        network.setSite(site);
        return network;
    }

    private void assertSiteVersion(CompanySite site, Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(site.getVersion())) {
            throw new CompanySiteConflictException("Site was updated in another session. Refresh and try again.");
        }
    }

    private CompanySite getOwnedSite(UUID siteId) {
        return companySiteRepository.findByIdAndCompanyId(siteId, currentCompanyId())
                .orElseThrow(() -> new CompanySiteNotFoundException(siteId));
    }

    private TenantSessionContext tenantContext() {
        return TenantContextHolder.get()
                .orElseThrow(() -> new IllegalStateException("No tenant context found"));
    }

    private UUID currentCompanyId() {
        return tenantContext().companyId();
    }

    private void requireTenantCompany(UUID companyId) {
        if (!currentCompanyId().equals(companyId)) {
            throw new CompanySiteNotFoundException(companyId);
        }
    }

    /**
     * Resolves the authenticated user's UUID from the Spring Security context.
     * Returns null gracefully if the context is not populated (e.g. during tests).
     */
    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthSessionPrincipal principal) {
            return principal.userId();
        }
        return null;
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }

    private <T> T defaultIfNull(T incoming, T current, T fallback) {
        if (incoming != null) {
            return incoming;
        }
        if (current != null) {
            return current;
        }
        return fallback;
    }

    private NetworkIpVersion deriveIpVersion(String cidrBlock) {
        return CidrValidator.deriveIpVersion(cidrBlock);
    }

    private SiteSetupIssueResponse issue(String code, String message, String field) {
        return new SiteSetupIssueResponse(code, message, field);
    }

    private record ValidationResult(
            boolean basicInfoComplete,
            boolean locationComplete,
            boolean networkComplete,
            boolean readyToActivate,
            List<SiteSetupIssueResponse> blockingIssues,
            List<SiteSetupIssueResponse> warnings
    ) {
    }
}
