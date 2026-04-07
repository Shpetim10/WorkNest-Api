package com.worknest.domain.enums;

/**
 * Operational lifecycle status for a CompanySite.
 *
 * <p>State machine:
 * <pre>
 *   DRAFT ──activate──> ACTIVE ──disable──> DISABLED ──re-enable──> ACTIVE
 *   ACTIVE / DISABLED ──archive──> ARCHIVED  (terminal, no transitions out)
 * </pre>
 *
 * <p>Setup wizard completeness is NOT encoded here. It is computed at
 * query-time by the service layer from field presence alone.
 *
 * <p>{@code INACTIVE} is preserved as a legacy alias; no new code should
 * reference it — use {@code DISABLED} instead.
 */
public enum SiteStatus {

    /**
     * Site is being configured via the setup wizard.
     * Attendance and network enforcement are NOT active.
     */
    DRAFT,

    /**
     * Site is fully configured and operationally live.
     */
    ACTIVE,

    /**
     * Site was active but has been administratively turned off.
     * Can be re-enabled back to ACTIVE.
     */
    DISABLED,

    /**
     * Terminal state. Site is read-only and no longer used.
     */
    ARCHIVED,

    /**
     * @deprecated Use {@link #DISABLED} for new code.
     * Kept for backward compatibility with any data that may already use this value.
     */
    @Deprecated
    INACTIVE;
}