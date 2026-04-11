package com.worknest.features.companySite.dto;

import com.worknest.domain.enums.LocationDetectionSource;
import com.worknest.domain.enums.GeofenceShapeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Location and geofence sub-section of {@link CreateSiteRequest}.
 *
 * <p>Latitude and longitude are the authoritative location fields. The address fields
 * (addressLine1, city, etc.) are UX assistance from reverse-geocoding and are
 * stored for display purposes; they are never used for attendance verification.
 *
 * <p>When {@code geofenceShapeType} is {@code CIRCLE}, {@code geofenceRadiusMeters}
 * must be provided. When it is {@code POLYGON}, {@code geofencePolygonGeoJson}
 * must be provided. Full server-side validation is enforced in the service layer.
 */
@Schema(description = "Location coordinates, reverse-geocoded address fields, and geofence configuration for the new site.")
public record SiteLocationRequest(

        // --- Authoritative coordinates ---

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
        @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
        @Schema(description = "WGS-84 latitude captured by navigator.geolocation or adjusted by the admin.", example = "41.3275")
        BigDecimal latitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
        @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
        @Schema(description = "WGS-84 longitude captured by navigator.geolocation or adjusted by the admin.", example = "19.8187")
        BigDecimal longitude,

        // --- Reverse-geocoded address (display only) ---

        @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
        @Schema(description = "Street address line 1 from reverse geocoding (Nominatim). Editable by admin.", example = "Rruga Ismail Qemali")
        String addressLine1,

        @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
        @Schema(description = "Street address line 2 (optional).", example = "Floor 3")
        String addressLine2,

        @Size(max = 100, message = "City must not exceed 100 characters")
        @Schema(description = "City from reverse geocoding.", example = "Tirana")
        String city,

        @Size(max = 100, message = "State/region must not exceed 100 characters")
        @Schema(description = "State or region from reverse geocoding.", example = "Tirana County")
        String stateRegion,

        @Size(max = 30, message = "Postal code must not exceed 30 characters")
        @Schema(description = "Postal code from reverse geocoding.", example = "1001")
        String postalCode,

        @Size(min = 2, max = 2, message = "Country code from location must be exactly 2 characters")
        @Schema(description = "Country code from reverse geocoding. Used to cross-validate with the site's primary country code.", example = "AL")
        String countryCode,

        // --- Geofence configuration ---

        @NotNull(message = "Geofence shape type is required")
        @Schema(description = "Shape of the geofence: CIRCLE or POLYGON.", example = "CIRCLE")
        GeofenceShapeType geofenceShapeType,

        @Min(value = 10, message = "Geofence radius must be at least 10 meters")
        @Schema(description = "Radius in meters. Required when geofenceShapeType is CIRCLE.", example = "150")
        Integer geofenceRadiusMeters,

        @Size(max = 65535, message = "GeoJSON polygon must not exceed 65535 characters")
        @Schema(description = "GeoJSON Polygon string. Required when geofenceShapeType is POLYGON.")
        String geofencePolygonGeoJson,

        @Min(value = 0, message = "Entry buffer must be >= 0 meters")
        @Schema(description = "Extra metres added inside the geofence boundary to reduce false negatives at edges.", example = "20")
        Integer entryBufferMeters,

        @Min(value = 0, message = "Exit buffer must be >= 0 meters")
        @Schema(description = "Extra metres added outside the geofence boundary before an exit event triggers.", example = "30")
        Integer exitBufferMeters,

        @Min(value = 1, message = "Max location accuracy must be at least 1 meter")
        @Schema(description = "Maximum acceptable GPS accuracy in metres. Clock-in attempts with worse accuracy are rejected.", example = "50")
        Integer maxLocationAccuracyMeters,

        @Schema(description = "How the location coordinates were originally captured in the frontend.", example = "BROWSER_GEOLOCATION")
        LocationDetectionSource locationDetectionSource
) {}
