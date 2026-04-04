package com.worknest.features.auth.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class AuthenticationFailedException extends BusinessException {

    public AuthenticationFailedException(String code, String message) {
        super(HttpStatus.UNAUTHORIZED, code, message);
    }
}
