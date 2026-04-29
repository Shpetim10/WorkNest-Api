package com.worknest.features.auth.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class UserEmailAlreadyExistsException extends BusinessException {

    public UserEmailAlreadyExistsException(String email) {
        super(HttpStatus.CONFLICT, "USER_EMAIL_ALREADY_EXISTS", "User email '" + email + "' is already registered.");
    }
}
