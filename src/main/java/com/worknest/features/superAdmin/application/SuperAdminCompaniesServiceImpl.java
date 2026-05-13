package com.worknest.features.superAdmin.application;

import com.worknest.audit.domain.PlatformEvent;
import com.worknest.common.api.PaginatedResponse;
import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.Company;
import com.worknest.domain.enums.CompanyStatus;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.superAdmin.dto.CompanyRowDto;
import com.worknest.features.superAdmin.dto.SuspendCompanyRequest;
import com.worknest.features.notification.email.service.CompanyStatusEmailService;
import com.worknest.features.superAdmin.repository.SuperAdminCompanyQueryRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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

    private final SuperAdminCompanyQueryRepository companyQueryRepository;
    private final CompanyRepository companyRepository;
    private final EntityManager entityManager;
    private final CompanyStatusEmailService companyStatusEmailService;

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<CompanyRowDto> listCompanies(String search, String status, String plan, Pageable pageable) {
        Page<Company> page = companyQueryRepository.findCompanies(search, status, plan, pageable);
        return PaginatedResponse.from(page.map(this::toDto));
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

        return toDto(company);
    }

    private CompanyRowDto toDto(Company c) {
        return new CompanyRowDto(
                c.getId().toString(),
                c.getName(),
                c.getName(),
                c.getCountryCode(),
                c.getNipt() != null ? c.getNipt() : "",
                c.getNipt() != null ? c.getNipt() : "",
                c.getEmail(),
                c.getSubscriptionPlan() != null ? c.getSubscriptionPlan().name() : "",
                c.getStatus().name().toLowerCase(),
                DATE_FMT.format(c.getCreatedAt())
        );
    }
}
