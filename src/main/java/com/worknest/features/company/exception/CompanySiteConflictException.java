package com.worknest.features.company.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class CompanySiteConflictException extends BusinessException {

    public CompanySiteConflictException(String message) {
        super(HttpStatus.CONFLICT, "SITE_CONFLICT", message);
    }
}
