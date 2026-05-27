package com.worknest.features.superAdmin.application;

import com.worknest.audit.domain.PlatformEvent;
import com.worknest.common.api.PaginatedResponse;
import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.Subscription;
import com.worknest.domain.enums.CompanyStatus;
import com.worknest.domain.enums.SubscriptionPlan;
import com.worknest.domain.enums.SubscriptionStatus;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.subscription.repository.SubscriptionRepository;
import com.worknest.features.superAdmin.dto.CompanyRowDto;
import com.worknest.features.superAdmin.dto.ExtendTrialRequest;
import com.worknest.features.superAdmin.dto.ExtendTrialResponse;
import com.worknest.features.superAdmin.dto.PendingDeactivationDto;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
    private final SubscriptionRepository subscriptionRepository;
    private final EntityManager entityManager;
    private final CompanyStatusEmailService companyStatusEmailService;
    private final SuperAdminSecurity superAdminSecurity;

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<CompanyRowDto> listCompanies(String search, String status, String plan, Pageable pageable) {
        Page<Company> page = companyQueryRepository.findCompanies(search, status, plan, pageable);
        List<UUID> companyIds = page.getContent().stream().map(Company::getId).toList();

        Map<UUID, Long> employeeCounts = companyQueryRepository.countEmployeesByCompanyIds(companyIds);
        Map<UUID, Subscription> subscriptions = subscriptionRepository.findLatestPerCompany(companyIds)
                .stream().collect(Collectors.toMap(Subscription::getCompanyId, s -> s, (a, b) -> a));

        return PaginatedResponse.from(page.map(company -> toDto(
                company,
                employeeCounts.getOrDefault(company.getId(), 0L),
                subscriptions.get(company.getId())
        )));
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

        return toDto(company, countEmployees(company.getId()), latestSubscription(company.getId()));
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

        String oldEndDate = resolveEndDate(company.getSubscriptionStatus(), company.getTrialEndsAt(), company.getSubscriptionRenewalAt());
        company.setTrialEndsAt(toEndOfDayUtc(trialEndDate));
        if (company.getSubscriptionStatus() == null) {
            company.setSubscriptionStatus(SubscriptionStatus.TRIALING);
        }
        companyRepository.save(company);

        String newEndDate = resolveEndDate(company.getSubscriptionStatus(), company.getTrialEndsAt(), company.getSubscriptionRenewalAt());
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

    private CompanyRowDto toDto(Company c, long employeeCount, Subscription sub) {
        // Prefer live subscription table data; fall back to Company entity fields
        SubscriptionPlan plan = sub != null ? sub.getPlan() : c.getSubscriptionPlan();
        SubscriptionStatus subStatus = sub != null ? sub.getStatus() : c.getSubscriptionStatus();
        Instant trialEndsAtInstant = sub != null ? sub.getTrialEndsAt() : c.getTrialEndsAt();
        Instant renewalInstant = sub != null ? sub.getCurrentPeriodEnd() : c.getSubscriptionRenewalAt();

        // If the trial end date has passed but status is still TRIALING (webhook missed in dev),
        // treat it as ACTIVE so the UI shows the correct state
        if (subStatus == SubscriptionStatus.TRIALING
                && trialEndsAtInstant != null
                && trialEndsAtInstant.isBefore(Instant.now())) {
            subStatus = SubscriptionStatus.ACTIVE;
        }

        String trialEndsAt = trialEndsAtInstant != null
                ? ISO_DATE_FMT.format(trialEndsAtInstant.atZone(ZoneOffset.UTC).toLocalDate())
                : null;

        String subscriptionEndDate = resolveEndDate(subStatus, trialEndsAtInstant, renewalInstant);

        return new CompanyRowDto(
                c.getId().toString(),
                c.getName(),
                c.getName(),
                c.getCountryCode(),
                c.getNipt() != null ? c.getNipt() : "",
                c.getNipt() != null ? c.getNipt() : "",
                c.getEmail(),
                planLabel(plan),
                employeeCount,
                statusLabel(c.getStatus()),
                DATE_FMT.format(c.getCreatedAt()),
                subscriptionEndDate,
                subStatus != null ? subStatus.name() : null,
                trialEndsAt,
                c.getDeactivationRequestedAt() != null ? c.getDeactivationRequestedAt().toString() : null,
                c.getDeletionScheduledAt() != null ? c.getDeletionScheduledAt().toString() : null
        );
    }

    private Subscription latestSubscription(UUID companyId) {
        return subscriptionRepository.findTopByCompanyIdOrderByCreatedAtDesc(companyId).orElse(null);
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
            case FOUNDATION -> "Foundation";
            case GROWTH -> "Growth";
            case PROFESSIONAL -> "Professional";
        };
    }

    private String statusLabel(CompanyStatus status) {
        return status == CompanyStatus.SUSPENDED ? "suspended" : "active";
    }

    private String resolveEndDate(SubscriptionStatus status, Instant trialEndsAt, Instant renewalAt) {
        Instant end = null;
        if (status == SubscriptionStatus.TRIALING && trialEndsAt != null) {
            end = trialEndsAt;
        } else if (renewalAt != null) {
            end = renewalAt;
        } else if (trialEndsAt != null) {
            end = trialEndsAt;
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

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<PendingDeactivationDto> listPendingDeactivation(Pageable pageable) {
        Page<Company> page = companyQueryRepository.findPendingDeactivation(pageable);
        return PaginatedResponse.from(page.map(this::toPendingDeactivationDto));
    }

    @Override
    @Transactional
    public CompanyRowDto reactivateCompany(UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND",
                        "Company not found: " + companyId));

        if (company.getDeactivationRequestedAt() == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "NO_DEACTIVATION_REQUESTED",
                    "No pending deactivation request found for company: " + companyId);
        }

        company.setStatus(CompanyStatus.ACTIVE);
        company.setDeactivationRequestedAt(null);
        company.setDeletionScheduledAt(null);
        companyRepository.save(company);

        entityManager.persist(new PlatformEvent("COMPANY_REACTIVATED", company.getId(), company.getName(),
                superAdminSecurity.currentPrincipal().map(p -> p.userId()).orElse(null), null));

        return toDto(company, countEmployees(company.getId()), latestSubscription(company.getId()));
    }

    private PendingDeactivationDto toPendingDeactivationDto(Company c) {
        Instant deactivationRequestedAt = c.getDeactivationRequestedAt();
        Instant deletionScheduledAt = c.getDeletionScheduledAt();
        long daysUntilDeletion = deletionScheduledAt != null
                ? ChronoUnit.DAYS.between(Instant.now(), deletionScheduledAt)
                : 0L;
        return new PendingDeactivationDto(
                c.getId().toString(),
                c.getName(),
                c.getEmail(),
                deactivationRequestedAt != null ? ISO_DATE_FMT.format(deactivationRequestedAt.atZone(ZoneOffset.UTC).toLocalDate()) : null,
                deletionScheduledAt != null ? ISO_DATE_FMT.format(deletionScheduledAt.atZone(ZoneOffset.UTC).toLocalDate()) : null,
                Math.max(daysUntilDeletion, 0L)
        );
    }

    private Instant toEndOfDayUtc(LocalDate date) {
        return date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusMillis(1);
    }
}
