package com.worknest.features.companySite.application;

import com.worknest.features.companySite.dto.CompanySiteDetailsResponse;
import com.worknest.features.companySite.dto.CompanySiteLookup;
import com.worknest.features.companySite.dto.CompanySiteResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CompanySiteQueryService {

    Page<CompanySiteResponse> listSites(UUID companyId, Pageable pageable);

    /**
     * Fetches comprehensive details of a specific site including its networks.
     */
    CompanySiteDetailsResponse getSiteDetails(UUID companyId, UUID siteId);

    /**
     * Returns a lightweight id/code/name list for all ACTIVE sites of the given company.
     * Intended for use in dropdowns and selection lists.
     */
    List<CompanySiteLookup> lookupSites(UUID companyId);
}
