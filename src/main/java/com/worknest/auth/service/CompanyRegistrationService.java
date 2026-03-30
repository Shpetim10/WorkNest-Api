package com.worknest.auth.service;

import com.worknest.auth.dto.CompanyRegistrationRequest;
import com.worknest.auth.dto.CompanyRegistrationResponse;

public interface CompanyRegistrationService {

    /**
     * Registers a new company along with its initial admin user.
     *
     * @param request the registration details
     * @return the registration response with company and user IDs
     */
    CompanyRegistrationResponse registerCompany(CompanyRegistrationRequest request);
}
