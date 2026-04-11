package com.worknest.features.companySite.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class SiteNotFoundException extends BusinessException {
    public SiteNotFoundException() {
        super(HttpStatus.NOT_FOUND, "SITE_NOT_FOUND", "Company site was not found");
    }
}
