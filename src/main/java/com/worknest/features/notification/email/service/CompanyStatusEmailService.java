package com.worknest.features.notification.email.service;

import com.worknest.domain.entities.Company;

public interface CompanyStatusEmailService {

    void sendCompanySuspendedEmail(Company company, String reason);

    void sendCompanyUnsuspendedEmail(Company company);
}
