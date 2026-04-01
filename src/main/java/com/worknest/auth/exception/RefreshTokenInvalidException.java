package com.worknest.auth.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a refresh token is structurally invalid or not found.
 */
public class RefreshTokenInvalidException extends BusinessException {

    public RefreshTokenInvalidException(String message) {
        super(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_INVALID", message);
    }
}
