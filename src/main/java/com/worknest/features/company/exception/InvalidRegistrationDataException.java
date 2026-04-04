package com.worknest.features.company.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidRegistrationDataException extends BusinessException {

    public InvalidRegistrationDataException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_REGISTRATION_DATA", message);
    }
}
