package com.worknest.features.company.application;

import com.worknest.features.company.application.export.CompanyDataExportFile;
import java.util.UUID;

public interface CompanyDataExportService {

    CompanyDataExportFile exportCompanyData(UUID companyId, String locale, String acceptLanguage);

    default CompanyDataExportFile exportCompanyData(String locale, String acceptLanguage) {
        return exportCompanyData(null, locale, acceptLanguage);
    }
}
