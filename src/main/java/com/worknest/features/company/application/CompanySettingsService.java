package com.worknest.features.company.application;

import com.worknest.features.company.dto.CompanySettingsResponse;
import com.worknest.features.company.dto.CurrencyExchangeRequest;
import com.worknest.features.company.dto.UpdateCompanySettingsRequest;
import java.util.UUID;

public interface CompanySettingsService {

    CompanySettingsResponse getSettings(UUID companyId);

    CompanySettingsResponse updateSettings(UUID companyId, UpdateCompanySettingsRequest request);

    CompanySettingsResponse updateCurrency(UUID companyId, CurrencyExchangeRequest request);
}
