package com.worknest.features.companySite.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Aggregated details of a company site, including its basic info, location, and network rules.
 * Used for the site details view.
 */
@Schema(description = "Comprehensive details of a company site and its trusted networks.")
public record CompanySiteDetailsResponse(
        @Schema(description = "Basic site information.")
        CompanySiteResponse site,

        @Schema(description = "Full name of the country.", example = "Albania")
        String countryName,

        @Schema(description = "All associated trusted-network rules.")
        List<TrustedNetworkResponse> trustedNetworks
) {
}
