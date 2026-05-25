package com.worknest.features.subscription.web;

import com.worknest.features.subscription.application.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Tag(name = "Stripe Webhooks", description = "Stripe webhook event receiver")
public class StripeWebhookController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/stripe")
    @Operation(
            summary = "Stripe webhook endpoint",
            description = "Receives Stripe events. Authentication is via Stripe signature header — no JWT required."
    )
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        subscriptionService.handleStripeEvent(payload, sigHeader);
        return ResponseEntity.ok().build();
    }
}
