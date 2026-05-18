package com.worknest.features.dashboard.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.worknest.domain.entities.AttendanceDayRecord;
import com.worknest.domain.entities.LeaveRequest;
import com.worknest.domain.enums.EmploymentStatus;
import com.worknest.domain.enums.PlatformAccess;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.dashboard.dto.AdminDashboardResponse;
import com.worknest.features.employee.repository.EmployeeRepository;
import com.worknest.security.AuthSessionPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TypedQuery<Long> longQuery;

    @Mock
    private TypedQuery<AttendanceDayRecord> attendanceQuery;

    @Mock
    private TypedQuery<LeaveRequest> leaveRequestQuery;

    private AdminDashboardServiceImpl service;
    private UUID companyId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new AdminDashboardServiceImpl(entityManager, employeeRepository, userRepository);
        companyId = UUID.randomUUID();
        userId = UUID.randomUUID();
        authenticate();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(employeeRepository.countByCompanyIdAndEmploymentStatus(companyId, EmploymentStatus.ACTIVE)).thenReturn(4L);

        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
        when(longQuery.setParameter(anyString(), any())).thenReturn(longQuery);
        when(longQuery.getSingleResult()).thenReturn(0L, 0L, 0L, 2L, 1L);

        when(entityManager.createQuery(anyString(), eq(AttendanceDayRecord.class))).thenReturn(attendanceQuery);
        when(attendanceQuery.setParameter(anyString(), any())).thenReturn(attendanceQuery);
        when(attendanceQuery.getResultList()).thenReturn(List.of());

        when(entityManager.createQuery(anyString(), eq(LeaveRequest.class))).thenReturn(leaveRequestQuery);
        when(leaveRequestQuery.setParameter(anyString(), any())).thenReturn(leaveRequestQuery);
        when(leaveRequestQuery.setMaxResults(anyInt())).thenReturn(leaveRequestQuery);
        when(leaveRequestQuery.getResultList()).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void existingPeriodFilterStillControlsAdminQuickStats() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        YearMonth lastMonth = YearMonth.from(today).minusMonths(1);

        AdminDashboardResponse response = service.getDashboard("last-month", null, null, null);

        assertThat(quickStat(response, "attendance-rate").percentage()).isEqualTo(50.0);
        assertThat(quickStat(response, "leave-utilization").percentage()).isEqualTo(25.0);
        verify(longQuery, times(2)).setParameter("from", lastMonth.atDay(1));
        verify(longQuery, times(2)).setParameter("to", lastMonth.atEndOfMonth());
    }

    @Test
    void customDateRangeOverridesPeriodForAdminQuickStats() {
        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 10);

        AdminDashboardResponse response = service.getDashboard("this-year", "this-week", "2026-05-01", "2026-05-10");

        assertThat(quickStat(response, "attendance-rate").valueLabel()).isEqualTo("50%");
        assertThat(quickStat(response, "leave-utilization").valueLabel()).isEqualTo("25%");
        verify(longQuery, times(2)).setParameter("from", start);
        verify(longQuery, times(2)).setParameter("to", end);
    }

    private AdminDashboardResponse.QuickStat quickStat(AdminDashboardResponse response, String id) {
        return response.quickStats().stream()
                .filter(stat -> stat.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private void authenticate() {
        AuthSessionPrincipal principal = new AuthSessionPrincipal(
                userId,
                "admin@acme.test",
                companyId,
                "acme",
                UUID.randomUUID(),
                PlatformRole.ADMIN,
                PlatformAccess.WEB
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }
}
