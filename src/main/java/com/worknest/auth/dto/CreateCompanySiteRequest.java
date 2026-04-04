package com.worknest.auth.dto;

import com.worknest.auth.domain.GeofenceShapeType;
import com.worknest.auth.domain.SiteStatus;
import com.worknest.auth.domain.SiteType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Request to create a new company site/location")
public record CreateCompanySiteRequest(
        @NotBlank
        @Size(max = 50)
        @Schema(description = "Internal unique code for the site", example = "HQ-TIR")
        String code,

        @NotBlank
        @Size(max = 255)
        @Schema(description = "Display name of the site", example = "Tirana Headquarters")
        String name,

        @NotNull
        @Schema(description = "Functional type of the site")
        SiteType type,

        @Schema(description = "Initial status of the site")
        SiteStatus status,

        @Size(max = 255)
        @Schema(description = "Street address line 1", example = "Bulevardi Deshmoret e Kombit")
        String addressLine1,

        @Size(max = 255)
        @Schema(description = "Street address line 2")
        String addressLine2,

        @Size(max = 100)
        @Schema(description = "City name", example = "Tirane")
        String city,

        @Size(max = 100)
        @Schema(description = "State or province/region")
        String stateRegion,

        @Size(max = 30)
        @Schema(description = "Postal or zip code", example = "1001")
        String postalCode,

        @Size(max = 2)
        @Schema(description = "ISO 3166-1 alpha-2 country code", example = "AL")
        String countryCode,

        @NotBlank
        @Size(max = 100)
        @Schema(description = "IANA timezone identifier", example = "Europe/Tirane")
        String timezone,

        @Schema(description = "Latitude coordinate of the site's center", example = "41.3275")
        BigDecimal latitude,

        @Schema(description = "Longitude coordinate of the site's center", example = "19.8189")
        BigDecimal longitude,

        @Schema(description = "Shape type for geofencing (CIRCLE, POLYGON, etc.)")
        GeofenceShapeType geofenceShapeType,

        @Schema(description = "Radius in meters for circular geofence", example = "100")
        Integer geofenceRadiusMeters,

        @Schema(description = "GeoJSON representation for polygon geofence")
        String geofencePolygonGeoJson,

        @Schema(description = "Buffer distance in meters for entry detection")
        Integer entryBufferMeters,

        @Schema(description = "Buffer distance in meters for exit detection")
        Integer exitBufferMeters,

        @Schema(description = "Maximum allowed GPS accuracy in meters for valid actions")
        Integer maxLocationAccuracyMeters,

        @Schema(description = "Whether GPS location is strictly required for clocking")
        Boolean locationRequired,

        @Schema(description = "Whether QR code scanning is enabled as an alternative")
        Boolean qrEnabled,

        @Schema(description = "Whether check-in is enabled for this site")
        Boolean checkInEnabled,

        @Schema(description = "Whether check-out is enabled for this site")
        Boolean checkOutEnabled,

        @Schema(description = "Internal notes for administrators")
        String notes
) {
}
