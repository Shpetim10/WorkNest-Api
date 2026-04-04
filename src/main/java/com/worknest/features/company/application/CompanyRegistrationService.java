package com.worknest.features.company.application;

import com.worknest.features.company.dto.CompanyRegistrationRequest;
import com.worknest.features.company.dto.CompanyRegistrationResponse;

public interface CompanyRegistrationService {

    CompanyRegistrationResponse registerCompany(CompanyRegistrationRequest request);
}
