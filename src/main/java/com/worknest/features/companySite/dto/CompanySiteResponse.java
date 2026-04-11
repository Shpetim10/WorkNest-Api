package com.worknest.features.companySite.dto;

import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.enums.GeofenceShapeType;
import com.worknest.domain.enums.LocationDetectionSource;
import com.worknest.domain.enums.SiteStatus;
import com.worknest.domain.enums.SiteType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Full representation of a company site.")
public record CompanySiteResponse(
        UUID id,
        UUID companyId,
        String code,
        String name,
        SiteType type,
        SiteStatus status,
        long version,
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
        boolean locationRequired,
        boolean qrEnabled,
        boolean checkInEnabled,
        boolean checkOutEnabled,
        String notes,
        LocationDetectionSource locationDetectionSource,
        Instant createdAt,
        Instant updatedAt
) {

    public static CompanySiteResponse fromEntity(CompanySite site) {
        return new CompanySiteResponse(
                site.getId(),
                site.getCompany().getId(),
                site.getCode(),
                site.getName(),
                site.getType(),
                site.getStatus(),
                site.getVersion() != null ? site.getVersion() : 0L,
                site.getAddressLine1(),
                site.getAddressLine2(),
                site.getCity(),
                site.getStateRegion(),
                site.getPostalCode(),
                site.getCountryCode(),
                site.getTimezone(),
                site.getLatitude(),
                site.getLongitude(),
                site.getGeofenceShapeType(),
                site.getGeofenceRadiusMeters(),
                site.getGeofencePolygonGeoJson(),
                site.getEntryBufferMeters(),
                site.getExitBufferMeters(),
                site.getMaxLocationAccuracyMeters(),
                Boolean.TRUE.equals(site.getLocationRequired()),
                Boolean.TRUE.equals(site.getQrEnabled()),
                Boolean.TRUE.equals(site.getCheckInEnabled()),
                Boolean.TRUE.equals(site.getCheckOutEnabled()),
                site.getNotes(),
                site.getLocationDetectionSource(),
                site.getCreatedAt(),
                site.getUpdatedAt()
        );
    }
}
