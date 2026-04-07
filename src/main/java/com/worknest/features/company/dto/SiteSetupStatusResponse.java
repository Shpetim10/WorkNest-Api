package com.worknest.features.company.dto;

import com.worknest.domain.enums.SiteStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Computed setup status for a site draft or active site")
public record SiteSetupStatusResponse(
        @Schema(description = "Site identifier")
        UUID siteId,

        @Schema(description = "Current site status")
        SiteStatus status,

        @Schema(description = "Current optimistic-lock version of the site")
        Long version,

        @Schema(description = "Whether the basic-info step is complete")
        boolean basicInfoComplete,

        @Schema(description = "Whether the location step is complete")
        boolean locationComplete,

        @Schema(description = "Whether the trusted-network step is complete")
        boolean networkComplete,

        @Schema(description = "Whether the site is currently ready for activation")
        boolean readyToActivate,

        @Schema(description = "Blocking issues that prevent activation")
        List<SiteSetupIssueResponse> blockingIssues,

        @Schema(description = "Non-blocking warnings surfaced to the wizard")
        List<SiteSetupIssueResponse> warnings,

        @Schema(description = "Current snapshot of the site")
        CompanySiteResponse site,

        @Schema(description = "Trusted-network rules currently attached to the site")
        List<TrustedNetworkResponse> trustedNetworks
) {
}
