package com.worknest.features.company.application;

import com.worknest.audit.domain.AuditLog;
import com.worknest.audit.service.AuditLogService;
import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.Company;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.features.company.application.export.CompanyDataExportDataProvider;
import com.worknest.features.company.application.export.CompanyDataExportFile;
import com.worknest.features.company.application.export.ExportLocalizationService;
import com.worknest.features.company.application.export.ExportWorkbookData;
import com.worknest.features.company.application.export.ZipExportWriter;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.security.AuthSessionPrincipal;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CompanyDataExportServiceImpl implements CompanyDataExportService {

    private final CompanyRepository companyRepository;
    private final CompanyDataExportDataProvider dataProvider;
    private final ZipExportWriter zipExportWriter;
    private final ExportLocalizationService localization;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public CompanyDataExportFile exportCompanyData(String locale, String acceptLanguage) {
        AuthSessionPrincipal principal = principal();
        if (principal.companyId() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "COMPANY_CONTEXT_MISSING",
                    "Company context is required to export company data."
            );
        }
        if (principal.role() != PlatformRole.ADMIN) {
            throw new AccessDeniedException("Only company admins can export company data.");
        }

        Company company = companyRepository.findById(principal.companyId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "COMPANY_NOT_FOUND",
                        "Company not found."
                ));

        String resolvedLocale = localization.resolveLocale(locale, acceptLanguage);
        List<ExportWorkbookData> workbooks = dataProvider.loadCompanyData(company.getId(), resolvedLocale);
        byte[] zipBytes = writeZip(workbooks);

        logExport(principal, company, resolvedLocale, workbooks);

        return new CompanyDataExportFile(zipBytes, fileName(company));
    }

    private byte[] writeZip(List<ExportWorkbookData> workbooks) {
        try {
            return zipExportWriter.write(workbooks);
        } catch (IOException exception) {
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "EXPORT_GENERATION_FAILED",
                    "Company data export generation failed."
            );
        }
    }

    private void logExport(
            AuthSessionPrincipal principal,
            Company company,
            String locale,
            List<ExportWorkbookData> workbooks
    ) {
        auditLogService.logAction(new AuditLog(
                company.getId(),
                principal.userId(),
                principal.roleAssignmentId(),
                principal.role(),
                null,
                "COMPANY_DATA_EXPORTED",
                "Company",
                company.getId(),
                Map.of(),
                Map.of(
                        "locale", locale,
                        "fileCount", workbooks.size()
                ),
                null
        ));
    }

    private AuthSessionPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthSessionPrincipal principal)) {
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "UNAUTHENTICATED",
                    "Authentication is required."
            );
        }
        return principal;
    }

    private String fileName(Company company) {
        String scope = StringUtils.hasText(company.getName()) ? company.getName() : company.getId().toString();
        return "company-data-" + safeFileNamePart(scope) + "-" + LocalDate.now(ZoneOffset.UTC) + ".zip";
    }

    private String safeFileNamePart(String value) {
        String safe = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return StringUtils.hasText(safe) ? safe : "company";
    }
}
