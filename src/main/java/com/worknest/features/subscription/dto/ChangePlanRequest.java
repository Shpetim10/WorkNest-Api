package com.worknest.features.subscription.dto;

import com.worknest.domain.enums.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;

public record ChangePlanRequest(
        @NotNull(message = "New plan is required")
        SubscriptionPlan newPlan
) {}
