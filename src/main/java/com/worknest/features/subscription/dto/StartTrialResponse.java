package com.worknest.features.subscription.dto;

import com.worknest.domain.enums.SubscriptionPlan;
import com.worknest.domain.enums.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Response after starting a trial subscription")
public record StartTrialResponse(

        @Schema(description = "Local subscription record ID")
        UUID subscriptionId,

        @Schema(description = "Plan enrolled in")
        SubscriptionPlan plan,

        @Schema(description = "Current subscription status")
        SubscriptionStatus status,

        @Schema(description = "When the trial period ends")
        Instant trialEndsAt,

        @Schema(description = "Stripe SetupIntent client secret for confirming the payment method on the frontend")
        String clientSecret
) {
}
