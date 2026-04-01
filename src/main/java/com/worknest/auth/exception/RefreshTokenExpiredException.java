package com.worknest.auth.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a refresh token has expired.
 */
public class RefreshTokenExpiredException extends BusinessException {

    public RefreshTokenExpiredException(String message) {
        super(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED", message);
    }
}
