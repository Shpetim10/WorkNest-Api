package com.worknest.features.companySite.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record DetectLocationResponse(
        boolean isWithinGeofence,
        Double distanceMeters,
        List<String> warnings,
        String recommendedTimezone,
        String recommendedCountryCode
) {}
