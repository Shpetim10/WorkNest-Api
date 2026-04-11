package com.worknest.features.companySite.application;

import com.worknest.features.companySite.dto.CompanySiteResponse;
import java.util.List;
import java.util.UUID;

public interface CompanySiteQueryService {

    List<CompanySiteResponse> listSites(UUID companyId);
}
