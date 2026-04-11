package com.worknest.features.companySite.exception;

import com.worknest.common.exception.ResourceNotFoundException;

/**
 * Thrown when the target company does not exist or is not visible to the caller.
 */
public class CompanyNotFoundException extends ResourceNotFoundException {

    public CompanyNotFoundException() {
        super("COMPANY_NOT_FOUND", "Company not found or access denied.");
    }
}
