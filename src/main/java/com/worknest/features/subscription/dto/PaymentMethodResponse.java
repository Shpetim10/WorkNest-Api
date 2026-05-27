package com.worknest.features.subscription.dto;

public record PaymentMethodResponse(
        String brand,
        String last4,
        int expMonth,
        int expYear
) {}
