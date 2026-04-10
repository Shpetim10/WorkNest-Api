package com.worknest.features.company.application;

import com.worknest.features.company.dto.CompanySiteResponse;
import com.worknest.features.company.dto.CreateSiteDraftRequest;
import com.worknest.features.company.dto.SiteActivationResponse;
import com.worknest.features.company.dto.SiteBasicInfoRequest;
import com.worknest.features.company.dto.SiteLocationRequest;
import com.worknest.features.company.dto.SiteSetupStatusResponse;
import com.worknest.features.company.dto.TrustedNetworkResponse;
import com.worknest.features.company.dto.TrustedNetworkUpsertRequest;
import java.util.List;
import java.util.UUID;

public interface CompanySiteSetupService {

    CompanySiteResponse createDraft(UUID companyId, CreateSiteDraftRequest request);

    CompanySiteResponse saveBasicInfo(UUID siteId, SiteBasicInfoRequest request);

    CompanySiteResponse saveLocation(UUID siteId, SiteLocationRequest request);

    TrustedNetworkResponse upsertTrustedNetwork(UUID siteId, UUID trustedNetworkId, TrustedNetworkUpsertRequest request);

    SiteSetupStatusResponse getSetupStatus(UUID siteId);

    SiteActivationResponse activate(UUID siteId, boolean dryRun);

    List<CompanySiteResponse> getSitesByCompany(UUID companyId);
}
