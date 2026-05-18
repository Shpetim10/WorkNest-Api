package com.worknest.features.company.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.worknest.features.company.application.CompanyDataExportService;
import com.worknest.features.company.application.export.CompanyDataExportFile;
import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

class AdminCompanyDataExportControllerTest {

    @Test
    void exportReturnsZipHeadersForFrontendBlobDownload() {
        CompanyDataExportService service = (companyId, locale, acceptLanguage) ->
                new CompanyDataExportFile(new byte[]{1, 2, 3}, "company-data-acme-2026-05-17.zip");
        AdminCompanyDataExportController controller = new AdminCompanyDataExportController(service);

        var response = controller.export(null, null, "en", null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/zip");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment")
                .contains("company-data-acme-2026-05-17.zip");
        assertThat(response.getBody()).containsExactly(1, 2, 3);
    }

    @Test
    void exportEndpointIsAuthenticatedAndMappedToExpectedPath() throws NoSuchMethodException {
        Method method = AdminCompanyDataExportController.class.getMethod(
                "export",
                UUID.class,
                UUID.class,
                String.class,
                UUID.class,
                String.class
        );

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("isAuthenticated()");
        assertThat(getMapping.value()).containsExactly("/export");
        assertThat(getMapping.produces()).containsExactly("application/zip");
    }

    @Test
    void exportPassesSelectedCompanyIdFromHeaderToService() {
        UUID selectedCompanyId = UUID.randomUUID();
        final UUID[] capturedCompanyId = new UUID[1];
        CompanyDataExportService service = (companyId, locale, acceptLanguage) -> {
            capturedCompanyId[0] = companyId;
            return new CompanyDataExportFile(new byte[]{1}, "company-data-acme-2026-05-17.zip");
        };
        AdminCompanyDataExportController controller = new AdminCompanyDataExportController(service);

        controller.export(null, null, "en", selectedCompanyId, null);

        assertThat(capturedCompanyId[0]).isEqualTo(selectedCompanyId);
    }
}
