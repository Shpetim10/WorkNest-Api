package com.worknest.features.subscription.dto;

import com.worknest.domain.enums.SubscriptionPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to start a 30-day free trial")
public record StartTrialRequest(

        @NotNull
        @Schema(description = "Plan to trial — FOUNDATION, GROWTH, or PROFESSIONAL", example = "GROWTH")
        SubscriptionPlan plan,

        @NotBlank
        @Schema(description = "Stripe PaymentMethod ID collected on the frontend (card required upfront)", example = "pm_card_visa")
        String paymentMethodId
) {
}
