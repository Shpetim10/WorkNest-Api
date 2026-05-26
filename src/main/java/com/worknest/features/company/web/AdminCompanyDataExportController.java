package com.worknest.features.company.web;

import com.worknest.common.api.ApiErrorResponse;
import com.worknest.features.company.application.CompanyDataExportService;
import com.worknest.features.company.application.export.CompanyDataExportFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping({
        "/api/v1/admin/company-data",
        "/api/v1/companies/{companyId}/company-data"
})
@Tag(name = "Admin Company Data Export", description = "Binary company data export for Admin users.")
@SecurityRequirement(name = "bearerAuth")
public class AdminCompanyDataExportController {

    private static final MediaType APPLICATION_ZIP = MediaType.parseMediaType("application/zip");

    private final CompanyDataExportService companyDataExportService;

    @GetMapping(value = "/export", produces = "application/zip")
    @PreAuthorize("@teamSecurity.hasCurrentCompanyPermission('COMPANY_DATA_EXPORT')")
    @Operation(summary = "Download all company dashboard table data as a ZIP of Excel workbooks")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Company data ZIP generated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Company context missing", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Company not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Export generation failed", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<byte[]> export(
            @PathVariable(required = false) UUID companyId,
            @RequestParam(name = "companyId", required = false) UUID queryCompanyId,
            @RequestParam(required = false) String locale,
            @RequestHeader(value = "X-Company-ID", required = false) UUID headerCompanyId,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage
    ) {
        CompanyDataExportFile file = companyDataExportService.exportCompanyData(
                resolveCompanyId(companyId, queryCompanyId, headerCompanyId),
                locale,
                acceptLanguage
        );
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(APPLICATION_ZIP)
                .contentLength(file.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(file.content());
    }

    private UUID resolveCompanyId(UUID pathCompanyId, UUID queryCompanyId, UUID headerCompanyId) {
        UUID resolved = pathCompanyId != null ? pathCompanyId : queryCompanyId;
        if (resolved == null) {
            return headerCompanyId;
        }
        if (queryCompanyId != null && !resolved.equals(queryCompanyId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conflicting company identifiers.");
        }
        if (headerCompanyId != null && !resolved.equals(headerCompanyId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conflicting company identifiers.");
        }
        return resolved;
    }
}
