package com.worknest.features.superAdmin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.worknest.audit.domain.PlatformEvent;
import com.worknest.domain.entities.Company;
import com.worknest.domain.enums.CompanyStatus;
import com.worknest.domain.enums.SubscriptionPlan;
import com.worknest.domain.enums.SubscriptionStatus;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.superAdmin.dto.SuperAdminDashboardResponse;
import com.worknest.security.SuperAdminSecurity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SuperAdminDashboardServiceImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SuperAdminSecurity superAdminSecurity;

    @Mock
    private TypedQuery<Long> longQuery;

    @Mock
    private TypedQuery<Company> companyQuery;

    @Mock
    private TypedQuery<PlatformEvent> platformEventQuery;

    private SuperAdminDashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SuperAdminDashboardServiceImpl(entityManager, userRepository, superAdminSecurity);

        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
        when(longQuery.setParameter(anyString(), any())).thenReturn(longQuery);
        when(longQuery.getSingleResult()).thenReturn(3L, 2L, 1L, 0L);

        when(entityManager.createQuery(anyString(), eq(Company.class))).thenReturn(companyQuery);

        when(entityManager.createQuery(anyString(), eq(PlatformEvent.class))).thenReturn(platformEventQuery);
        when(platformEventQuery.setMaxResults(anyInt())).thenReturn(platformEventQuery);
        when(platformEventQuery.getResultList()).thenReturn(List.of());

        when(userRepository.findAllById(any())).thenReturn(List.of());
        when(superAdminSecurity.currentPrincipal()).thenReturn(Optional.empty());
    }

    @Test
    void existingPeriodFilterStillControlsSuperAdminPlansAndQuickStats() {
        Instant now = Instant.now();
        int currentYear = ZonedDateTime.ofInstant(now, ZoneOffset.UTC).getYear();
        when(companyQuery.getResultList()).thenReturn(List.of(
                company(CompanyStatus.ACTIVE, SubscriptionPlan.BASIC, SubscriptionStatus.TRIAL, now.minus(1, ChronoUnit.SECONDS)),
                company(CompanyStatus.SUSPENDED, SubscriptionPlan.PREMIUM, SubscriptionStatus.ACTIVE, now.minus(370, ChronoUnit.DAYS))
        ));

        SuperAdminDashboardResponse response = service.getDashboard(currentYear, "this-year", null, null, null);

        assertThat(plan(response, SubscriptionPlan.BASIC).companyCount()).isEqualTo(1);
        assertThat(plan(response, SubscriptionPlan.PREMIUM).companyCount()).isZero();
        assertThat(quickStat(response, "active").percentage()).isEqualTo(100.0);
        assertThat(quickStat(response, "suspended").percentage()).isZero();
    }

    @Test
    void validCustomDateRangeFiltersSuperAdminPlansAndQuickStats() {
        when(companyQuery.getResultList()).thenReturn(List.of(
                company(CompanyStatus.ACTIVE, SubscriptionPlan.BASIC, SubscriptionStatus.TRIAL, Instant.parse("2026-05-10T09:00:00Z")),
                company(CompanyStatus.SUSPENDED, SubscriptionPlan.PREMIUM, SubscriptionStatus.ACTIVE, Instant.parse("2026-05-31T23:59:59Z")),
                company(CompanyStatus.ACTIVE, SubscriptionPlan.PREMIUM, SubscriptionStatus.TRIAL, Instant.parse("2026-04-30T23:59:59Z"))
        ));

        SuperAdminDashboardResponse response = service.getDashboard(2026, null, "2026-05-01", "2026-05-31", null);

        assertThat(plan(response, SubscriptionPlan.BASIC).companyCount()).isEqualTo(1);
        assertThat(plan(response, SubscriptionPlan.PREMIUM).companyCount()).isEqualTo(1);
        assertThat(quickStat(response, "active").percentage()).isEqualTo(50.0);
        assertThat(quickStat(response, "trial").percentage()).isEqualTo(50.0);
        assertThat(quickStat(response, "suspended").percentage()).isEqualTo(50.0);
    }

    @Test
    void customDateRangeOverridesPeriodForSuperAdminFilteredSections() {
        when(companyQuery.getResultList()).thenReturn(List.of(
                company(CompanyStatus.ACTIVE, SubscriptionPlan.BASIC, SubscriptionStatus.TRIAL, Instant.parse("2026-05-15T09:00:00Z")),
                company(CompanyStatus.SUSPENDED, SubscriptionPlan.PREMIUM, SubscriptionStatus.ACTIVE, Instant.parse("2026-01-15T09:00:00Z"))
        ));

        SuperAdminDashboardResponse response = service.getDashboard(2026, "this-year", "2026-05-01", "2026-05-31", null);

        assertThat(plan(response, SubscriptionPlan.BASIC).companyCount()).isEqualTo(1);
        assertThat(plan(response, SubscriptionPlan.PREMIUM).companyCount()).isZero();
        assertThat(quickStat(response, "active").percentage()).isEqualTo(100.0);
        assertThat(quickStat(response, "suspended").percentage()).isZero();
    }

    @Test
    void sectionCanLimitCustomFilteringToSubscriptionPlans() {
        when(companyQuery.getResultList()).thenReturn(List.of(
                company(CompanyStatus.ACTIVE, SubscriptionPlan.BASIC, SubscriptionStatus.TRIAL, Instant.parse("2026-05-15T09:00:00Z")),
                company(CompanyStatus.SUSPENDED, SubscriptionPlan.PREMIUM, SubscriptionStatus.ACTIVE, Instant.parse("2026-01-15T09:00:00Z"))
        ));

        SuperAdminDashboardResponse response = service.getDashboard(
                2026,
                "this-year",
                "2026-05-01",
                "2026-05-31",
                "subscriptionPlans"
        );

        assertThat(plan(response, SubscriptionPlan.BASIC).companyCount()).isEqualTo(1);
        assertThat(plan(response, SubscriptionPlan.PREMIUM).companyCount()).isZero();
        assertThat(quickStat(response, "active").percentage()).isEqualTo(50.0);
        assertThat(quickStat(response, "suspended").percentage()).isEqualTo(50.0);
    }

    private SuperAdminDashboardResponse.SubscriptionPlanBreakdown plan(
            SuperAdminDashboardResponse response,
            SubscriptionPlan plan
    ) {
        return response.subscriptionPlans().stream()
                .filter(item -> item.planId().equals(plan.name().toLowerCase()))
                .findFirst()
                .orElseThrow();
    }

    private SuperAdminDashboardResponse.QuickStat quickStat(SuperAdminDashboardResponse response, String id) {
        return response.quickStats().stream()
                .filter(stat -> stat.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private Company company(
            CompanyStatus status,
            SubscriptionPlan plan,
            SubscriptionStatus subscriptionStatus,
            Instant createdAt
    ) {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setName("Acme Corporation");
        company.setSlug("acme-" + company.getId());
        company.setStatus(status);
        company.setEmail(company.getId() + "@acme.test");
        company.setPhoneNumber("+355690000000");
        company.setCountryCode("AL");
        company.setSubscriptionPlan(plan);
        company.setSubscriptionStatus(subscriptionStatus);
        company.setCreatedAt(createdAt);
        company.setUpdatedAt(createdAt);
        return company;
    }
}
