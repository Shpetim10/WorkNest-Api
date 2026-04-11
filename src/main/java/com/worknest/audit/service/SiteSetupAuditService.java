package com.worknest.audit.service;

import java.util.UUID;

/**
 * Audit facade for Company Site setup and lifecycle actions.
 *
 * <p>Mirrors the {@link AuthAuditService} pattern: the facade assembles
 * {@link com.worknest.audit.domain.AuditLog} and
 * {@link com.worknest.audit.domain.PlatformEvent} entries from typed arguments,
 * then delegates to {@link AuditLogService} and {@link PlatformEventService}.
 *
 * <p>All methods run in a nested {@code REQUIRES_NEW} transaction via the
 * underlying {@link AuditLogService} implementation, so an audit failure
 * never rolls back the calling business transaction.
 */
public interface SiteSetupAuditService {

    /**
     * Emitted when a new site draft is created.
     *
     * @param companyId  owning company
     * @param siteId     newly created site
     * @param siteCode   site code (non-sensitive; useful for search)
     * @param actorUserId actor who triggered the operation
     * @param actorRoleAssignmentId actor's active role assignment
     * @param ipAddress  client IP (may be null)
     */
    void appendSiteDraftCreated(
            UUID companyId,
            UUID siteId,
            String siteCode,
            UUID actorUserId,
            UUID actorRoleAssignmentId,
            String ipAddress
    );

    /**
     * Emitted when basic info is saved on a draft or active site.
     *
     * @param companyId  owning company
     * @param siteId     site being updated
     * @param siteCode   new code value (after normalisation)
     * @param actorUserId actor who triggered the operation
     * @param actorRoleAssignmentId actor's active role assignment
     * @param ipAddress  client IP (may be null)
     */
    void appendSiteBasicInfoSaved(
            UUID companyId,
            UUID siteId,
            String siteCode,
            UUID actorUserId,
            UUID actorRoleAssignmentId,
            String ipAddress
    );

    /**
     * Emitted when location / geofence data is saved on a draft or active site.
     *
     * @param companyId  owning company
     * @param siteId     site being updated
     * @param locationRequired whether GPS enforcement is enabled
     * @param actorUserId actor who triggered the operation
     * @param actorRoleAssignmentId actor's active role assignment
     * @param ipAddress  client IP (may be null)
     */
    void appendSiteLocationSaved(
            UUID companyId,
            UUID siteId,
            boolean locationRequired,
            UUID actorUserId,
            UUID actorRoleAssignmentId,
            String ipAddress
    );

    /**
     * Emitted when a trusted-network rule is created or updated.
     *
     * @param companyId     owning company
     * @param siteId        site the rule belongs to
     * @param networkRuleId the rule id
     * @param cidrBlock     CIDR saved (for audit trail — not masked at this layer)
     * @param wasCreated    true = new rule, false = update to existing rule
     * @param actorUserId   actor who triggered the operation
     * @param actorRoleAssignmentId actor's active role assignment
     * @param ipAddress     client IP (may be null)
     */
    void appendTrustedNetworkUpserted(
            UUID companyId,
            UUID siteId,
            UUID networkRuleId,
            String cidrBlock,
            boolean wasCreated,
            UUID actorUserId,
            UUID actorRoleAssignmentId,
            String ipAddress
    );

    /**
     * Emitted when site activation is attempted (both dry-run and real).
     *
     * @param companyId    owning company
     * @param siteId       site being activated
     * @param dryRun       whether this was a validation-only call
     * @param succeeded    whether the site transitioned to ACTIVE (always false for dry-run)
     * @param actorUserId  actor who triggered the operation
     * @param actorRoleAssignmentId actor's active role assignment
     * @param ipAddress    client IP (may be null)
     */
    void appendSiteActivationAttempted(
            UUID companyId,
            UUID siteId,
            boolean dryRun,
            boolean succeeded,
            UUID actorUserId,
            UUID actorRoleAssignmentId,
            String ipAddress
    );

    /**
     * Emitted when a site is administratively disabled.
     *
     * @param companyId    owning company
     * @param siteId       site being disabled
     * @param actorUserId  actor who triggered the operation
     * @param actorRoleAssignmentId actor's active role assignment
     * @param ipAddress    client IP (may be null)
     */
    void appendSiteDisabled(
            UUID companyId,
            UUID siteId,
            UUID actorUserId,
            UUID actorRoleAssignmentId,
            String ipAddress
    );
}
