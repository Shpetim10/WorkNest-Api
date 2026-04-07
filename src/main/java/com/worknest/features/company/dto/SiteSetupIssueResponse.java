package com.worknest.features.company.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Structured issue or warning returned by site setup validation")
public record SiteSetupIssueResponse(
        @Schema(description = "Stable code for programmatic handling", example = "SITE_LOCATION_INCOMPLETE")
        String code,

        @Schema(description = "Human-readable issue message")
        String message,

        @Schema(description = "Optional field or section associated with the issue", example = "location")
        String field
) {
}
