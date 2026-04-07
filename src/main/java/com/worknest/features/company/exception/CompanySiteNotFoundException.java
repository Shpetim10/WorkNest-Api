package com.worknest.features.company.exception;

import com.worknest.common.exception.ResourceNotFoundException;
import java.util.UUID;

public class CompanySiteNotFoundException extends ResourceNotFoundException {

    public CompanySiteNotFoundException(UUID siteId) {
        super("SITE_NOT_FOUND", "Site '" + siteId + "' was not found for the current company");
    }
}
