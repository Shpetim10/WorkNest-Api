package com.worknest.features.auth.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class WeakPasswordException extends BusinessException {

    public WeakPasswordException(String message) {
        super(HttpStatus.BAD_REQUEST, "WEAK_PASSWORD", message);
    }
}
