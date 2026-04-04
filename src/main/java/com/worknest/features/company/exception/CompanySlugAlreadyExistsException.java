package com.worknest.features.company.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class CompanySlugAlreadyExistsException extends BusinessException {

    public CompanySlugAlreadyExistsException(String slug) {
        super(HttpStatus.CONFLICT, "COMPANY_SLUG_ALREADY_EXISTS", "A company with slug '" + slug + "' already exists");
    }
}
