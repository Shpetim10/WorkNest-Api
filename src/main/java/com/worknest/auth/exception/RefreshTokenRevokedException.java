package com.worknest.auth.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a refresh token has been revoked.
 */
public class RefreshTokenRevokedException extends BusinessException {

    public RefreshTokenRevokedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REVOKED", message);
    }
}
