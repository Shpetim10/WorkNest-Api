package com.worknest.features.companySite.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting to update a site with an outdated optimistic lock version.
 * Maps to HTTP 409 Conflict.
 */
public class StaleSiteDataException extends BusinessException {

    public StaleSiteDataException() {
        super(
                HttpStatus.CONFLICT,
                "STALE_SITE_DATA_CONFLICT",
                "This site was recently modified by another user. Please refresh and try again."
        );
    }
}
