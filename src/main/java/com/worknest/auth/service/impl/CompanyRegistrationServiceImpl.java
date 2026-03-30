package com.worknest.auth.service.impl;

import com.worknest.auth.dto.CompanyRegistrationRequest;
import com.worknest.auth.dto.CompanyRegistrationResponse;
import com.worknest.auth.service.CompanyRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyRegistrationServiceImpl implements CompanyRegistrationService {

    @Override
    @Transactional
    public CompanyRegistrationResponse registerCompany(CompanyRegistrationRequest request) {
        log.info("Registering company with slug: {}", request.companySlug());
        
        // TODO: Validate company slug uniqueness
        // TODO: Create company entity
        // TODO: Create admin user entity
        // TODO: Assign PLATFORM_ADMIN role
        // TODO: Publish CompanyRegisteredEvent
        
        return new CompanyRegistrationResponse(null, null, request.companyName(), request.companySlug(), null);
    }
}
