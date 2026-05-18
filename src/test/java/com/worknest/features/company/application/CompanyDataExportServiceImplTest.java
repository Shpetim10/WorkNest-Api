package com.worknest.features.company.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.worknest.audit.domain.AuditLog;
import com.worknest.audit.service.AuditLogService;
import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.RoleAssignment;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.CompanyStatus;
import com.worknest.domain.enums.PlatformAccess;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.features.auth.repository.RoleAssignmentRepository;
import com.worknest.features.company.application.export.CompanyDataExportDataProvider;
import com.worknest.features.company.application.export.CompanyDataExportFile;
import com.worknest.features.company.application.export.ExcelExportWriter;
import com.worknest.features.company.application.export.ExportLocalizationService;
import com.worknest.features.company.application.export.ExportWorkbookData;
import com.worknest.features.company.application.export.ZipExportWriter;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.security.AuthSessionPrincipal;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipInputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CompanyDataExportServiceImplTest {

    private static final List<String> EXPECTED_PATHS = List.of(
            "employees/employee-list.xlsx",
            "employees/staff-list.xlsx",
            "employees/assign-employees.xlsx",
            "attendance/attendance.xlsx",
            "leave/leave.xlsx",
            "payroll/payroll.xlsx",
            "locations/locations.xlsx",
            "departments/departments.xlsx",
            "announcements/announcements.xlsx",
            "audit-log/audit-log.xlsx"
    );

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private RoleAssignmentRepository roleAssignmentRepository;

    @Mock
    private CompanyDataExportDataProvider dataProvider;

    @Mock
    private AuditLogService auditLogService;

    private ExportLocalizationService localization;
    private CompanyDataExportServiceImpl service;
    private UUID companyId;
    private UUID userId;
    private UUID roleAssignmentId;

    @BeforeEach
    void setUp() {
        localization = new ExportLocalizationService();
        service = new CompanyDataExportServiceImpl(
                companyRepository,
                roleAssignmentRepository,
                dataProvider,
                new ZipExportWriter(new ExcelExportWriter()),
                localization,
                auditLogService
        );
        companyId = UUID.randomUUID();
        userId = UUID.randomUUID();
        roleAssignmentId = UUID.randomUUID();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminReceivesZipWithAllExpectedXlsxPathsAndNoReports() throws IOException {
        authenticate(PlatformRole.ADMIN, companyId);
        allowExportAs(PlatformRole.ADMIN, companyId);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company()));
        when(dataProvider.loadCompanyData(eq(companyId), eq("en"))).thenReturn(workbooks("en"));

        CompanyDataExportFile file = service.exportCompanyData("en", null);

        Map<String, byte[]> entries = unzip(file.content());
        assertThat(file.fileName()).startsWith("company-data-acme-corporation-").endsWith(".zip");
        assertThat(entries.keySet()).containsExactlyInAnyOrderElementsOf(EXPECTED_PATHS);
        assertThat(entries.keySet()).noneMatch(path -> path.toLowerCase().contains("report"));
        verify(dataProvider).loadCompanyData(companyId, "en");
    }

    @Test
    void eachWorkbookHasExpectedHeadersFreezePaneAndAutofilter() throws IOException {
        authenticate(PlatformRole.ADMIN, companyId);
        allowExportAs(PlatformRole.ADMIN, companyId);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company()));
        when(dataProvider.loadCompanyData(eq(companyId), eq("en"))).thenReturn(workbooks("en"));

        CompanyDataExportFile file = service.exportCompanyData("en", null);
        byte[] employeeWorkbook = unzip(file.content()).get("employees/employee-list.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(employeeWorkbook))) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("Employee List");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Name");
            assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Role");
            assertThat(sheet.getPaneInformation()).isNotNull();
            assertThat(sheet.getPaneInformation().isFreezePane()).isTrue();
            assertThat(sheet.getCTWorksheet().isSetAutoFilter()).isTrue();
        }
    }

    @Test
    void zipWritesRowsIntoEveryWorkbookEntry() throws IOException {
        authenticate(PlatformRole.ADMIN, companyId);
        allowExportAs(PlatformRole.ADMIN, companyId);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company()));
        when(dataProvider.loadCompanyData(eq(companyId), eq("en"))).thenReturn(workbooks("en"));

        CompanyDataExportFile file = service.exportCompanyData("en", null);
        Map<String, byte[]> entries = unzip(file.content());

        for (String path : EXPECTED_PATHS) {
            try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(entries.get(path)))) {
                var sheet = workbook.getSheetAt(0);
                assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Sample");
            }
        }
    }

    @Test
    void localeChangesSheetNamesAndHeaders() throws IOException {
        authenticate(PlatformRole.ADMIN, companyId);
        allowExportAs(PlatformRole.ADMIN, companyId);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company()));
        when(dataProvider.loadCompanyData(eq(companyId), eq("sq"))).thenReturn(workbooks("sq"));

        CompanyDataExportFile file = service.exportCompanyData("sq", null);
        byte[] employeeWorkbook = unzip(file.content()).get("employees/employee-list.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(employeeWorkbook))) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("Lista e punonj\u00EBsve");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Emri");
            assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Roli");
        }
    }

    @Test
    void acceptLanguageIsUsedWhenLocaleQueryParamIsMissing() {
        authenticate(PlatformRole.ADMIN, companyId);
        allowExportAs(PlatformRole.ADMIN, companyId);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company()));
        when(dataProvider.loadCompanyData(eq(companyId), eq("de"))).thenReturn(workbooks("de"));

        service.exportCompanyData(null, "de-DE,de;q=0.9,en;q=0.5");

        verify(dataProvider).loadCompanyData(companyId, "de");
    }

    @Test
    void nonAdminReceivesForbiddenBeforeExportGeneration() {
        authenticate(PlatformRole.STAFF, companyId);
        allowExportAs(PlatformRole.STAFF, companyId);

        assertThrows(AccessDeniedException.class, () -> service.exportCompanyData("en", null));

        verifyNoInteractions(dataProvider, auditLogService);
        verify(companyRepository, never()).findById(companyId);
    }

    @Test
    void missingCompanyContextReceivesBadRequest() {
        authenticate(PlatformRole.ADMIN, null);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.exportCompanyData("en", null));

        assertThat(exception.getCode()).isEqualTo("COMPANY_CONTEXT_MISSING");
        verifyNoInteractions(roleAssignmentRepository, dataProvider, auditLogService);
    }

    @Test
    void exportWritesAuditLog() {
        authenticate(PlatformRole.ADMIN, companyId);
        allowExportAs(PlatformRole.ADMIN, companyId);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company()));
        when(dataProvider.loadCompanyData(eq(companyId), eq("en"))).thenReturn(workbooks("en"));

        service.exportCompanyData("en", null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).logAction(captor.capture());
        assertThat(captor.getValue().getCompanyId()).isEqualTo(companyId);
        assertThat(captor.getValue().getActorUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getActorRoleAssignmentId()).isEqualTo(roleAssignmentId);
        assertThat(captor.getValue().getAction()).isEqualTo("COMPANY_DATA_EXPORTED");
        assertThat(captor.getValue().getMetadata()).containsEntry("fileCount", EXPECTED_PATHS.size());
        assertThat(captor.getValue().getMetadata()).containsKey("rowCounts");
    }

    @Test
    void requestedCompanyIdIsUsedWhenFrontendPassesSelectedCompany() {
        UUID selectedCompanyId = UUID.randomUUID();
        authenticate(PlatformRole.ADMIN, companyId);
        allowExportAs(PlatformRole.ADMIN, selectedCompanyId);
        when(companyRepository.findById(selectedCompanyId)).thenReturn(Optional.of(company(selectedCompanyId)));
        when(dataProvider.loadCompanyData(eq(selectedCompanyId), eq("en"))).thenReturn(workbooks("en"));

        service.exportCompanyData(selectedCompanyId, "en", null);

        verify(dataProvider).loadCompanyData(selectedCompanyId, "en");
        verify(companyRepository, never()).findById(companyId);
    }

    private void authenticate(PlatformRole role, UUID principalCompanyId) {
        AuthSessionPrincipal principal = new AuthSessionPrincipal(
                userId,
                "admin@acme.test",
                principalCompanyId,
                "acme",
                roleAssignmentId,
                role,
                PlatformAccess.WEB
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }

    private void allowExportAs(PlatformRole role, UUID exportCompanyId) {
        when(roleAssignmentRepository.findFirstByUserIdAndCompanyIdAndIsActiveTrue(userId, exportCompanyId))
                .thenReturn(Optional.of(roleAssignment(role, exportCompanyId)));
    }

    private Company company() {
        return company(companyId);
    }

    private Company company(UUID id) {
        Company company = new Company();
        company.setId(id);
        company.setName("Acme Corporation");
        company.setSlug("acme");
        company.setStatus(CompanyStatus.ACTIVE);
        company.setEmail("info@acme.test");
        company.setPhoneNumber("+355690000000");
        company.setCountryCode("AL");
        company.setCreatedAt(Instant.parse("2026-05-01T10:15:30Z"));
        company.setUpdatedAt(Instant.parse("2026-05-01T10:15:30Z"));
        return company;
    }

    private RoleAssignment roleAssignment(PlatformRole role, UUID assignmentCompanyId) {
        User user = new User();
        user.setId(userId);

        RoleAssignment assignment = new RoleAssignment();
        assignment.setId(roleAssignmentId);
        assignment.setUser(user);
        assignment.setCompany(company(assignmentCompanyId));
        assignment.setRole(role);
        assignment.setIsActive(true);
        assignment.setJobTitle(role == PlatformRole.STAFF ? "Manager" : "Admin");
        assignment.setPlatformAccess(PlatformAccess.WEB);
        assignment.setActivatedAt(Instant.parse("2026-05-01T10:15:30Z"));
        return assignment;
    }

    private List<ExportWorkbookData> workbooks(String locale) {
        return List.of(
                workbook("employees/employee-list.xlsx", "employeeList", locale, List.of("name", "role")),
                workbook("employees/staff-list.xlsx", "staffList", locale, List.of("name", "email")),
                workbook("employees/assign-employees.xlsx", "assignEmployees", locale, List.of("name", "assignedEmployees")),
                workbook("attendance/attendance.xlsx", "attendance", locale, List.of("name", "status")),
                workbook("leave/leave.xlsx", "leaveRequests", locale, List.of("name", "status")),
                workbook("payroll/payroll.xlsx", "payroll", locale, List.of("employeeName", "payment")),
                workbook("locations/locations.xlsx", "locations", locale, List.of("siteName", "status")),
                workbook("departments/departments.xlsx", "departments", locale, List.of("name", "status")),
                workbook("announcements/announcements.xlsx", "announcements", locale, List.of("title", "priority")),
                workbook("audit-log/audit-log.xlsx", "auditLog", locale, List.of("user", "action"))
        );
    }

    private ExportWorkbookData workbook(String path, String sheetKey, String locale, List<String> headerKeys) {
        List<String> headers = headerKeys.stream()
                .map(key -> localization.header(locale, key))
                .toList();
        return new ExportWorkbookData(
                path,
                localization.sheet(locale, sheetKey),
                headers,
                List.of(List.of("Sample", "Value"))
        );
    }

    private Map<String, byte[]> unzip(byte[] content) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(content))) {
            var entry = zipInputStream.getNextEntry();
            while (entry != null) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                zipInputStream.transferTo(outputStream);
                entries.put(entry.getName(), outputStream.toByteArray());
                zipInputStream.closeEntry();
                entry = zipInputStream.getNextEntry();
            }
        }
        return entries;
    }
}
