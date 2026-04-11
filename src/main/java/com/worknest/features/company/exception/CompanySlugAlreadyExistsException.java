package com.worknest.features.company.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting to register a company with a slug that is already 
 * taken by another active company.
 */
public class CompanySlugAlreadyExistsException extends BusinessException {

    public CompanySlugAlreadyExistsException(String slug) {
        super(
            HttpStatus.CONFLICT, 
            "COMPANY_SLUG_ALREADY_EXISTS", 
            "The company slug '" + slug + "' is already in use."
        );
    }
}
