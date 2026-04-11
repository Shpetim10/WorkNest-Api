package com.worknest.features.companySite.application;

import com.worknest.features.companySite.dto.CompanySiteResponse;
import java.util.List;
import java.util.UUID;

public interface CompanySiteQueryService {

    List<CompanySiteResponse> listSites(UUID companyId);

    /**
     * Fetches comprehensive details of a specific site including its networks.
     */
    CompanySiteDetailsResponse getSiteDetails(UUID companyId, UUID siteId);
}
