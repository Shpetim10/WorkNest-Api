package com.worknest.features.company.application.export;

public record CompanyDataExportFile(
        byte[] content,
        String fileName
) {
}
