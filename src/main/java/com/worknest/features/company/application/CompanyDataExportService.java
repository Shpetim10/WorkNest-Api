package com.worknest.features.company.application;

import com.worknest.features.company.application.export.CompanyDataExportFile;

public interface CompanyDataExportService {

    CompanyDataExportFile exportCompanyData(String locale, String acceptLanguage);
}
