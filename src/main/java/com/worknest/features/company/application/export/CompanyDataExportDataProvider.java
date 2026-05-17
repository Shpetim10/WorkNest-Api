package com.worknest.features.company.application.export;

import java.util.List;
import java.util.UUID;

public interface CompanyDataExportDataProvider {

    List<ExportWorkbookData> loadCompanyData(UUID companyId, String locale);
}
