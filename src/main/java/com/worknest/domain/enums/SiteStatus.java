package com.worknest.domain.enums;

/**
 * Operational lifecycle status for a CompanySite.
 *
 * <p>State machine for the one-shot create flow:
 * <pre>
 *   [create] ──► PENDING_REVIEW  ──activate──► ACTIVE ──disable──► DISABLED ──re-enable──► ACTIVE
 *                                              ACTIVE / DISABLED ──archive──► ARCHIVED  (terminal)
 * </pre>
 *
 * <p>{@code PENDING_REVIEW} replaces {@code DRAFT} as the post-create status.
 * A site in {@code PENDING_REVIEW} has passed all server-side create-time validation
 * but has not yet been explicitly activated. Attendance and network enforcement are
 * NOT active until the site transitions to {@code ACTIVE}.
 *
 * <p>{@code DRAFT} is retained as a deprecated alias; no new code should reference it.
 * Use {@code PENDING_REVIEW} for all new site creation.
 */
public enum SiteStatus {

    /**
     * Site has been created and validated but not yet activated.
     * Attendance and network enforcement are NOT active.
     * This is the initial status assigned by the one-shot create endpoint.
     */
    PENDING_REVIEW,

    /**
     * Site is fully configured and operationally live.
     * Attendance check-in, geofence enforcement, and trusted-network checks are active.
     */
    ACTIVE,

    /**
     * Site was active but has been administratively turned off.
     * Can be re-enabled back to ACTIVE.
     */
    DISABLED,

    /**
     * Terminal state. Site is read-only and no longer used.
     * No transitions are permitted out of this state.
     */
    ARCHIVED,

    /**
     * @deprecated Use {@link #PENDING_REVIEW} for new site creation.
     * Kept for backward compatibility with existing data that may already use this value.
     */
    @Deprecated
    DRAFT,

    /**
     * @deprecated Use {@link #DISABLED} for new code.
     * Kept for backward compatibility with any data that may already use this value.
     */
    @Deprecated
    INACTIVE
}