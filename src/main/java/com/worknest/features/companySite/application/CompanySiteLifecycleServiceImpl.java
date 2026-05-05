package com.worknest.features.companySite.application;

import com.worknest.audit.service.SiteSetupAuditService;
import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.enums.GeofenceShapeType;
import com.worknest.domain.enums.SiteStatus;
import com.worknest.features.companySite.dto.CompanySiteResponse;
import com.worknest.features.companySite.exception.SiteNotFoundException;
import com.worknest.features.companySite.repository.CompanySiteRepository;
import com.worknest.security.AuthSessionPrincipal;
import com.worknest.tenant.TenantContextHolder;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link CompanySiteLifecycleService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanySiteLifecycleServiceImpl implements CompanySiteLifecycleService {

    private final CompanySiteRepository siteRepository;
    private final SiteSetupAuditService  auditService;

    @Override
    @Transactional
    public CompanySiteResponse activateSite(UUID companyId, UUID siteId, String actorIp) {
        AuthSessionPrincipal actor = resolveActor();
        validateTenant(companyId);

        CompanySite site = siteRepository.findByIdAndCompanyId(siteId, companyId)
                .orElseThrow(SiteNotFoundException::new);

        if (site.getStatus() == SiteStatus.ACTIVE) {
            return CompanySiteResponse.fromEntity(site);
        }

        if (site.getStatus() != SiteStatus.PENDING_REVIEW && site.getStatus() != SiteStatus.DISABLED) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_STATUS_TRANSITION",
                    "Site must be in PENDING_REVIEW or DISABLED status to be activated. Current status: " + site.getStatus()
            );
        }

        validateGeofenceCompleteness(site);

        site.setStatus(SiteStatus.ACTIVE);
        site = siteRepository.save(site);

        auditService.appendSiteActivationAttempted(
                companyId,
                siteId,
                false,
                true,
                actor.userId(),
                actor.roleAssignmentId(),
                actorIp
        );

        log.info("CompanySiteLifecycle: site '{}' (id={}) activated by actor={}", 
                site.getCode(), siteId, actor.userId());

        return CompanySiteResponse.fromEntity(site);
    }

    @Override
    @Transactional
    public CompanySiteResponse disableSite(UUID companyId, UUID siteId, String actorIp) {
        AuthSessionPrincipal actor = resolveActor();
        validateTenant(companyId);

        CompanySite site = siteRepository.findByIdAndCompanyId(siteId, companyId)
                .orElseThrow(SiteNotFoundException::new);

        if (site.getStatus() == SiteStatus.DISABLED) {
            return CompanySiteResponse.fromEntity(site);
        }

        if (site.getStatus() != SiteStatus.ACTIVE) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_STATUS_TRANSITION",
                    "Only ACTIVE sites can be disabled. Current status: " + site.getStatus()
            );
        }

        site.setStatus(SiteStatus.DISABLED);
        site = siteRepository.save(site);

        auditService.appendSiteDisabled(
                companyId,
                siteId,
                actor.userId(),
                actor.roleAssignmentId(),
                actorIp
        );

        log.info("CompanySiteLifecycle: site '{}' (id={}) disabled by actor={}", 
                site.getCode(), siteId, actor.userId());

        return CompanySiteResponse.fromEntity(site);
    }

    private void validateGeofenceCompleteness(CompanySite site) {
        if (!Boolean.TRUE.equals(site.getLocationRequired())) {
            return;
        }
        if (site.getGeofenceShapeType() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INCOMPLETE_GEOFENCE",
                    "Site requires location verification but has no geofence configured. Set a geofence before activating.");
        }
        if (site.getGeofenceShapeType() == GeofenceShapeType.CIRCLE) {
            if (site.getLatitude() == null || site.getLongitude() == null || site.getGeofenceRadiusMeters() == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "INCOMPLETE_GEOFENCE",
                        "CIRCLE geofence requires latitude, longitude, and radius before activation.");
            }
        } else if (site.getGeofenceShapeType() == GeofenceShapeType.POLYGON) {
            if (site.getGeofencePolygonGeoJson() == null || site.getGeofencePolygonGeoJson().isBlank()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "INCOMPLETE_GEOFENCE",
                        "POLYGON geofence requires GeoJSON data before activation.");
            }
        }
    }

    private void validateTenant(UUID companyId) {
        UUID tenantCompanyId = TenantContextHolder.get()
                .orElseThrow(() -> new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "TENANT_CONTEXT_MISSING", "Tenant context missing"))
                .companyId();

        if (!tenantCompanyId.equals(companyId)) {
            throw new SiteNotFoundException(); // 404 to avoid leaking cross-tenant info
        }
    }

    private AuthSessionPrincipal resolveActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthSessionPrincipal principal)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "No valid session found.");
        }
        return principal;
    }
}
