package com.worknest.audit.service.impl;

import com.worknest.audit.domain.AuditLog;
import com.worknest.audit.domain.PlatformEvent;
import com.worknest.audit.service.AuditLogService;
import com.worknest.audit.service.PlatformEventService;
import com.worknest.audit.service.SiteSetupAuditService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Implementation of {@link SiteSetupAuditService}.
 *
 * <p>Pattern is identical to {@link AuthAuditServiceImpl}:
 * <ol>
 *   <li>Build an immutable {@link AuditLog} and persist it via
 *       {@link AuditLogService} (runs in {@code REQUIRES_NEW} transaction
 *       so audit never rolls back the calling business tx).</li>
 *   <li>Publish a lightweight {@link PlatformEvent} for dashboard / notification
 *       consumers via {@link PlatformEventService}.</li>
 * </ol>
 *
 * <p>No {@code companyName} is stored here — the company's display name is
 * derivable from {@code companyId} if needed for reports.  Avoiding it keeps
 * this service free of a {@code CompanyRepository} dependency, consistent with
 * how {@link AuthAuditServiceImpl} occasionally uses null for company name.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiteSetupAuditServiceImpl implements SiteSetupAuditService {

    private final AuditLogService auditLogService;
    private final PlatformEventService platformEventService;

    // ─────────────────────────────────────────────────────────
    // Interface implementations
    // ─────────────────────────────────────────────────────────

    @Override
    public void appendSiteDraftCreated(
            UUID companyId,
            UUID siteId,
            String siteCode,
            UUID actorUserId,
            UUID actorRoleAssignmentId,
            String ipAddress
    ) {
        auditLogService.logAction(buildAuditLog(
                companyId, actorUserId, actorRoleAssignmentId,
                "SITE_DRAFT_CREATED", "CompanySite", siteId,
                mapOf("siteCode", siteCode),
                Map.of(),
                ipAddress
        ));

        platformEventService.publishEvent(new PlatformEvent(
                "SITE_DRAFT_CREATED",
                companyId,
                null,
                actorUserId,
                "Site draft created with code '" + siteCode + "'"
        ));

        log.debug("Audit: SITE_DRAFT_CREATED site={} company={} actor={}", siteId, companyId, actorUserId);
    }

    @Override
    public void appendSiteBasicInfoSaved(
            UUID companyId,
            UUID siteId,
            String siteCode,
            UUID actorUserId,
            UUID actorRoleAssignmentId,
            String ipAddress
    ) {
        auditLogService.logAction(buildAuditLog(
                companyId, actorUserId, actorRoleAssignmentId,
                "SITE_BASIC_INFO_SAVED", "CompanySite", siteId,
                mapOf("siteCode", siteCode),
                Map.of(),
                ipAddress
        ));

        platformEventService.publishEvent(new PlatformEvent(
                "SITE_BASIC_INFO_SAVED",
                companyId,
                null,
                actorUserId,
                "Site basic info updated for site '" + siteCode + "'"
        ));

        log.debug("Audit: SITE_BASIC_INFO_SAVED site={} company={} actor={}", siteId, companyId, actorUserId);
    }

    @Override
    public void appendSiteLocationSaved(
            UUID companyId,
            UUID siteId,
            boolean locationRequired,
            UUID actorUserId,
            UUID actorRoleAssignmentId,
            String ipAddress
    ) {
        auditLogService.logAction(buildAuditLog(
                companyId, actorUserId, actorRoleAssignmentId,
                "SITE_LOCATION_SAVED", "CompanySite", siteId,
                mapOf("locationRequired", locationRequired),
                Map.of(),
                ipAddress
        ));

        platformEventService.publishEvent(new PlatformEvent(
                "SITE_LOCATION_SAVED",
                companyId,
                null,
                actorUserId,
                "Site location saved, locationRequired=" + locationRequired
        ));

        log.debug("Audit: SITE_LOCATION_SAVED site={} company={} actor={}", siteId, companyId, actorUserId);
    }

    @Override
    public void appendTrustedNetworkUpserted(
            UUID companyId,
            UUID siteId,
            UUID networkRuleId,
            String cidrBlock,
            boolean wasCreated,
            UUID actorUserId,
            UUID actorRoleAssignmentId,
            String ipAddress
    ) {
        String action = wasCreated ? "SITE_NETWORK_RULE_CREATED" : "SITE_NETWORK_RULE_UPDATED";

        auditLogService.logAction(buildAuditLog(
                companyId, actorUserId, actorRoleAssignmentId,
                action, "SiteTrustedNetwork", networkRuleId,
                mapOf("siteId", siteId, "cidrBlock", cidrBlock),
                Map.of(),
                ipAddress
        ));

        platformEventService.publishEvent(new PlatformEvent(
                action,
                companyId,
                null,
                actorUserId,
                (wasCreated ? "Trusted network rule created" : "Trusted network rule updated")
                        + " for site " + siteId
        ));

        log.debug("Audit: {} networkRule={} site={} company={} actor={}", action, networkRuleId, siteId, companyId, actorUserId);
    }

    @Override
    public void appendSiteActivationAttempted(
            UUID companyId,
            UUID siteId,
            boolean dryRun,
            boolean succeeded,
            UUID actorUserId,
            UUID actorRoleAssignmentId,
            String ipAddress
    ) {
        String action = dryRun ? "SITE_ACTIVATION_DRY_RUN" : (succeeded ? "SITE_ACTIVATED" : "SITE_ACTIVATION_FAILED");

        auditLogService.logAction(buildAuditLog(
                companyId, actorUserId, actorRoleAssignmentId,
                action, "CompanySite", siteId,
                mapOf("dryRun", dryRun, "succeeded", succeeded),
                Map.of(),
                ipAddress
        ));

        platformEventService.publishEvent(new PlatformEvent(
                action,
                companyId,
                null,
                actorUserId,
                dryRun
                        ? "Site activation dry-run completed for site " + siteId
                        : (succeeded ? "Site " + siteId + " activated" : "Site activation failed for site " + siteId)
        ));

        log.debug("Audit: {} site={} company={} actor={} dryRun={} succeeded={}", action, siteId, companyId, actorUserId, dryRun, succeeded);
    }

    // ─────────────────────────────────────────────────────────
    // Internal helpers — mirrors AuthAuditServiceImpl exactly
    // ─────────────────────────────────────────────────────────

    private AuditLog buildAuditLog(
            UUID companyId,
            UUID actorUserId,
            UUID actorRoleAssignmentId,
            String action,
            String entityType,
            UUID entityId,
            Map<String, Object> diff,
            Map<String, Object> metadata,
            String ipAddress
    ) {
        return new AuditLog(
                companyId,
                actorUserId,
                actorRoleAssignmentId,
                null,  // actorRole — not required for site setup audit events
                null,  // actorJobTitle — not required
                action,
                entityType,
                entityId,
                diff,
                metadata,
                ipAddress
        );
    }

    private Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            Object key = keyValues[i];
            Object value = keyValues[i + 1];
            if (key instanceof String stringKey && isMeaningful(value)) {
                values.put(stringKey, value);
            }
        }
        return values;
    }

    private boolean isMeaningful(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String stringValue) {
            return StringUtils.hasText(stringValue);
        }
        return true;
    }
}
