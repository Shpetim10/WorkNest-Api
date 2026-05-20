package com.worknest.features.subscription.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class PlanLimitExceededException extends BusinessException {

    public PlanLimitExceededException(String message) {
        super(HttpStatus.FORBIDDEN, "PLAN_LIMIT_EXCEEDED", message);
    }
}
