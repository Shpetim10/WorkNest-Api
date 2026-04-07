package com.worknest.features.company.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Payload for POST /api/v1/sites/{siteId}/detect-location.
 *
 * <p>The client supplies the raw browser Geolocation API output.
 * The server validates staleness and accuracy but does NOT persist coordinates
 * from this call alone — the result is advisory only.
 * Coordinates are only written to the site record when the admin explicitly
 * confirms them via PUT /api/v1/sites/{siteId}/location.
 *
 * <p>Fields mirror the Geolocation API's {@code GeolocationCoordinates} shape
 * so the frontend can forward them without transformation.
 */
@Schema(description = "Browser Geolocation API coordinates submitted for server-side staleness and accuracy assessment")
public record LocationDetectionRequest(

        @Schema(description = "Latitude in decimal degrees (WGS-84)", example = "41.3275", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal latitude,

        @Schema(description = "Longitude in decimal degrees (WGS-84)", example = "19.8189", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal longitude,

        @Schema(
                description = "GPS accuracy radius reported by the browser, in metres. "
                        + "Values above maxLocationAccuracyMeters (or above 100 m when the site has no override) "
                        + "will produce an accuracy warning.",
                example = "45.0"
        )
        Double accuracyMeters,

        @Schema(
                description = "Unix epoch milliseconds reported by the browser via GeolocationPosition.timestamp. "
                        + "The server rejects or warns if |now - browserTimestamp| > 30 seconds.",
                example = "1712520000000"
        )
        Long browserTimestampMs
) {}
