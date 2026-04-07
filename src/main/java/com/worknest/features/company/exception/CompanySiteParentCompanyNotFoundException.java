package com.worknest.features.company.exception;

import com.worknest.common.exception.ResourceNotFoundException;
import java.util.UUID;

public class CompanySiteParentCompanyNotFoundException extends ResourceNotFoundException {

    public CompanySiteParentCompanyNotFoundException(UUID companyId) {
        super("COMPANY_NOT_FOUND", "Company '" + companyId + "' was not found");
    }
}
