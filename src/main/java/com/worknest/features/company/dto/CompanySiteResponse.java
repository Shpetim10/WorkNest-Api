package com.worknest.features.company.dto;

import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.enums.GeofenceShapeType;
import com.worknest.domain.enums.SiteStatus;
import com.worknest.domain.enums.SiteType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Response containing details of a company site/location")
public record CompanySiteResponse(
        @Schema(description = "Unique ID of the site")
        UUID id,

        @Schema(description = "ID of the company this site belongs to")
        UUID companyId,

        @Schema(description = "Internal code for the site", example = "HQ-TIR")
        String code,

        @Schema(description = "Display name of the site", example = "Tirana Headquarters")
        String name,

        @Schema(description = "The functional type of the site")
        SiteType type,

        @Schema(description = "The current operational status of the site")
        SiteStatus status,

        @Schema(description = "Optimistic-lock token for the site")
        Long version,

        @Schema(description = "Street address line 1", example = "Bulevardi Deshmoret e Kombit")
        String addressLine1,

        @Schema(description = "Street address line 2")
        String addressLine2,

        @Schema(description = "City", example = "Tirane")
        String city,

        @Schema(description = "State or region")
        String stateRegion,

        @Schema(description = "Postal code", example = "1001")
        String postalCode,

        @Schema(description = "ISO 3166-1 alpha-2 country code", example = "AL")
        String countryCode,

        @Schema(description = "IANA timezone identifier", example = "Europe/Tirane")
        String timezone,

        @Schema(description = "Latitude coordinate for circle geofence center")
        BigDecimal latitude,

        @Schema(description = "Longitude coordinate for circle geofence center")
        BigDecimal longitude,

        @Schema(description = "The shape type used for geofencing")
        GeofenceShapeType geofenceShapeType,

        @Schema(description = "Radius in meters for circle geofence")
        Integer geofenceRadiusMeters,

        @Schema(description = "GeoJSON polygon for polygon geofence")
        String geofencePolygonGeoJson,

        @Schema(description = "Entry buffer in meters")
        Integer entryBufferMeters,

        @Schema(description = "Exit buffer in meters")
        Integer exitBufferMeters,

        @Schema(description = "Maximum accepted GPS accuracy in meters")
        Integer maxLocationAccuracyMeters,

        @Schema(description = "Whether GPS location is required for clocking in/out at this site")
        Boolean locationRequired,

        @Schema(description = "Whether QR code scanning is enabled for this site")
        Boolean qrEnabled,

        @Schema(description = "Whether check-in operations are permitted at this site")
        Boolean checkInEnabled,

        @Schema(description = "Whether check-out operations are permitted at this site")
        Boolean checkOutEnabled,

        @Schema(description = "Additional internal notes or descriptions")
        String notes,

        @Schema(description = "When the site was created")
        Instant createdAt,

        @Schema(description = "When the site was last updated")
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
                site.getVersion(),
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
                site.getLocationRequired(),
                site.getQrEnabled(),
                site.getCheckInEnabled(),
                site.getCheckOutEnabled(),
                site.getNotes(),
                site.getCreatedAt(),
                site.getUpdatedAt()
        );
    }
}
