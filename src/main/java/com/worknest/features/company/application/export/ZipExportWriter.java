package com.worknest.features.company.application.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ZipExportWriter {

    private final ExcelExportWriter excelExportWriter;

    public byte[] write(List<ExportWorkbookData> workbooks) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            for (ExportWorkbookData workbook : workbooks) {
                zipOutputStream.putNextEntry(new ZipEntry(workbook.path()));
                zipOutputStream.write(excelExportWriter.write(workbook));
                zipOutputStream.closeEntry();
            }
            zipOutputStream.finish();
            return outputStream.toByteArray();
        }
    }
}
