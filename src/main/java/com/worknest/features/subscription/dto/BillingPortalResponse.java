package com.worknest.features.subscription.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Stripe Billing Portal session URL")
public record BillingPortalResponse(

        @Schema(description = "Redirect the user to this URL to manage their subscription in Stripe")
        String url
) {
}
