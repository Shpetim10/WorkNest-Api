package com.worknest.features.company.dto;

import com.worknest.domain.enums.SiteType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Minimal payload required to create a site draft.
 * Only the fields that belong to "Step 1 – Basic Info" are required here.
 * Everything else is populated through subsequent PUT step-saves.
 */
@Schema(description = "Request to create a new site draft under a company")
public record CreateSiteDraftRequest(

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

        @NotBlank
        @Size(max = 2)
        @Schema(description = "ISO 3166-1 alpha-2 country code", example = "AL")
        String countryCode,

        @NotBlank
        @Size(max = 100)
        @Schema(description = "IANA timezone identifier", example = "Europe/Tirane")
        String timezone,

        @Schema(description = "Optional administrator notes")
        String notes
) {}
