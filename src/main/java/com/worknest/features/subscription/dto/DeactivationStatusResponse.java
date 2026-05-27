package com.worknest.features.subscription.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Deactivation state for the company")
public record DeactivationStatusResponse(

        @Schema(description = "Whether a deactivation is pending")
        boolean deactivationRequested,

        @Schema(description = "When deactivation was requested (null if not pending)")
        Instant deactivationRequestedAt,

        @Schema(description = "When the company will be permanently deleted (null if not pending)")
        Instant deletionScheduledAt
) {
}
