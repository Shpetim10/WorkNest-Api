package com.worknest.features.companySite.dto;

import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.enums.GeofenceShapeType;
import com.worknest.domain.enums.LocationDetectionSource;
import com.worknest.domain.enums.SiteStatus;
import com.worknest.domain.enums.SiteType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateSiteResponse(
        UUID id,
        UUID companyId,
        String code,
        String name,
        SiteType type,
        SiteStatus status,
        long version,
        BigDecimal latitude,
        BigDecimal longitude,
        String countryCode,
        String timezone,
        String addressLine1,
        String addressLine2,
        String city,
        String stateRegion,
        String postalCode,
        LocationDetectionSource locationDetectionSource,
        GeofenceShapeType geofenceShapeType,
        Integer geofenceRadiusMeters,
        String geofencePolygonGeoJson,
        Integer entryBufferMeters,
        Integer exitBufferMeters,
        Integer maxLocationAccuracyMeters,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        List<TrustedNetworkResponse> trustedNetworks,
        SiteAttendancePolicySummaryResponse attendancePolicy,
        LinkedQrTerminalResponse defaultQrTerminal
) {

    public static CreateSiteResponse fromEntity(
            CompanySite site,
            List<TrustedNetworkResponse> networks,
            SiteAttendancePolicySummaryResponse policy,
            LinkedQrTerminalResponse defaultQrTerminal
    ) {
        return new CreateSiteResponse(
                site.getId(),
                site.getCompany().getId(),
                site.getCode(),
                site.getName(),
                site.getType(),
                site.getStatus(),
                site.getVersion() != null ? site.getVersion() : 0L,
                site.getLatitude(),
                site.getLongitude(),
                site.getCountryCode(),
                site.getTimezone(),
                site.getAddressLine1(),
                site.getAddressLine2(),
                site.getCity(),
                site.getStateRegion(),
                site.getPostalCode(),
                site.getLocationDetectionSource(),
                site.getGeofenceShapeType(),
                site.getGeofenceRadiusMeters(),
                site.getGeofencePolygonGeoJson(),
                site.getEntryBufferMeters(),
                site.getExitBufferMeters(),
                site.getMaxLocationAccuracyMeters(),
                site.getNotes(),
                site.getCreatedAt(),
                site.getUpdatedAt(),
                networks,
                policy,
                defaultQrTerminal
        );
    }
}
