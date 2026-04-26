package com.worknest.features.attendance.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public final class GeoJsonPolygonUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private GeoJsonPolygonUtil() {
    }

    public static boolean isPointInside(String polygonGeoJson, double latitude, double longitude) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(polygonGeoJson);
            JsonNode coordinates = root.path("coordinates");
            if (!coordinates.isArray() || coordinates.isEmpty()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_POLYGON", "Polygon GeoJSON coordinates are invalid.");
            }

            JsonNode ring = coordinates.get(0);
            int n = ring.size();
            if (n < 4) {
                return false;
            }

            boolean inside = false;
            for (int i = 0, j = n - 1; i < n; j = i++) {
                double xi = ring.get(i).get(0).asDouble();
                double yi = ring.get(i).get(1).asDouble();
                double xj = ring.get(j).get(0).asDouble();
                double yj = ring.get(j).get(1).asDouble();

                boolean intersects = ((yi > latitude) != (yj > latitude))
                        && (longitude < (xj - xi) * (latitude - yi) / ((yj - yi) + 1e-12) + xi);
                if (intersects) {
                    inside = !inside;
                }
            }
            return inside;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_POLYGON", "Polygon GeoJSON is invalid.");
        }
    }
}
