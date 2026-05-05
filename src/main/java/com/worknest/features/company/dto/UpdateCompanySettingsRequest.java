package com.worknest.features.company.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for updating company settings.")
public record UpdateCompanySettingsRequest(

        @NotBlank
        @Size(max = 255)
        @Schema(description = "Display name of the company", example = "Acme Corp")
        String name,

        @Size(max = 30)
        @Schema(description = "Tax identification number (NIPT)", example = "K12345678A")
        String nipt,

        @Size(max = 50)
        @Schema(description = "Primary contact phone number", example = "+355 69 123 4567")
        String phoneNumber,

        @Size(max = 100)
        @Schema(description = "Business sector or industry", example = "Technology")
        String industry,

        @Size(max = 10)
        @Schema(description = "ISO 4217 currency code", example = "ALL")
        String currency,

        @Size(max = 20)
        @Schema(description = "Date format used throughout the UI", example = "DD/MM/YYYY")
        String dateFormat,

        @Size(max = 100)
        @Schema(description = "IANA timezone identifier", example = "Europe/Tirane")
        String timezone,

        @Size(max = 2)
        @Pattern(regexp = "^[A-Z]{2}$", message = "countryCode must be a 2-letter ISO 3166-1 alpha-2 code")
        @Schema(description = "ISO 3166-1 alpha-2 country code", example = "AL")
        String countryCode,

        @Size(max = 500)
        @Schema(description = "Storage key returned by the media upload endpoint", example = "companies/uuid/branding/logo/2025/01/uuid.jpg")
        String logoKey,

        @Size(max = 1000)
        @Schema(description = "Storage path returned by the media upload endpoint")
        String logoPath,

        @Schema(description = "When true, removes the existing company logo")
        Boolean clearLogo
) {}
