package com.worknest.features.subscription.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class StripeException extends BusinessException {

    public StripeException(String message) {
        super(HttpStatus.BAD_GATEWAY, "STRIPE_ERROR", message);
    }
}
