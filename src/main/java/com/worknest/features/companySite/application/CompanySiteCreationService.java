package com.worknest.features.companySite.application;

import com.worknest.features.companySite.dto.CreateSiteRequest;
import com.worknest.features.companySite.dto.CreateSiteResponse;
import java.util.UUID;

/**
 * One-shot company site creation service.
 *
 * <p>Accepts a fully validated {@link CreateSiteRequest}, performs strict
 * server-side validation, and persists both the site and any optional
 * trusted-network rules atomically in a single transaction.
 *
 * <p>No intermediate draft or step-by-step state is created.
 */
public interface CompanySiteCreationService {

    /**
     * Creates a new company site and its optional trusted-network rules in one transaction.
     *
     * @param companyId  the owning company; must match the caller's tenant context
     * @param request    the fully reviewed, post-validation request payload
     * @param actorIp    the resolved client IP of the actor (for audit logging)
     * @return a complete snapshot of the persisted site and its network rules
     */
    CreateSiteResponse createSite(UUID companyId, CreateSiteRequest request, String actorIp);
}
