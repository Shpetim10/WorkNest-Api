package com.worknest.features.company.dto;

import com.worknest.common.i18n.Language;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Core company settings used for UI formatting, localization, and display.")
public record CompanySettingsResponse(

        @Schema(description = "Unique identifier of the company", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID companyId,

        @Schema(description = "Display name of the company", example = "Acme Corp")
        String name,

        @Schema(description = "IANA timezone identifier used for scheduling and display", example = "Europe/Tirane")
        String timezone,

        @Schema(description = "Date format used throughout the UI", example = "DD/MM/YYYY")
        String dateFormat,

        @Schema(description = "ISO 4217 currency code", example = "ALL")
        String currency,

        @Schema(description = "BCP 47 locale / language code", example = "SQ")
        Language locale,

        @Schema(description = "ISO 3166-1 alpha-2 country code", example = "AL")
        String countryCode,

        @Schema(description = "Public URL of the company logo, or null if not set", example = "https://cdn.example.com/logos/acme.png")
        String logoPath
) {}
