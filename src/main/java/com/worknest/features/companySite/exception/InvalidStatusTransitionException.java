package com.worknest.features.companySite.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an invalid site status transition is attempted directly via update.
 * Maps to HTTP 400 Bad Request.
 */
public class InvalidStatusTransitionException extends BusinessException {

    public InvalidStatusTransitionException(String message) {
        super(
                HttpStatus.BAD_REQUEST,
                "INVALID_STATUS_TRANSITION",
                message
        );
    }
}
