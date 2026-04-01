package com.worknest.auth.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a password reset token has already been used.
 */
public class ResetTokenAlreadyUsedException extends BusinessException {

    public ResetTokenAlreadyUsedException(String message) {
        super(HttpStatus.BAD_REQUEST, "RESET_TOKEN_ALREADY_USED", message);
    }
}
