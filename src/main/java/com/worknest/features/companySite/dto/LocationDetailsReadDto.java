package com.worknest.features.companySite.dto;

import com.worknest.domain.enums.GeofenceShapeType;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;

@Builder
public record LocationDetailsReadDto(
        UUID id,
        String addressLine1,
        String addressLine2,
        String city,
        String stateRegion,
        String postalCode,
        String countryCode,
        String timezone,
        BigDecimal latitude,
        BigDecimal longitude,
        GeofenceShapeType geofenceShapeType,
        Integer geofenceRadiusMeters,
        String geofencePolygonGeoJson,
        Integer entryBufferMeters,
        Integer exitBufferMeters,
        Integer maxLocationAccuracyMeters,
        Boolean locationRequired,
        Long version
) {}
