package com.worknest.features.companySite.dto;

import com.worknest.domain.enums.GeofenceShapeType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record LocationDetailsUpdateRequest(
        @Size(max=255) String addressLine1,
        @Size(max=255) String addressLine2,
        @Size(max=100) String city,
        @Size(max=100) String stateRegion,
        @Size(max=30) String postalCode,
        
        @NotBlank(message = "Country code is required.")
        @Size(min=2, max=2, message = "Country code must be 2 characters.")
        String countryCode,
        
        @NotBlank(message = "Timezone is required.")
        @Size(max=100) String timezone,

        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
        @DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
        BigDecimal latitude,

        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
        @DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
        BigDecimal longitude,

        GeofenceShapeType geofenceShapeType,
        
        @Min(value = 10, message = "Geofence radius must be at least 10 meters")
        @Max(value = 50000, message = "Geofence radius must be at most 50000 meters")
        Integer geofenceRadiusMeters,
        
        String geofencePolygonGeoJson,

        @Min(0) @Max(1000) Integer entryBufferMeters,
        @Min(0) @Max(1000) Integer exitBufferMeters,
        @Min(1) @Max(5000) Integer maxLocationAccuracyMeters,

        @NotNull(message = "Location Required flag must be set.")
        Boolean locationRequired,
        
        @NotNull(message = "Version is required for optimistic locking.")
        Long version
) {}
