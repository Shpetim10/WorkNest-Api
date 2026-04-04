package com.worknest.features.auth.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a password reset token has expired.
 */
public class ResetTokenExpiredException extends BusinessException {

    public ResetTokenExpiredException(String message) {
        super(HttpStatus.BAD_REQUEST, "RESET_TOKEN_EXPIRED", message);
    }
}
