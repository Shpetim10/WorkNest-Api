package com.worknest.features.companySite.application;

import com.worknest.audit.service.SiteSetupAuditService;
import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.AttendancePolicy;
import com.worknest.domain.entities.AttendanceQrTerminal;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.entities.SiteTrustedNetwork;
import com.worknest.domain.enums.AttendancePolicySource;
import com.worknest.domain.enums.SiteStatus;
import com.worknest.features.attendance.application.AttendanceQrService;
import com.worknest.features.attendance.repository.AttendancePolicyRepository;
import com.worknest.features.companySite.dto.CreateSiteRequest;
import com.worknest.features.companySite.dto.CreateSiteResponse;
import com.worknest.features.companySite.dto.LinkedQrTerminalResponse;
import com.worknest.features.companySite.dto.SiteAttendancePolicySummaryResponse;
import com.worknest.features.companySite.dto.TrustedNetworkRequest;
import com.worknest.features.companySite.dto.TrustedNetworkResponse;
import com.worknest.features.companySite.exception.CompanyNotFoundException;
import com.worknest.features.companySite.exception.DuplicateTrustedNetworkException;
import com.worknest.features.companySite.exception.SiteCodeAlreadyExistsException;
import com.worknest.features.companySite.repository.CompanySiteRepository;
import com.worknest.features.companySite.repository.SiteTrustedNetworkRepository;
import com.worknest.features.companySite.validation.CidrValidator;
import com.worknest.features.companySite.validation.GeofenceValidator;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.security.AuthSessionPrincipal;
import com.worknest.tenant.TenantContextHolder;
import jakarta.persistence.EntityManager;
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

    private final CompanySiteRepository       siteRepository;
    private final SiteTrustedNetworkRepository networkRepository;
    private final CompanyRepository            companyRepository;
    private final SiteSetupAuditService        auditService;
    private final AttendancePolicyRepository   attendancePolicyRepository;
    private final AttendanceQrService          attendanceQrService;
    private final EntityManager                entityManager;

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

        // ── 5. Geofence and Address validation ───────────────────────────────────
        GeofenceValidator.validate(request.location());
        
        if (request.location().countryCode() != null && 
            !request.location().countryCode().equalsIgnoreCase(request.countryCode())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "COUNTRY_MISMATCH", "The selected location's country (" + request.location().countryCode() + ") must match the site's primary country code (" + request.countryCode() + ").");
        }

        // ── 6. Trusted-network validation ────────────────────────────────────────
        List<TrustedNetworkRequest> networks = request.trustedNetworks() != null
                ? request.trustedNetworks()
                : List.of();

        validateTrustedNetworks(networks);
        validateAttendancePolicy(request);

        // ── 7. Persist site ──────────────────────────────────────────────────────
        Company companyRef = entityManager.getReference(Company.class, companyId);
        CompanySite site   = buildSiteEntity(companyRef, request, normalizedCode);
        site = siteRepository.save(site);

        // ── 8. Persist trusted-network rules ─────────────────────────────────────
        List<SiteTrustedNetwork> savedNetworks = new ArrayList<>();
        for (TrustedNetworkRequest netReq : networks) {
            SiteTrustedNetwork rule = buildNetworkEntity(site, netReq);
            savedNetworks.add(networkRepository.save(rule));
        }

        AttendancePolicy attendancePolicy = buildAttendancePolicy(companyRef, site, request);
        attendancePolicy = attendancePolicyRepository.save(attendancePolicy);
        AttendanceQrTerminal defaultTerminal = Boolean.TRUE.equals(attendancePolicy.getRequireQr())
                ? attendanceQrService.ensureDefaultTerminal(companyRef, site)
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
    private void validateTrustedNetworks(List<TrustedNetworkRequest> networks) {
        Set<String> seenKeys = new HashSet<>();
        for (TrustedNetworkRequest net : networks) {
            // CIDR validation (throws InvalidCidrException on failure)
            CidrValidator.validate(net.cidrBlock(), net.ipVersion());

            // Intra-request duplicate check
            String dedupKey = CidrValidator.normalize(net.cidrBlock()) + "|" + net.networkType();
            if (!seenKeys.add(dedupKey)) {
                throw new DuplicateTrustedNetworkException(net.cidrBlock());
            }
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
        site.setName(req.name() != null ? req.name().trim() : "Unnamed Site");
        site.setType(req.type());
        site.setStatus(SiteStatus.PENDING_REVIEW);
        site.setCountryCode(req.countryCode() != null ? req.countryCode().trim().toUpperCase() : "??");
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
    private SiteTrustedNetwork buildNetworkEntity(CompanySite site, TrustedNetworkRequest req) {
        SiteTrustedNetwork rule = new SiteTrustedNetwork();
        rule.setSite(site);
        rule.setName(req.name().trim());
        rule.setNetworkType(req.networkType());
        rule.setCidrBlock(CidrValidator.normalize(req.cidrBlock()));
        // Re-derive IP version server-side from the CIDR — never trust the frontend value directly.
        rule.setIpVersion(CidrValidator.resolveIpVersion(req.cidrBlock()));
        rule.setIsActive(true);
        rule.setPriorityOrder(req.priorityOrder());
        rule.setNotes(req.notes());
        rule.setExpiresAt(req.expiresAt());
        return rule;
    }

    private void validateAttendancePolicy(CreateSiteRequest request) {
        var policy = request.attendancePolicy();
        if (policy == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ATTENDANCE_POLICY_REQUIRED", "Attendance policy is required.");
        }
        if (Boolean.TRUE.equals(policy.missingCheckoutAutoCloseEnabled()) && policy.autoCheckoutAfterMinutes() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "AUTO_CHECKOUT_MINUTES_REQUIRED",
                    "autoCheckoutAfterMinutes is required when missing checkout auto-close is enabled."
            );
        }
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
        policy.setMissingCheckoutAutoCloseEnabled(input.missingCheckoutAutoCloseEnabled());
        policy.setAutoCheckoutAfterMinutes(input.autoCheckoutAfterMinutes());
        policy.setLateGraceMinutes(input.lateGraceMinutes());
        policy.setEarlyClockInWindowMinutes(input.earlyClockInWindowMinutes());
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
                Boolean.TRUE.equals(policy.getAllowManagerManualEntry()),
                Boolean.TRUE.equals(policy.getMissingCheckoutAutoCloseEnabled()),
                policy.getAutoCheckoutAfterMinutes(),
                policy.getLateGraceMinutes() != null ? policy.getLateGraceMinutes() : 0,
                policy.getEarlyClockInWindowMinutes() != null ? policy.getEarlyClockInWindowMinutes() : 0
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
