package com.worknest.features.companySite.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a site code already exists for the same company.
 * Maps to HTTP 409 Conflict.
 */
public class SiteCodeAlreadyExistsException extends BusinessException {

    public SiteCodeAlreadyExistsException(String code) {
        super(
                HttpStatus.CONFLICT,
                "SITE_CODE_ALREADY_EXISTS",
                "A site with code '" + code + "' already exists for this company."
        );
    }
}
