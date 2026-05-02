package com.worknest.features.company.application;

import com.worknest.features.company.dto.CompanySettingsResponse;
import java.util.UUID;

public interface CompanySettingsService {

    CompanySettingsResponse getSettings(UUID companyId);
}
