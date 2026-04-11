package com.worknest.features.companySite.application;

import com.worknest.features.companySite.dto.CompanySiteResponse;
import java.util.UUID;

/**
 * Service to manage the operational status of a company site.
 */
public interface CompanySiteLifecycleService {

    /**
     * Activates a site from PENDING_REVIEW or DISABLED state to ACTIVE.
     *
     * @param companyId expected owning company
     * @param siteId    site to activate
     * @param actorIp   IP of the administrator
     * @return updated site snapshot
     */
    CompanySiteResponse activateSite(UUID companyId, UUID siteId, String actorIp);

    /**
     * Administratively disables an ACTIVE site.
     *
     * @param companyId expected owning company
     * @param siteId    site to disable
     * @param actorIp   IP of the administrator
     * @return updated site snapshot
     */
    CompanySiteResponse disableSite(UUID companyId, UUID siteId, String actorIp);
}
