package com.worknest.features.superAdmin.application;

import com.worknest.audit.domain.PlatformEvent;
import com.worknest.common.api.PaginatedResponse;
import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.Company;
import com.worknest.domain.enums.CompanyStatus;
import com.worknest.domain.enums.SubscriptionPlan;
import com.worknest.domain.enums.SubscriptionStatus;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.superAdmin.dto.CompanyRowDto;
import com.worknest.features.superAdmin.dto.ExtendTrialRequest;
import com.worknest.features.superAdmin.dto.ExtendTrialResponse;
import com.worknest.features.superAdmin.dto.SuspendCompanyRequest;
import com.worknest.features.notification.email.service.CompanyStatusEmailService;
import com.worknest.features.superAdmin.repository.SuperAdminCompanyQueryRepository;
import com.worknest.security.SuperAdminSecurity;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SuperAdminCompaniesServiceImpl implements SuperAdminCompaniesService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter ISO_DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final SuperAdminCompanyQueryRepository companyQueryRepository;
    private final CompanyRepository companyRepository;
    private final EntityManager entityManager;
    private final CompanyStatusEmailService companyStatusEmailService;
    private final SuperAdminSecurity superAdminSecurity;

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<CompanyRowDto> listCompanies(String search, String status, String plan, Pageable pageable) {
        Page<Company> page = companyQueryRepository.findCompanies(search, status, plan, pageable);
        Map<UUID, Long> employeeCounts = companyQueryRepository.countEmployeesByCompanyIds(
                page.getContent().stream().map(Company::getId).toList()
        );
        return PaginatedResponse.from(page.map(company -> toDto(company, employeeCounts.getOrDefault(company.getId(), 0L))));
    }

    @Override
    @Transactional
    public CompanyRowDto toggleSuspend(UUID companyId, SuspendCompanyRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND",
                        "Company not found: " + companyId));

        boolean isSuspending = company.getStatus() != CompanyStatus.SUSPENDED;

        if (isSuspending) {
            company.setStatus(CompanyStatus.SUSPENDED);
            company.setSuspendedAt(Instant.now());
            company.setSuspendedReason(request.reason());
        } else {
            company.setStatus(CompanyStatus.ACTIVE);
            company.setSuspendedAt(null);
            company.setSuspendedReason(null);
        }

        companyRepository.save(company);

        String eventType = isSuspending ? "COMPANY_SUSPENDED" : "COMPANY_UNSUSPENDED";
        entityManager.persist(new PlatformEvent(eventType, company.getId(), company.getName(), null, request.reason()));

        if (isSuspending) {
            companyStatusEmailService.sendCompanySuspendedEmail(company, request.reason());
        } else {
            companyStatusEmailService.sendCompanyUnsuspendedEmail(company);
        }

        return toDto(company, countEmployees(company.getId()));
    }

    @Override
    @Transactional
    public ExtendTrialResponse extendTrial(UUID companyId, ExtendTrialRequest request) {
        LocalDate trialEndDate = parseTrialEndDate(request);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (trialEndDate.isBefore(today)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TRIAL_END_DATE_IN_PAST",
                    "Trial end date cannot be in the past");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND",
                        "Company not found: " + companyId));

        String oldEndDate = subscriptionEndDate(company);
        company.setTrialEndsAt(toEndOfDayUtc(trialEndDate));
        if (company.getSubscriptionStatus() == null) {
            company.setSubscriptionStatus(SubscriptionStatus.TRIAL);
        }
        companyRepository.save(company);

        String newEndDate = subscriptionEndDate(company);
        String description = "Trial extended from %s to %s".formatted(
                oldEndDate != null ? oldEndDate : "none",
                newEndDate
        );
        entityManager.persist(new PlatformEvent(
                "COMPANY_TRIAL_EXTENDED",
                company.getId(),
                company.getName(),
                superAdminSecurity.currentPrincipal().map(principal -> principal.userId()).orElse(null),
                description
        ));

        return new ExtendTrialResponse(company.getId().toString(), newEndDate);
    }

    private CompanyRowDto toDto(Company c, long employeeCount) {
        return new CompanyRowDto(
                c.getId().toString(),
                c.getName(),
                c.getName(),
                c.getCountryCode(),
                c.getNipt() != null ? c.getNipt() : "",
                c.getNipt() != null ? c.getNipt() : "",
                c.getEmail(),
                planLabel(c.getSubscriptionPlan()),
                employeeCount,
                statusLabel(c.getStatus()),
                DATE_FMT.format(c.getCreatedAt()),
                subscriptionEndDate(c)
        );
    }

    private long countEmployees(UUID companyId) {
        return companyQueryRepository.countEmployeesByCompanyIds(java.util.List.of(companyId))
                .getOrDefault(companyId, 0L);
    }

    private String planLabel(SubscriptionPlan plan) {
        if (plan == null) {
            return "";
        }
        return switch (plan) {
            case BASIC -> "Starter";
            case PREMIUM -> "Professional";
            case FOUNDATION -> "Foundation";
            case GROWTH -> "Growth";
            case PROFESSIONAL -> "Professional";
        };
    }

    private String statusLabel(CompanyStatus status) {
        return status == CompanyStatus.SUSPENDED ? "suspended" : "active";
    }

    private String subscriptionEndDate(Company company) {
        Instant end = null;
        if (company.getSubscriptionStatus() == SubscriptionStatus.TRIAL && company.getTrialEndsAt() != null) {
            end = company.getTrialEndsAt();
        } else if (company.getSubscriptionRenewalAt() != null) {
            end = company.getSubscriptionRenewalAt();
        } else if (company.getTrialEndsAt() != null) {
            end = company.getTrialEndsAt();
        }

        return end != null ? ISO_DATE_FMT.format(end.atZone(ZoneOffset.UTC).toLocalDate()) : null;
    }

    private LocalDate parseTrialEndDate(ExtendTrialRequest request) {
        if (request == null || request.trialEndDate() == null || request.trialEndDate().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TRIAL_END_DATE_REQUIRED",
                    "Trial end date is required");
        }
        try {
            return LocalDate.parse(request.trialEndDate().trim(), ISO_DATE_FMT);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_TRIAL_END_DATE",
                    "Trial end date must use YYYY-MM-DD format");
        }
    }

    private Instant toEndOfDayUtc(LocalDate date) {
        return date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusMillis(1);
    }
}
