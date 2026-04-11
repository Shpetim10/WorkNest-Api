package com.worknest.features.companySite.application;

import com.worknest.features.companySite.dto.CreateNetworkRequest;
import com.worknest.features.companySite.dto.ToggleNetworkStatusRequest;
import com.worknest.features.companySite.dto.TrustedNetworkResponse;
import com.worknest.features.companySite.dto.UpdateNetworkRequest;
import java.util.List;
import java.util.UUID;

public interface SiteTrustedNetworkService {
    
    List<TrustedNetworkResponse> listNetworks(UUID companyId, UUID siteId);
    
    TrustedNetworkResponse updateNetwork(UUID companyId, UUID siteId, UUID networkId, UpdateNetworkRequest request);
}
