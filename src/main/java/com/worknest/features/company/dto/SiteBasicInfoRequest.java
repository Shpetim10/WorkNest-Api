package com.worknest.features.company.dto;

import com.worknest.domain.enums.SiteType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for PUT /api/v1/sites/{siteId}/basic-info (wizard Step 1 re-save).
 * Idempotent: sending the same body twice leaves the site in the same state.
 */
@Schema(description = "Payload for saving or updating a site's basic information")
public record SiteBasicInfoRequest(

        @NotBlank
        @Size(max = 50)
        @Schema(description = "Short internal code, unique within the company", example = "HQ-TIR")
        String code,

        @NotBlank
        @Size(max = 255)
        @Schema(description = "Human-readable display name of the site", example = "Tirana Headquarters")
        String name,

        @NotNull
        @Schema(description = "Functional classification of the site")
        SiteType type,

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
        @Schema(description = "State or province")
        String stateRegion,

        @Size(max = 30)
        @Schema(description = "Postal / zip code", example = "1001")
        String postalCode,

        @NotBlank
        @Size(max = 2)
        @Schema(description = "ISO 3166-1 alpha-2 country code", example = "AL")
        String countryCode,

        @NotBlank
        @Size(max = 100)
        @Schema(description = "IANA timezone identifier", example = "Europe/Tirane")
        String timezone,

        @Schema(description = "Whether check-in is enabled for this site")
        Boolean checkInEnabled,

        @Schema(description = "Whether check-out is enabled for this site")
        Boolean checkOutEnabled,

        @Schema(description = "Whether QR code scanning is enabled as an alternative clock method")
        Boolean qrEnabled,

        @Schema(description = "Optimistic-lock token from the last read of the site", example = "3")
        Long version,

        @Schema(description = "Internal notes for administrators")
        String notes
) {}
