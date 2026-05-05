package com.worknest.features.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.AttendanceDayRecord;
import com.worknest.domain.entities.AttendanceEvent;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.CompanySite;
import com.worknest.domain.entities.Employee;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.PlatformAccess;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.domain.enums.AttendancePolicySource;
import com.worknest.features.attendance.application.AttendancePolicyResolver;
import com.worknest.features.attendance.application.ResolvedAttendancePolicy;
import com.worknest.features.attendance.application.StaffAttendanceServiceImpl;
import com.worknest.features.attendance.dto.EffectiveAttendancePolicyDto;
import com.worknest.features.attendance.dto.ManualCheckInRequest;
import com.worknest.features.attendance.repository.AttendanceDayRecordRepository;
import com.worknest.features.attendance.repository.AttendanceEventRepository;
import com.worknest.features.attendance.repository.AttendanceReviewActionRepository;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.companySite.repository.CompanySiteRepository;
import com.worknest.features.employee.repository.EmployeeRepository;
import com.worknest.security.AuthSessionPrincipal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StaffAttendanceAuthorizationTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private CompanySiteRepository companySiteRepository;
    @Mock private AttendanceDayRecordRepository attendanceDayRecordRepository;
    @Mock private AttendanceEventRepository attendanceEventRepository;
    @Mock private AttendanceReviewActionRepository attendanceReviewActionRepository;
    @Mock private UserRepository userRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private AttendancePolicyResolver attendancePolicyResolver;

    @InjectMocks private StaffAttendanceServiceImpl service;

    private final UUID companyId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID roleAssignmentId = UUID.randomUUID();
    private static final String SITE_TIMEZONE = "Europe/Tirane";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        EffectiveAttendancePolicyDto permissivePolicy = new EffectiveAttendancePolicyDto(
                null, AttendancePolicySource.COMPANY_DEFAULT,
                true, true, true, true, true, true, false, true, true
        );
        when(attendancePolicyResolver.resolveForSite(any(), any()))
                .thenReturn(new ResolvedAttendancePolicy(null, permissivePolicy));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- helpers ---

    private void setAuthentication(PlatformRole role) {
        AuthSessionPrincipal principal = new AuthSessionPrincipal(
                UUID.randomUUID(), "testuser", companyId, "test-slug", roleAssignmentId, role, PlatformAccess.WEB
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, null)
        );
    }

    private Employee mockEmployee() {
        Company company = mock(Company.class);
        when(company.getId()).thenReturn(companyId);

        CompanySite site = mock(CompanySite.class);
        when(site.getTimezone()).thenReturn(SITE_TIMEZONE);

        User user = mock(User.class);

        Employee employee = mock(Employee.class);
        when(employee.getId()).thenReturn(employeeId);
        when(employee.getCompany()).thenReturn(company);
        when(employee.getCompanySite()).thenReturn(site);
        when(employee.getUser()).thenReturn(user);
        return employee;
    }

    // --- Test 1: STAFF cannot modify attendance for a previous day ---

    @Test
    void staffCannotManualCheckInForPreviousDay() {
        setAuthentication(PlatformRole.STAFF);
        Employee employee = mockEmployee();
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employeeRepository.findAllAssignedToManager(companyId, PlatformRole.EMPLOYEE, roleAssignmentId))
                .thenReturn(List.of(employee));

        // eventAt is 2 days ago — clearly a previous day in any timezone
        Instant twoDaysAgo = Instant.now().minus(2, ChronoUnit.DAYS);
        ManualCheckInRequest request = new ManualCheckInRequest(twoDaysAgo, "reason");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.manualCheckIn(employeeId, request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("PREVIOUS_DAY_NOT_ALLOWED", ex.getCode());
    }

    // --- Test 2: ADMIN can modify attendance for a previous day ---

    @Test
    void adminCanManualCheckInForPreviousDay() {
        setAuthentication(PlatformRole.ADMIN);
        Employee employee = mockEmployee();
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(attendanceDayRecordRepository.findByCompanyIdAndEmployeeIdAndWorkDate(
                any(), any(), any())).thenReturn(Optional.empty());
        when(attendanceEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(attendanceDayRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Instant twoDaysAgo = Instant.now().minus(2, ChronoUnit.DAYS);
        ManualCheckInRequest request = new ManualCheckInRequest(twoDaysAgo, "admin correction");

        // Should not throw
        service.manualCheckIn(employeeId, request);

        verify(attendanceEventRepository).save(any());
        verify(attendanceDayRecordRepository).save(any());
    }

    // --- Test 3: workDate is derived from eventAt in site timezone, not UTC ---

    @Test
    void workDateDerivedFromEventTimestampInSiteTimezone() {
        setAuthentication(PlatformRole.ADMIN);
        Employee employee = mockEmployee();
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(attendanceDayRecordRepository.findByCompanyIdAndEmployeeIdAndWorkDate(
                any(), any(), any())).thenReturn(Optional.empty());
        when(attendanceEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(attendanceDayRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 2026-05-02T00:30:00Z = 2026-05-02T02:30:00+02:00 (Europe/Tirane, CEST)
        // workDate should be 2026-05-02 in site timezone
        Instant eventAt = Instant.parse("2026-05-02T00:30:00Z");
        ManualCheckInRequest request = new ManualCheckInRequest(eventAt, "late night entry");

        service.manualCheckIn(employeeId, request);

        // 2026-05-02T00:30:00Z = 2026-05-02T02:30:00+02:00 in Europe/Tirane → workDate 2026-05-02
        LocalDate expectedWorkDate = LocalDate.of(2026, 5, 2);

        verify(attendanceDayRecordRepository)
                .findByCompanyIdAndEmployeeIdAndWorkDate(companyId, employeeId, expectedWorkDate);
    }

    // --- Test 4: STAFF cannot dismiss warnings for a previous day ---

    @Test
    void staffCannotDismissWarningsForPreviousDay() {
        setAuthentication(PlatformRole.STAFF);

        UUID recordId = UUID.randomUUID();
        AttendanceDayRecord record = mock(AttendanceDayRecord.class);
        Employee employee = mockEmployee();

        when(record.getPayrollLocked()).thenReturn(false);
        when(record.getEmployee()).thenReturn(employee);
        when(record.getTimezone()).thenReturn(SITE_TIMEZONE);
        // workDate is clearly in the past
        when(record.getWorkDate()).thenReturn(LocalDate.now(ZoneId.of(SITE_TIMEZONE)).minusDays(2));
        when(attendanceDayRecordRepository.findByIdAndCompanyId(recordId, companyId))
                .thenReturn(Optional.of(record));
        when(employeeRepository.findAllAssignedToManager(companyId, PlatformRole.EMPLOYEE, roleAssignmentId))
                .thenReturn(List.of(employee));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.dismissWarnings(recordId, new com.worknest.features.attendance.dto.DismissWarningsRequest(null)));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("PREVIOUS_DAY_NOT_ALLOWED", ex.getCode());
    }

    // --- Test 5: adjustDayRecord rejects checkout before checkin ---

    @Test
    void adjustDayRecordRejectsCheckoutBeforeCheckin() {
        setAuthentication(PlatformRole.ADMIN);

        UUID recordId = UUID.randomUUID();
        AttendanceDayRecord record = mock(AttendanceDayRecord.class);
        when(record.getPayrollLocked()).thenReturn(false);
        when(record.getTimezone()).thenReturn(SITE_TIMEZONE);
        when(record.getWorkDate()).thenReturn(LocalDate.now(ZoneId.of(SITE_TIMEZONE)));
        when(attendanceDayRecordRepository.findByIdAndCompanyId(recordId, companyId))
                .thenReturn(Optional.of(record));

        Instant checkIn = Instant.parse("2026-05-02T08:00:00Z");
        Instant checkOut = Instant.parse("2026-05-02T07:00:00Z"); // before checkin
        var request = new com.worknest.features.attendance.dto.AdjustAttendanceDayRecordRequest(
                checkIn, checkOut, null, com.worknest.domain.enums.AttendanceDayStatus.PRESENT, null
        );

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.adjustDayRecord(recordId, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_TIME_RANGE", ex.getCode());
    }

    // --- Test 6: cross-tenant isolation — employee from different company is rejected ---

    @Test
    void manualCheckInRejectsEmployeeFromDifferentCompany() {
        setAuthentication(PlatformRole.ADMIN);

        UUID otherCompanyId = UUID.randomUUID();
        Company otherCompany = mock(Company.class);
        when(otherCompany.getId()).thenReturn(otherCompanyId);

        CompanySite site = mock(CompanySite.class);
        when(site.getTimezone()).thenReturn(SITE_TIMEZONE);

        Employee employee = mock(Employee.class);
        when(employee.getId()).thenReturn(employeeId);
        when(employee.getCompany()).thenReturn(otherCompany);
        when(employee.getCompanySite()).thenReturn(site);
        when(employee.getUser()).thenReturn(mock(User.class));
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        Instant now = Instant.now();
        ManualCheckInRequest request = new ManualCheckInRequest(now, "cross-tenant attempt");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.manualCheckIn(employeeId, request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }
}
