package com.worknest.features.companySite.dto;

import com.worknest.domain.enums.SiteType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "One-shot payload to create a company site with location, trusted networks, and attendance policy.")
public record CreateSiteRequest(

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
        @Size(min = 2, max = 2, message = "Country code must be exactly 2 characters")
        @Schema(description = "ISO 3166-1 alpha-2 country code.", example = "AL")
        String countryCode,

        @NotBlank(message = "Timezone is required")
        @Size(max = 100, message = "Timezone must not exceed 100 characters")
        @Schema(description = "IANA time-zone identifier.", example = "Europe/Tirane")
        String timezone,

        @Schema(description = "Free-text notes or internal remarks for this site.", example = "Primary HQ site - 3rd floor")
        String notes,

        @NotNull(message = "Location data is required")
        @Valid
        @Schema(description = "Authoritative coordinates, address, and geofence configuration.")
        SiteLocationRequest location,

        @Valid
        @Schema(description = "Optional trusted network rules to create alongside the site.")
        List<@Valid TrustedNetworkRequest> trustedNetworks,

        @NotNull(message = "Attendance policy is required. Send the 'attendancePolicy' object with all attendance settings.")
        @Valid
        @Schema(description = "Attendance policy configured during site creation.")
        SiteAttendancePolicyCreateRequest attendancePolicy
) {
}
