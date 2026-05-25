package com.worknest.features.subscription.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class SubscriptionNotFoundException extends BusinessException {

    public SubscriptionNotFoundException() {
        super(HttpStatus.NOT_FOUND, "SUBSCRIPTION_NOT_FOUND", "No active subscription found for this company.");
    }
}
