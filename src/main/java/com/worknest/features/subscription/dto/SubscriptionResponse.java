package com.worknest.features.subscription.dto;

import com.worknest.domain.enums.SubscriptionPlan;
import com.worknest.domain.enums.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Current subscription status for the authenticated company")
public record SubscriptionResponse(

        @Schema(description = "Local subscription record ID — null when no Stripe subscription exists yet")
        UUID subscriptionId,

        @Schema(description = "Active plan")
        SubscriptionPlan plan,

        @Schema(description = "Subscription lifecycle status")
        SubscriptionStatus status,

        @Schema(description = "When the trial ends (null if not trialing)")
        Instant trialEndsAt,

        @Schema(description = "When the current billing period ends")
        Instant currentPeriodEnd,

        @Schema(description = "When deactivation was requested (null if not pending)")
        Instant deactivationRequestedAt,

        @Schema(description = "When the company will be permanently deleted (null if not pending)")
        Instant deletionScheduledAt
) {
}
