package com.worknest.features.companySite.application;

import com.worknest.domain.entities.AttendancePolicy;
import com.worknest.domain.entities.AttendanceQrTerminal;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.CompanySite;
import java.util.UUID;

/**
 * Port that decouples site creation from attendance internals.
 * Implemented by the attendance feature; injected into companySite via Spring DI.
 */
public interface SiteAttendanceProvisioningPort {

    AttendancePolicy savePolicy(AttendancePolicy policy);

    /**
     * Returns the existing default terminal for the site, or creates one.
     * Returns {@code null} when QR is not required.
     */
    AttendanceQrTerminal ensureDefaultTerminal(Company company, CompanySite site);

    /**
     * Keeps {@code requireLocation} on the site's {@link AttendancePolicy} in sync
     * when the location modal changes the geofence-enabled toggle on the site.
     * No-op if no site-level policy exists yet.
     */
    void syncRequireLocation(UUID companyId, UUID siteId, boolean requireLocation);
}
