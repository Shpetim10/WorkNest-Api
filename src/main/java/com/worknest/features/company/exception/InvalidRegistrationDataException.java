package com.worknest.features.company.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when the inbound registration request fails structural or business validation
 * (e.g. missing mandatory fields, invalid slug format, failed consent).
 */
public class InvalidRegistrationDataException extends BusinessException {

    public InvalidRegistrationDataException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_REGISTRATION_DATA", message);
    }
}
