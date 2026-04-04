package com.worknest.features.auth.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class UserAlreadyActiveException extends BusinessException {

    public UserAlreadyActiveException(String email) {
        super(HttpStatus.CONFLICT, "USER_ALREADY_ACTIVE", "User '" + email + "' is already active");
    }
}
