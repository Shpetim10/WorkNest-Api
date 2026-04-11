package com.worknest.features.companySite.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when geofence fields are incomplete or contradictory.
 * <ul>
 *   <li>CIRCLE shape but no {@code geofenceRadiusMeters}.</li>
 *   <li>POLYGON shape but no {@code geofencePolygonGeoJson}.</li>
 *   <li>Invalid or unparseable GeoJSON polygon.</li>
 * </ul>
 * Maps to HTTP 422 Unprocessable Entity.
 */
public class InvalidGeofenceException extends BusinessException {

    public InvalidGeofenceException(String detail) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_GEOFENCE", detail);
    }
}
