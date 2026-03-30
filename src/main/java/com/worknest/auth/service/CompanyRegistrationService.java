package com.worknest.auth.service;

import com.worknest.auth.dto.CompanyRegistrationRequest;
import com.worknest.auth.dto.CompanyRegistrationResponse;

public interface CompanyRegistrationService {

    CompanyRegistrationResponse registerCompany(CompanyRegistrationRequest request);
}
