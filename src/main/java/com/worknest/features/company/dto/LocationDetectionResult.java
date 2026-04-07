package com.worknest.features.company.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * Server assessment result for POST /api/v1/sites/{siteId}/detect-location.
 *
 * <p>This response is advisory — coordinates are NOT persisted by the detect call.
 * The admin inspects the result, adjusts on the map, then calls
 * PUT /api/v1/sites/{siteId}/location to commit the confirmed values.
 */
@Schema(description = "Server-side assessment of browser-supplied geolocation data")
public record LocationDetectionResult(

        @Schema(description = "Whether the coordinates are considered usable for geofence configuration")
        boolean usable,

        @Schema(description = "Latitude as reported by the browser", example = "41.3275")
        BigDecimal latitude,

        @Schema(description = "Longitude as reported by the browser", example = "19.8189")
        BigDecimal longitude,

        @Schema(description = "Accuracy radius reported by the browser (metres)", example = "45.0")
        Double accuracyMeters,

        @Schema(
                description = "Age of the browser-supplied fix in milliseconds at the time this request arrived. "
                        + "Values above 30 000 ms (30 s) produce a STALE_COORDINATES warning.",
                example = "1200"
        )
        Long coordinateAgeMs,

        @Schema(
                description = "Whether the browser timestamp was more than 30 seconds old when received. "
                        + "Stale coordinates must not be silently accepted.",
                example = "false"
        )
        boolean stale,

        @Schema(
                description = "Whether the accuracy is worse than the site's maxLocationAccuracyMeters "
                        + "(or worse than 100 m when no site override is set).",
                example = "false"
        )
        boolean lowAccuracy,

        @Schema(
                description = "Suggested geofence radius in metres, computed as: "
                        + "(effectiveMaxAccuracy × 2) + 30. "
                        + "Null when accuracy data is unavailable.",
                example = "120"
        )
        Integer suggestedRadiusMeters,

        @Schema(description = "Non-blocking warnings that the wizard should surface to the admin")
        List<SiteSetupIssueResponse> warnings
) {}
