package com.worknest.features.companySite.application;

import com.worknest.features.companySite.dto.DetectLocationRequest;
import com.worknest.features.companySite.dto.DetectLocationResponse;
import com.worknest.features.companySite.dto.LocationDetailsReadDto;
import com.worknest.features.companySite.dto.LocationDetailsUpdateRequest;
import java.util.UUID;

public interface CompanySiteLocationService {
    LocationDetailsReadDto getLocation(UUID companyId, UUID siteId);

    LocationDetailsReadDto updateLocation(UUID companyId, UUID siteId, LocationDetailsUpdateRequest request);

    DetectLocationResponse assessLocation(UUID companyId, UUID siteId, DetectLocationRequest request);
}
