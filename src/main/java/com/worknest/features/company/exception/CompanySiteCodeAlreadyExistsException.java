package com.worknest.features.company.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class CompanySiteCodeAlreadyExistsException extends BusinessException {

    public CompanySiteCodeAlreadyExistsException(String code) {
        super(HttpStatus.CONFLICT, "SITE_CODE_ALREADY_EXISTS", "A site with code '" + code + "' already exists in this company");
    }
}
