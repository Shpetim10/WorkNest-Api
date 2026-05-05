package com.worknest.features.companySite.application;

import com.worknest.audit.service.SiteSetupAuditService;
import com.worknest.common.api.FieldValidationError;
import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.AttendancePolicy;
import com.worknest.domain.entities.AttendanceQrTerminal;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.entities.SiteTrustedNetwork;
import com.worknest.domain.enums.AttendancePolicySource;
import com.worknest.domain.enums.SiteStatus;
import com.worknest.features.companySite.application.SiteAttendanceProvisioningPort;
import com.worknest.features.companySite.dto.CreateSiteRequest;
import com.worknest.features.companySite.dto.CreateSiteResponse;
import com.worknest.features.companySite.dto.LinkedQrTerminalResponse;
import com.worknest.features.companySite.dto.SiteAttendancePolicySummaryResponse;
import com.worknest.features.companySite.dto.TrustedNetworkRequest;
import com.worknest.features.companySite.dto.TrustedNetworkResponse;
import com.worknest.features.companySite.exception.CompanyNotFoundException;
import com.worknest.features.companySite.exception.InvalidCidrException;
import com.worknest.features.companySite.exception.InvalidGeofenceException;
import com.worknest.features.companySite.exception.SiteCodeAlreadyExistsException;
import com.worknest.features.companySite.exception.SiteCreationValidationException;
import com.worknest.features.companySite.repository.CompanySiteRepository;
import com.worknest.features.companySite.repository.SiteTrustedNetworkRepository;
import com.worknest.features.companySite.validation.CidrValidator;
import com.worknest.features.companySite.validation.GeofenceValidator;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.security.AuthSessionPrincipal;
import com.worknest.tenant.TenantContextHolder;
import jakarta.persistence.EntityManager;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link CompanySiteCreationService}.
 *
 * <p>Performs strict server-side validation in this order before any
 * database write is attempted:
 * <ol>
 *   <li>Verify the caller's tenant context maps to the requested {@code companyId}.</li>
 *   <li>Assert the company exists and is accessible.</li>
 *   <li>Assert the site code is unique for this company.</li>
 *   <li>Validate geofence completeness and consistency.</li>
 *   <li>Validate every CIDR block in the trusted-network list.</li>
 *   <li>Assert no two rules in the inbound list share the same CIDR + networkType.</li>
 * </ol>
 *
 * <p>All persistence (site + networks) happens inside a single
 * {@code @Transactional} boundary. The site status is set to
 * {@link SiteStatus#PENDING_REVIEW} — never DRAFT.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanySiteCreationServiceImpl implements CompanySiteCreationService {

    private final CompanySiteRepository            siteRepository;
    private final SiteTrustedNetworkRepository     networkRepository;
    private final CompanyRepository                companyRepository;
    private final SiteSetupAuditService            auditService;
    private final SiteAttendanceProvisioningPort   attendanceProvisioning;
    private final EntityManager                    entityManager;

    @Override
    @Transactional
    public CreateSiteResponse createSite(UUID companyId, CreateSiteRequest request, String actorIp) {

        // ── 1. Resolve actor identity ────────────────────────────────────────────
        AuthSessionPrincipal actor = resolveActor();

        // ── 2. Tenant-boundary check ─────────────────────────────────────────────
        UUID tenantCompanyId = TenantContextHolder.get()
                .orElseThrow(() -> new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "TENANT_CONTEXT_MISSING", "Security context violation: No tenant company identified for the current session."))
                .companyId();

        if (!tenantCompanyId.equals(companyId)) {
            // Requested company does not match the authenticated tenant — surface as 404
            // to avoid leaking cross-tenant existence information.
            throw new CompanyNotFoundException();
        }

        // ── 3. Company existence check ───────────────────────────────────────────
        if (!companyRepository.existsById(companyId)) {
            throw new CompanyNotFoundException();
        }

        // ── 4. Site code uniqueness ──────────────────────────────────────────────
        if (request.code() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MISSING_FIELD", "Site code is required");
        }
        String normalizedCode = request.code().trim().toUpperCase();
        if (siteRepository.existsByCompanyIdAndCode(companyId, normalizedCode)) {
            throw new SiteCodeAlreadyExistsException(normalizedCode);
        }

        List<TrustedNetworkRequest> networks = request.trustedNetworks() != null
                ? request.trustedNetworks()
                : List.of();

        validateCreateRequest(request, networks);

        // ── 7. Persist site ──────────────────────────────────────────────────────
        Company companyRef = entityManager.getReference(Company.class, companyId);
        CompanySite site   = buildSiteEntity(companyRef, request, normalizedCode);
        site = siteRepository.save(site);

        // ── 8. Persist trusted-network rules ─────────────────────────────────────
        List<SiteTrustedNetwork> networkEntities = new ArrayList<>();
        for (int i = 0; i < networks.size(); i++) {
            networkEntities.add(buildNetworkEntity(site, networks.get(i), i + 1));
        }
        List<SiteTrustedNetwork> savedNetworks = networkRepository.saveAll(networkEntities);

        AttendancePolicy attendancePolicy = buildAttendancePolicy(companyRef, site, request);
        attendancePolicy = attendanceProvisioning.savePolicy(attendancePolicy);
        AttendanceQrTerminal defaultTerminal = Boolean.TRUE.equals(attendancePolicy.getRequireQr())
                ? attendanceProvisioning.ensureDefaultTerminal(companyRef, site)
                : null;

        // ── 9. Audit ─────────────────────────────────────────────────────────────
        auditService.appendSiteDraftCreated(
                companyId,
                site.getId(),
                site.getCode(),
                actor.userId(),
                actor.roleAssignmentId(),
                actorIp
        );

        for (SiteTrustedNetwork rule : savedNetworks) {
            auditService.appendTrustedNetworkUpserted(
                    companyId,
                    site.getId(),
                    rule.getId(),
                    rule.getCidrBlock(),
                    true,
                    actor.userId(),
                    actor.roleAssignmentId(),
                    actorIp
            );
        }

        log.info("CompanySiteCreation: site '{}' (id={}) created for company={} with {} network rule(s)",
                site.getCode(), site.getId(), companyId, savedNetworks.size());

        // ── 10. Build and return response ─────────────────────────────────────────
        List<TrustedNetworkResponse> networkResponses = savedNetworks.stream()
                .map(TrustedNetworkResponse::fromEntity)
                .toList();

        return CreateSiteResponse.fromEntity(
                site,
                networkResponses,
                mapPolicySummary(attendancePolicy),
                mapTerminal(defaultTerminal)
        );
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Validates all trusted-network rules in the inbound list:
     * <ul>
     *   <li>CIDR syntax and IP-version cross-validation.</li>
     *   <li>No duplicate (cidrBlock + networkType) pairs within the same request.</li>
     * </ul>
     */
    private void validateCreateRequest(CreateSiteRequest request, List<TrustedNetworkRequest> networks) {
        List<FieldValidationError> fieldErrors = new ArrayList<>();

        validateTimezone(request.timezone(), fieldErrors);
        validateLocation(request, fieldErrors);
        validateTrustedNetworks(networks, fieldErrors);
        validateAttendancePolicy(request, fieldErrors);

        if (!fieldErrors.isEmpty()) {
            throw new SiteCreationValidationException(fieldErrors);
        }
    }

    private void validateTrustedNetworks(List<TrustedNetworkRequest> networks, List<FieldValidationError> fieldErrors) {
        Set<String> seenKeys = new HashSet<>();
        for (int i = 0; i < networks.size(); i++) {
            TrustedNetworkRequest net = networks.get(i);
            String fieldPrefix = "trustedNetworks[" + i + "]";
            boolean cidrValid = true;

            try {
                CidrValidator.validate(net.cidrBlock(), net.ipVersion());
            } catch (InvalidCidrException exception) {
                fieldErrors.add(new FieldValidationError(fieldPrefix + ".cidrBlock", exception.getMessage()));
                cidrValid = false;
            }

            if (cidrValid) {
                String dedupKey = CidrValidator.normalize(net.cidrBlock()) + "|" + net.networkType();
                if (!seenKeys.add(dedupKey)) {
                    fieldErrors.add(new FieldValidationError(
                            fieldPrefix + ".cidrBlock",
                            "This CIDR block is duplicated in the request for the same network type."
                    ));
                }
            }

            if (net.expiresAt() != null && net.expiresAt().isBefore(Instant.now())) {
                fieldErrors.add(new FieldValidationError(
                        fieldPrefix + ".expiresAt",
                        "Trusted network expiration must be in the future."
                ));
            }
        }
    }

    private void validateLocation(CreateSiteRequest request, List<FieldValidationError> fieldErrors) {
        try {
            GeofenceValidator.validate(request.location());
        } catch (InvalidGeofenceException exception) {
            fieldErrors.add(new FieldValidationError(inferGeofenceField(request), exception.getMessage()));
        }

        if (request.location().countryCode() != null
                && !request.location().countryCode().equalsIgnoreCase(request.countryCode())) {
            fieldErrors.add(new FieldValidationError(
                    "location.countryCode",
                    "Location country must match the site's primary country code."
            ));
        }
    }

    private void validateTimezone(String timezone, List<FieldValidationError> fieldErrors) {
        if (timezone == null || timezone.isBlank()) {
            return;
        }

        try {
            ZoneId.of(timezone.trim());
        } catch (DateTimeException exception) {
            fieldErrors.add(new FieldValidationError(
                    "timezone",
                    "Timezone must be a valid IANA timezone, for example 'Europe/Tirane'."
            ));
        }
    }

    /**
     * Constructs a {@link CompanySite} entity from the validated request.
     * Status is always set to {@link SiteStatus#PENDING_REVIEW}; never DRAFT.
     */
    private CompanySite buildSiteEntity(Company company, CreateSiteRequest req, String normalizedCode) {
        CompanySite site = new CompanySite();
        site.setCompany(company);
        site.setCode(normalizedCode);
        site.setName(req.name().trim());
        site.setType(req.type());
        site.setStatus(SiteStatus.PENDING_REVIEW);
        site.setCountryCode(req.countryCode().trim().toUpperCase());
        site.setTimezone(req.timezone() != null ? req.timezone().trim() : "UTC");
        site.setNotes(req.notes());

        // Legacy site-level toggles are kept in sync with the attendance policy for backward compatibility.
        site.setLocationRequired(req.attendancePolicy().requireLocation());
        site.setQrEnabled(req.attendancePolicy().requireQr());
        site.setCheckInEnabled(req.attendancePolicy().checkInEnabled());
        site.setCheckOutEnabled(req.attendancePolicy().checkOutEnabled());

        // Location — authoritative fields
        var loc = req.location();
        site.setLatitude(loc.latitude());
        site.setLongitude(loc.longitude());
        site.setLocationDetectionSource(loc.locationDetectionSource());

        // Address — UX display only, not used for geofence evaluation
        site.setAddressLine1(loc.addressLine1());
        site.setAddressLine2(loc.addressLine2());
        site.setCity(loc.city());
        site.setStateRegion(loc.stateRegion());
        site.setPostalCode(loc.postalCode());

        // Geofence
        site.setGeofenceShapeType(loc.geofenceShapeType());
        site.setGeofenceRadiusMeters(loc.geofenceRadiusMeters());
        site.setGeofencePolygonGeoJson(loc.geofencePolygonGeoJson());
        site.setEntryBufferMeters(loc.entryBufferMeters());
        site.setExitBufferMeters(loc.exitBufferMeters());
        site.setMaxLocationAccuracyMeters(loc.maxLocationAccuracyMeters());

        return site;
    }

    /**
     * Constructs a {@link SiteTrustedNetwork} from a validated inbound rule request.
     * The CIDR is normalized to lowercase. IP version is re-derived from the CIDR
     * rather than trusted directly from the request payload.
     */
    private SiteTrustedNetwork buildNetworkEntity(CompanySite site, TrustedNetworkRequest req, int priorityOrder) {
        SiteTrustedNetwork rule = new SiteTrustedNetwork();
        rule.setSite(site);
        rule.setName(req.name().trim());
        rule.setNetworkType(req.networkType());
        rule.setCidrBlock(CidrValidator.normalize(req.cidrBlock()));
        rule.setIpVersion(CidrValidator.resolveIpVersion(req.cidrBlock()));
        rule.setIsActive(true);
        rule.setPriorityOrder(priorityOrder);
        rule.setNotes(req.notes());
        rule.setExpiresAt(req.expiresAt());
        return rule;
    }

    private void validateAttendancePolicy(CreateSiteRequest request, List<FieldValidationError> fieldErrors) {
        if (request.attendancePolicy() == null) {
            fieldErrors.add(new FieldValidationError(
                    "attendancePolicy",
                    "Attendance policy is required. Send the 'attendancePolicy' object with all attendance settings."
            ));
        }
    }

    private String inferGeofenceField(CreateSiteRequest request) {
        if (request.location() == null || request.location().geofenceShapeType() == null) {
            return "location.geofenceShapeType";
        }

        return switch (request.location().geofenceShapeType()) {
            case CIRCLE -> request.location().geofencePolygonGeoJson() != null
                    && !request.location().geofencePolygonGeoJson().isBlank()
                    ? "location.geofencePolygonGeoJson"
                    : "location.geofenceRadiusMeters";
            case POLYGON -> request.location().geofenceRadiusMeters() != null
                    ? "location.geofenceRadiusMeters"
                    : "location.geofencePolygonGeoJson";
        };
    }

    private AttendancePolicy buildAttendancePolicy(Company company, CompanySite site, CreateSiteRequest request) {
        var input = request.attendancePolicy();
        AttendancePolicy policy = new AttendancePolicy();
        policy.setCompany(company);
        policy.setSite(site);
        policy.setRequireQr(input.requireQr());
        policy.setRequireLocation(input.requireLocation());
        policy.setCheckInEnabled(input.checkInEnabled());
        policy.setCheckOutEnabled(input.checkOutEnabled());
        policy.setUseNetworkAsWarning(input.useNetworkAsWarning());
        policy.setRejectOutsideGeofence(input.rejectOutsideGeofence());
        policy.setRejectPoorAccuracy(input.rejectPoorAccuracy());
        policy.setAllowManualCorrection(input.allowManualCorrection());
        policy.setAllowManagerManualEntry(input.allowManagerManualEntry());
        return policy;
    }

    private SiteAttendancePolicySummaryResponse mapPolicySummary(AttendancePolicy policy) {
        return new SiteAttendancePolicySummaryResponse(
                policy.getId(),
                AttendancePolicySource.SITE_OVERRIDE,
                Boolean.TRUE.equals(policy.getRequireQr()),
                Boolean.TRUE.equals(policy.getRequireLocation()),
                Boolean.TRUE.equals(policy.getCheckInEnabled()),
                Boolean.TRUE.equals(policy.getCheckOutEnabled()),
                Boolean.TRUE.equals(policy.getUseNetworkAsWarning()),
                Boolean.TRUE.equals(policy.getRejectOutsideGeofence()),
                Boolean.TRUE.equals(policy.getRejectPoorAccuracy()),
                Boolean.TRUE.equals(policy.getAllowManualCorrection()),
                Boolean.TRUE.equals(policy.getAllowManagerManualEntry())
        );
    }

    private LinkedQrTerminalResponse mapTerminal(AttendanceQrTerminal terminal) {
        if (terminal == null) {
            return null;
        }
        return new LinkedQrTerminalResponse(
                terminal.getId(),
                terminal.getName(),
                terminal.getStatus(),
                terminal.getRotationSeconds(),
                Boolean.TRUE.equals(terminal.getAutoCreated()),
                terminal.getLastHeartbeatAt()
        );
    }

    /**
     * Extracts the authenticated actor from the Security context.
     * Throws {@link IllegalStateException} if no authenticated principal is present —
     * this should never happen if the endpoint is correctly secured.
     */
    private AuthSessionPrincipal resolveActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthSessionPrincipal principal)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "No valid authentication session found.");
        }
        return principal;
    }
}
