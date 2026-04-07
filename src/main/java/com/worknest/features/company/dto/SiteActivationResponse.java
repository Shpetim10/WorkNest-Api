package com.worknest.features.company.dto;

import com.worknest.domain.enums.SiteStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Activation or dry-run activation result for a site")
public record SiteActivationResponse(
        @Schema(description = "Site identifier")
        UUID siteId,

        @Schema(description = "Whether this was a dry-run validation")
        boolean dryRun,

        @Schema(description = "Whether the site transitioned to ACTIVE")
        boolean activated,

        @Schema(description = "Current site status after processing")
        SiteStatus status,

        @Schema(description = "Whether the site is ready to activate")
        boolean readyToActivate,

        @Schema(description = "Blocking issues that prevented activation")
        List<SiteSetupIssueResponse> blockingIssues,

        @Schema(description = "Warnings returned by validation")
        List<SiteSetupIssueResponse> warnings,

        @Schema(description = "Current snapshot of the site")
        CompanySiteResponse site
) {
}
