package com.worknest.features.superAdmin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.worknest.audit.domain.PlatformEvent;
import com.worknest.common.api.PaginatedResponse;
import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.Company;
import com.worknest.domain.enums.CompanyStatus;
import com.worknest.domain.enums.SubscriptionPlan;
import com.worknest.domain.enums.SubscriptionStatus;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.notification.email.service.CompanyStatusEmailService;
import com.worknest.features.superAdmin.dto.CompanyRowDto;
import com.worknest.features.superAdmin.dto.ExtendTrialRequest;
import com.worknest.features.superAdmin.dto.ExtendTrialResponse;
import com.worknest.features.superAdmin.dto.SuspendCompanyRequest;
import com.worknest.features.superAdmin.repository.SuperAdminCompanyQueryRepository;
import com.worknest.features.superAdmin.web.SuperAdminCompaniesController;
import com.worknest.security.AuthSessionPrincipal;
import com.worknest.security.SuperAdminSecurity;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import com.worknest.domain.enums.PlatformAccess;
import com.worknest.domain.enums.PlatformRole;

@ExtendWith(MockitoExtension.class)
class SuperAdminCompaniesServiceImplTest {

    @Mock
    private SuperAdminCompanyQueryRepository companyQueryRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private CompanyStatusEmailService companyStatusEmailService;

    @Mock
    private SuperAdminSecurity superAdminSecurity;

    @InjectMocks
    private SuperAdminCompaniesServiceImpl service;

    @Test
    void listCompaniesReturnsPaginationAndFrontendFields() {
        UUID companyId = UUID.randomUUID();
        Company company = company(companyId, CompanyStatus.ACTIVE, SubscriptionPlan.BASIC, SubscriptionStatus.TRIAL);
        LocalDate endDate = LocalDate.now(ZoneOffset.UTC).plusDays(10);
        company.setTrialEndsAt(endDate.atStartOfDay().toInstant(ZoneOffset.UTC));

        PageRequest pageable = PageRequest.of(0, 5);
        when(companyQueryRepository.findCompanies("acme", "active", null, pageable))
                .thenReturn(new PageImpl<>(List.of(company), pageable, 1));
        when(companyQueryRepository.countEmployeesByCompanyIds(List.of(companyId)))
                .thenReturn(Map.of(companyId, 7L));

        PaginatedResponse<CompanyRowDto> response = service.listCompanies("acme", "active", null, pageable);

        assertThat(response.items()).hasSize(1);
        assertThat(response.totalItems()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);

        CompanyRowDto row = response.items().getFirst();
        assertThat(row.employeeCount()).isEqualTo(7);
        assertThat(row.subscriptionEndDate()).isEqualTo(endDate.toString());
        assertThat(row.plan()).isEqualTo("Starter");
        assertThat(row.status()).isEqualTo("active");
    }

    @Test
    void extendTrialUpdatesTrialEndDateAndWritesPlatformEvent() {
        UUID companyId = UUID.randomUUID();
        Company company = company(companyId, CompanyStatus.ACTIVE, SubscriptionPlan.BASIC, SubscriptionStatus.TRIAL);
        company.setTrialEndsAt(LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
        LocalDate newEndDate = LocalDate.now(ZoneOffset.UTC).plusDays(30);
        UUID actorUserId = UUID.randomUUID();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(superAdminSecurity.currentPrincipal()).thenReturn(Optional.of(superAdminPrincipal(actorUserId)));

        ExtendTrialResponse response = service.extendTrial(companyId, new ExtendTrialRequest(newEndDate.toString()));

        assertThat(response.companyId()).isEqualTo(companyId.toString());
        assertThat(response.subscriptionEndDate()).isEqualTo(newEndDate.toString());
        assertThat(company.getTrialEndsAt().atZone(ZoneOffset.UTC).toLocalDate()).isEqualTo(newEndDate);
        verify(companyRepository).save(company);

        ArgumentCaptor<PlatformEvent> eventCaptor = ArgumentCaptor.forClass(PlatformEvent.class);
        verify(entityManager).persist(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("COMPANY_TRIAL_EXTENDED");
        assertThat(eventCaptor.getValue().getActorUserId()).isEqualTo(actorUserId);
        assertThat(eventCaptor.getValue().getDescription()).contains(newEndDate.toString());
    }

    @Test
    void extendTrialRejectsMissingInvalidAndPastDates() {
        UUID companyId = UUID.randomUUID();

        assertBusinessCode("TRIAL_END_DATE_REQUIRED",
                () -> service.extendTrial(companyId, new ExtendTrialRequest(" ")));
        assertBusinessCode("INVALID_TRIAL_END_DATE",
                () -> service.extendTrial(companyId, new ExtendTrialRequest("05/16/2026")));
        assertBusinessCode("TRIAL_END_DATE_IN_PAST",
                () -> service.extendTrial(companyId, new ExtendTrialRequest(LocalDate.now(ZoneOffset.UTC).minusDays(1).toString())));
    }

    @Test
    void extendTrialRejectsUnknownCompany() {
        UUID companyId = UUID.randomUUID();
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        assertBusinessCode("COMPANY_NOT_FOUND",
                () -> service.extendTrial(companyId, new ExtendTrialRequest(LocalDate.now(ZoneOffset.UTC).toString())));
    }

    @Test
    void toggleSuspendSuspendsActiveCompanyAndWritesEvent() {
        UUID companyId = UUID.randomUUID();
        Company company = company(companyId, CompanyStatus.ACTIVE, SubscriptionPlan.PREMIUM, SubscriptionStatus.ACTIVE);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(companyQueryRepository.countEmployeesByCompanyIds(List.of(companyId))).thenReturn(Map.of(companyId, 2L));

        CompanyRowDto row = service.toggleSuspend(companyId, new SuspendCompanyRequest("Billing issue"));

        assertThat(company.getStatus()).isEqualTo(CompanyStatus.SUSPENDED);
        assertThat(company.getSuspendedReason()).isEqualTo("Billing issue");
        assertThat(row.status()).isEqualTo("suspended");
        assertThat(row.plan()).isEqualTo("Professional");
        verify(companyStatusEmailService).sendCompanySuspendedEmail(company, "Billing issue");

        ArgumentCaptor<PlatformEvent> eventCaptor = ArgumentCaptor.forClass(PlatformEvent.class);
        verify(entityManager).persist(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("COMPANY_SUSPENDED");
    }

    @Test
    void toggleSuspendUnsuspendsSuspendedCompany() {
        UUID companyId = UUID.randomUUID();
        Company company = company(companyId, CompanyStatus.SUSPENDED, SubscriptionPlan.BASIC, SubscriptionStatus.TRIAL);
        company.setSuspendedReason("Old reason");
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(companyQueryRepository.countEmployeesByCompanyIds(List.of(companyId))).thenReturn(Map.of());

        CompanyRowDto row = service.toggleSuspend(companyId, new SuspendCompanyRequest("Resolved"));

        assertThat(company.getStatus()).isEqualTo(CompanyStatus.ACTIVE);
        assertThat(company.getSuspendedReason()).isNull();
        assertThat(row.status()).isEqualTo("active");
        verify(companyStatusEmailService).sendCompanyUnsuspendedEmail(company);
    }

    @Test
    void extendTrialEndpointRequiresSuperAdminAuthorization() throws NoSuchMethodException {
        Method method = SuperAdminCompaniesController.class.getMethod(
                "extendTrial",
                UUID.class,
                ExtendTrialRequest.class
        );

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("@superAdminSecurity.isSuperAdmin()");
    }

    private void assertBusinessCode(String expectedCode, Executable executable) {
        BusinessException exception = assertThrows(BusinessException.class, executable::execute);
        assertThat(exception.getCode()).isEqualTo(expectedCode);
    }

    private Company company(UUID id, CompanyStatus status, SubscriptionPlan plan, SubscriptionStatus subscriptionStatus) {
        Company company = new Company();
        company.setId(id);
        company.setName("Acme Corporation");
        company.setSlug("acme");
        company.setStatus(status);
        company.setNipt("L12345678Q");
        company.setEmail("info@acme.test");
        company.setPhoneNumber("+355690000000");
        company.setCountryCode("AL");
        company.setSubscriptionPlan(plan);
        company.setSubscriptionStatus(subscriptionStatus);
        company.setCreatedAt(Instant.parse("2026-05-01T10:15:30Z"));
        company.setUpdatedAt(Instant.parse("2026-05-01T10:15:30Z"));
        return company;
    }

    private AuthSessionPrincipal superAdminPrincipal(UUID userId) {
        return new AuthSessionPrincipal(
                userId,
                "superadmin@worknest.test",
                UUID.randomUUID(),
                "worknest-platform",
                UUID.randomUUID(),
                PlatformRole.SUPERADMIN,
                PlatformAccess.WEB
        );
    }

    @FunctionalInterface
    private interface Executable {
        void execute();
    }
}
