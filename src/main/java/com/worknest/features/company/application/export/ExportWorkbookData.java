package com.worknest.features.company.application.export;

import java.util.List;

public record ExportWorkbookData(
        String path,
        String sheetName,
        List<String> headers,
        List<List<Object>> rows
) {
}
