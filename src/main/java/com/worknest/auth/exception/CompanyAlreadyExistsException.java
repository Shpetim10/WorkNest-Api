package com.worknest.auth.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class CompanyAlreadyExistsException extends BusinessException {

    public CompanyAlreadyExistsException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
