package com.worknest.features.subscription.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdatePaymentMethodRequest(
        @NotBlank String paymentMethodId
) {}
