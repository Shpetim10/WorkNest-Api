package com.worknest.features.company.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class AdminEmailAlreadyExistsException extends BusinessException {

    public AdminEmailAlreadyExistsException(String email) {
        super(HttpStatus.CONFLICT, "ADMIN_EMAIL_ALREADY_EXISTS", "An admin user with email '" + email + "' already exists in this company");
    }
}
