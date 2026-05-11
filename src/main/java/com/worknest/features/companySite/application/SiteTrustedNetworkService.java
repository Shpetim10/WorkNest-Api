package com.worknest.features.companySite.application;

import com.worknest.features.companySite.dto.CreateNetworkRequest;
import com.worknest.features.companySite.dto.ToggleNetworkStatusRequest;
import com.worknest.features.companySite.dto.TrustedNetworkResponse;
import com.worknest.features.companySite.dto.UpdateNetworkRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SiteTrustedNetworkService {
    
    Page<TrustedNetworkResponse> listNetworks(UUID companyId, UUID siteId, Pageable pageable);
    
    TrustedNetworkResponse updateNetwork(UUID companyId, UUID siteId, UUID networkId, UpdateNetworkRequest request);
}
