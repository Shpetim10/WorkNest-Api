package com.worknest.auth.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a password reset token is structurally invalid or not found.
 */
public class ResetTokenInvalidException extends BusinessException {

    public ResetTokenInvalidException(String message) {
        super(HttpStatus.BAD_REQUEST, "RESET_TOKEN_INVALID", message);
    }
}
