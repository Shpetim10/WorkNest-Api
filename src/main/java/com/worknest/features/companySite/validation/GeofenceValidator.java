package com.worknest.features.companySite.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worknest.domain.enums.GeofenceShapeType;
import com.worknest.features.companySite.dto.SiteLocationRequest;
import com.worknest.features.companySite.exception.InvalidGeofenceException;

/**
 * Stateless geofence completeness validator.
 *
 * <p>Enforces the invariant that a geofence is internally consistent
 * before the site reaches the persistence layer:
 * <ul>
 *   <li>CIRCLE → {@code geofenceRadiusMeters} must be present and positive.</li>
 *   <li>POLYGON → {@code geofencePolygonGeoJson} must be a non-blank string;
 *       basic structural check (starts with {@code {}) is performed.</li>
 * </ul>
 *
 * <p>All methods throw {@link InvalidGeofenceException} on failure.
 */
public final class GeofenceValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GeofenceValidator() {
        // utility class
    }

    /**
     * Validates that the geofence fields in the location sub-request are complete
     * and internally consistent for the selected {@code geofenceShapeType}.
     *
     * @param location the location sub-request containing geofence configuration
     * @throws InvalidGeofenceException if the geofence is incomplete or contradictory
     */
    public static void validate(SiteLocationRequest location) {
        GeofenceShapeType shape = location.geofenceShapeType();

        if (shape == null) {
            throw new InvalidGeofenceException("Geofence shape type must be specified (CIRCLE or POLYGON).");
        }

        switch (shape) {
            case CIRCLE -> {
                if (location.geofenceRadiusMeters() == null || location.geofenceRadiusMeters() < 10) {
                    throw new InvalidGeofenceException(
                            "A CIRCLE geofence requires 'geofenceRadiusMeters' to be at least 10 metres."
                    );
                }
                if (location.geofencePolygonGeoJson() != null && !location.geofencePolygonGeoJson().isBlank()) {
                    throw new InvalidGeofenceException(
                            "A CIRCLE geofence must not include 'geofencePolygonGeoJson'. Remove it or set geofenceShapeType to POLYGON."
                    );
                }
            }
            case POLYGON -> {
                if (location.geofencePolygonGeoJson() == null || location.geofencePolygonGeoJson().isBlank()) {
                    throw new InvalidGeofenceException(
                            "A POLYGON geofence requires a non-blank 'geofencePolygonGeoJson'."
                    );
                }
                String geoJson = location.geofencePolygonGeoJson().trim();
                try {
                    MAPPER.readTree(geoJson);
                } catch (Exception e) {
                    throw new InvalidGeofenceException(
                            "'geofencePolygonGeoJson' is not valid JSON."
                    );
                }
                if (location.geofenceRadiusMeters() != null) {
                    throw new InvalidGeofenceException(
                            "A POLYGON geofence must not include 'geofenceRadiusMeters'. Remove it or set geofenceShapeType to CIRCLE."
                    );
                }
            }
        }
    }
}
