package com.worknest.features.company.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when the designated administrator email is already registered 
 * and active within the same company context during registration.
 */
public class AdminEmailAlreadyExistsException extends BusinessException {

    public AdminEmailAlreadyExistsException(String email) {
        super(
            HttpStatus.CONFLICT, 
            "ADMIN_EMAIL_ALREADY_EXISTS", 
            "The administrator email '" + email + "' is already registered."
        );
    }
}
