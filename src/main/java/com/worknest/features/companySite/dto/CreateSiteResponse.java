package com.worknest.features.companySite.dto;

import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.enums.GeofenceShapeType;
import com.worknest.domain.enums.LocationDetectionSource;
import com.worknest.domain.enums.SiteStatus;
import com.worknest.domain.enums.SiteType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response returned by {@code POST /api/v1/companies/{companyId}/sites}.
 *
 * <p>Contains the full serialized state of the newly created {@link CompanySite}
 * plus all {@link TrustedNetworkResponse} entries created in the same transaction.
 */
@Schema(description = "Full representation of the newly created company site and its trusted-network rules.")
public record CreateSiteResponse(

        // ── Identity ──────────────────────────────────────────────────────────────

        @Schema(description = "Stable UUID of the created site.")
        UUID id,

        @Schema(description = "UUID of the owning company.")
        UUID companyId,

        @Schema(description = "Site code, normalized to uppercase.", example = "TIR-HQ")
        String code,

        @Schema(description = "Human-readable site name.", example = "Tirana Head Office")
        String name,

        @Schema(description = "Operational type of the site.", example = "HQ")
        SiteType type,

        @Schema(description = "Initial lifecycle status. Always PENDING_REVIEW for newly created sites.", example = "PENDING_REVIEW")
        SiteStatus status,

        @Schema(description = "Optimistic-lock version token. Callers must echo this on subsequent updates.")
        long version,

        // ── Location ──────────────────────────────────────────────────────────────

        @Schema(example = "41.3275")   BigDecimal latitude,
        @Schema(example = "19.8187")   BigDecimal longitude,
        @Schema(example = "AL")        String countryCode,
        @Schema(example = "Europe/Tirane") String timezone,

        String addressLine1,
        String addressLine2,
        String city,
        String stateRegion,
        String postalCode,

        @Schema(description = "How the location coordinates were originally captured.", example = "BROWSER_GEOLOCATION")
        LocationDetectionSource locationDetectionSource,

        // ── Geofence ──────────────────────────────────────────────────────────────

        @Schema(example = "CIRCLE")    GeofenceShapeType geofenceShapeType,
        @Schema(example = "150")       Integer geofenceRadiusMeters,
                                       String  geofencePolygonGeoJson,
        @Schema(example = "20")        Integer entryBufferMeters,
        @Schema(example = "30")        Integer exitBufferMeters,
        @Schema(example = "50")        Integer maxLocationAccuracyMeters,

        // ── Feature flags ─────────────────────────────────────────────────────────

        boolean locationRequired,
        boolean qrEnabled,
        boolean checkInEnabled,
        boolean checkOutEnabled,

        String notes,

        // ── Timestamps ────────────────────────────────────────────────────────────

        Instant createdAt,
        Instant updatedAt,

        // ── Associated trusted-network rules ─────────────────────────────────────

        @Schema(description = "All trusted-network rules created in the same transaction.")
        List<TrustedNetworkResponse> trustedNetworks
) {

    /**
     * Maps a persisted {@link CompanySite} plus its created network rules to this DTO.
     */
    public static CreateSiteResponse fromEntity(CompanySite site, List<TrustedNetworkResponse> networks) {
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
                Boolean.TRUE.equals(site.getLocationRequired()),
                Boolean.TRUE.equals(site.getQrEnabled()),
                Boolean.TRUE.equals(site.getCheckInEnabled()),
                Boolean.TRUE.equals(site.getCheckOutEnabled()),
                site.getNotes(),
                site.getCreatedAt(),
                site.getUpdatedAt(),
                networks
        );
    }
}
