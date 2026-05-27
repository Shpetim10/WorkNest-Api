package com.worknest.features.company.application;

import com.worknest.audit.domain.AuditLog;
import com.worknest.audit.service.AuditLogService;
import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.RoleAssignment;
import com.worknest.domain.enums.PermissionCode;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.features.auth.repository.RoleAssignmentPermissionRepository;
import com.worknest.features.company.application.export.CompanyDataExportDataProvider;
import com.worknest.features.company.application.export.CompanyDataExportFile;
import com.worknest.features.company.application.export.ExportLocalizationService;
import com.worknest.features.company.application.export.ExportWorkbookData;
import com.worknest.features.company.application.export.ZipExportWriter;
import com.worknest.features.auth.repository.RoleAssignmentRepository;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.security.AuthSessionPrincipal;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class CompanyDataExportServiceImpl implements CompanyDataExportService {

    private final CompanyRepository companyRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final RoleAssignmentPermissionRepository roleAssignmentPermissionRepository;
    private final CompanyDataExportDataProvider dataProvider;
    private final ZipExportWriter zipExportWriter;
    private final ExportLocalizationService localization;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public CompanyDataExportFile exportCompanyData(String locale, String acceptLanguage) {
        return exportCompanyData(null, locale, acceptLanguage);
    }

    @Override
    @Transactional
    public CompanyDataExportFile exportCompanyData(UUID requestedCompanyId, String locale, String acceptLanguage) {
        AuthSessionPrincipal principal = principal();
        UUID companyId = requestedCompanyId != null ? requestedCompanyId : principal.companyId();
        if (companyId == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "COMPANY_CONTEXT_MISSING",
                    "Company context is required to export company data."
            );
        }

        RoleAssignment actorAssignment = roleAssignmentRepository
                .findFirstByUserIdAndCompanyIdAndIsActiveTrue(principal.userId(), companyId)
                .orElseThrow(() -> new AccessDeniedException("Only company admins can export company data."));
        if (actorAssignment.getRole() != PlatformRole.ADMIN
                && actorAssignment.getRole() != PlatformRole.SUPERADMIN
                && !hasPermission(actorAssignment, PermissionCode.COMPANY_DATA_EXPORT)) {
            throw new AccessDeniedException("Only company admins can export company data.");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "COMPANY_NOT_FOUND",
                        "Company not found."
                ));

        String resolvedLocale = localization.resolveLocale(locale, acceptLanguage);
        List<ExportWorkbookData> workbooks = dataProvider.loadCompanyData(company.getId(), resolvedLocale);
        Map<String, Integer> rowCounts = rowCounts(workbooks);
        log.info(
                "Company data export generated: companyId={}, companySlug={}, actorUserId={}, files={}, rowCounts={}",
                company.getId(),
                company.getSlug(),
                principal.userId(),
                workbooks.size(),
                rowCounts
        );
        byte[] zipBytes = writeZip(workbooks);

        logExport(principal, actorAssignment, company, resolvedLocale, workbooks, rowCounts);

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

    private boolean hasPermission(RoleAssignment assignment, PermissionCode permissionCode) {
        if (assignment.getRole() != PlatformRole.STAFF) {
            return false;
        }
        return roleAssignmentPermissionRepository
                .findByRoleAssignmentIdAndPermissionCode(assignment.getId(), permissionCode.code())
                .map(grant -> Boolean.TRUE.equals(grant.getIsGranted()))
                .orElse(false);
    }

    private void logExport(
            AuthSessionPrincipal principal,
            RoleAssignment actorAssignment,
            Company company,
            String locale,
            List<ExportWorkbookData> workbooks,
            Map<String, Integer> rowCounts
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("locale", locale);
        metadata.put("fileCount", workbooks.size());
        metadata.put("rowCounts", rowCounts);

        auditLogService.logAction(new AuditLog(
                company.getId(),
                principal.userId(),
                actorAssignment.getId(),
                actorAssignment.getRole(),
                actorAssignment.getJobTitle(),
                "COMPANY_DATA_EXPORTED",
                "Company",
                company.getId(),
                Map.of(),
                metadata,
                null
        ));
    }

    private Map<String, Integer> rowCounts(List<ExportWorkbookData> workbooks) {
        Map<String, Integer> rowCounts = new LinkedHashMap<>();
        for (ExportWorkbookData workbook : workbooks) {
            rowCounts.put(workbook.path(), workbook.rows().size());
        }
        return rowCounts;
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
