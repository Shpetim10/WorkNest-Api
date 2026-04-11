package com.worknest.features.companySite.application;

import com.worknest.features.companySite.dto.MainDetailsReadDto;
import com.worknest.features.companySite.dto.MainDetailsUpdateRequest;
import java.util.UUID;

public interface CompanySiteUpdateService {
    
    MainDetailsReadDto getMainDetails(UUID companyId, UUID siteId);
    
    MainDetailsReadDto updateMainDetails(UUID companyId, UUID siteId, MainDetailsUpdateRequest request);
}
