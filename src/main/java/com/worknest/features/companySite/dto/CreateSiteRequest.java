package com.worknest.features.companySite.dto;

import com.worknest.domain.enums.SiteType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Single payload for {@code POST /api/v1/companies/{companyId}/sites}.
 *
 * <p>This is the one-shot create-site request. It carries:
 * <ol>
 *   <li>Site business data (name, code, type, etc.).</li>
 *   <li>Final, admin-reviewed location fields (coordinates + address + geofence).</li>
 *   <li>An optional list of trusted-network rules to attach in the same transaction.</li>
 * </ol>
 *
 * <p>The server performs strict validation on every field before any persistence occurs.
 * No intermediate/draft state is created.
 */
@Schema(description = "One-shot payload to create a company site and optional trusted networks in a single transaction.")
public record CreateSiteRequest(

        // ── Business info ─────────────────────────────────────────────────────────

        @NotBlank(message = "Site name is required")
        @Size(max = 255, message = "Site name must not exceed 255 characters")
        @Schema(description = "Human-readable name for the site.", example = "Tirana Head Office")
        String name,

        @NotBlank(message = "Site code is required")
        @Size(max = 50, message = "Site code must not exceed 50 characters")
        @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Site code must contain only uppercase letters, digits, hyphens, or underscores")
        @Schema(description = "Short, unique alphanumeric code scoped to the company.", example = "TIR-HQ")
        String code,

        @NotNull(message = "Site type is required")
        @Schema(description = "Operational type of the site.", example = "HQ")
        SiteType type,

        @NotBlank(message = "Country code is required")
        @Size(min = 2, max = 2, message = "Country code must be exactly 2 characters (ISO 3166-1 alpha-2)")
        @Schema(description = "ISO 3166-1 alpha-2 country code.", example = "AL")
        String countryCode,

        @NotBlank(message = "Timezone is required")
        @Size(max = 100, message = "Timezone must not exceed 100 characters")
        @Schema(description = "IANA time-zone identifier.", example = "Europe/Tirane")
        String timezone,

        @Schema(description = "Free-text notes or internal remarks for this site.", example = "Primary HQ site — 3rd floor")
        String notes,

        // ── Attendance feature flags ──────────────────────────────────────────────

        @Schema(description = "Whether clock-in requires location verification. Defaults to true.", example = "true")
        Boolean locationRequired,

        @Schema(description = "Whether QR-code check-in is enabled for this site. Defaults to true.", example = "true")
        Boolean qrEnabled,

        @Schema(description = "Whether clock-in is enabled. Defaults to true.", example = "true")
        Boolean checkInEnabled,

        @Schema(description = "Whether clock-out is enabled. Defaults to true.", example = "true")
        Boolean checkOutEnabled,

        // ── Location & geofence ───────────────────────────────────────────────────

        @NotNull(message = "Location data is required")
        @Valid
        @Schema(description = "Authoritative coordinates (lat/lng), reverse-geocoded address, and geofence configuration.")
        SiteLocationRequest location,

        // ── Trusted networks (optional) ───────────────────────────────────────────

        @Valid
        @Schema(description = "Optional list of trusted-network rules to create alongside the site. May be empty or null.")
        List<@Valid TrustedNetworkRequest> trustedNetworks
) {}
