package com.worknest.features.company.dto;

import com.worknest.domain.enums.GeofenceShapeType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Payload for PUT /api/v1/sites/{siteId}/location (wizard Step 2).
 *
 * <p>All geofence fields are nullable here on purpose — the plan explicitly requires
 * that draft saves are NEVER blocked by incomplete location data. Strict completeness
 * is validated only at activation time.
 *
 * <p>The IP version for the geofence is auto-derived from the CIDR; it is not
 * supplied by the client.
 */
@Schema(description = "Payload for saving or updating a site's location and geofence settings")
public record SiteLocationRequest(

        @Schema(description = "Latitude of the site centre point", example = "41.3275")
        BigDecimal latitude,

        @Schema(description = "Longitude of the site centre point", example = "19.8189")
        BigDecimal longitude,

        @Schema(description = "Shape used for the geofence boundary")
        GeofenceShapeType geofenceShapeType,

        @Schema(
                description = "Radius in metres for a CIRCLE geofence. "
                        + "Recommended formula: (maxLocationAccuracyMeters × 2) + 30",
                example = "130"
        )
        Integer geofenceRadiusMeters,

        @Schema(description = "GeoJSON Polygon string for a POLYGON geofence")
        String geofencePolygonGeoJson,

        @Schema(description = "Entry buffer in metres — helps reduce false-negatives at the boundary", example = "10")
        Integer entryBufferMeters,

        @Schema(description = "Exit buffer in metres", example = "10")
        Integer exitBufferMeters,

        @Schema(
                description = "Maximum acceptable GPS accuracy in metres. "
                        + "Clock events with weaker accuracy are rejected.",
                example = "50"
        )
        Integer maxLocationAccuracyMeters,

        @Schema(
                description = "When true, GPS location is mandatory for clock-in/out. "
                        + "When false, all geofence fields will be cleared on activation.",
                example = "true"
        )
        Boolean locationRequired,

        @Schema(description = "Optimistic-lock token from the last read of the site", example = "3")
        Long version
) {}
