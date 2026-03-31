package com.worknest.auth.service.impl;

import com.worknest.audit.domain.AuditLog;
import com.worknest.audit.domain.PlatformEvent;
import com.worknest.audit.service.AuditLogService;
import com.worknest.audit.service.PlatformEventService;
import com.worknest.auth.domain.Company;
import com.worknest.auth.domain.CompanyStatus;
import com.worknest.auth.domain.PlatformAccess;
import com.worknest.auth.domain.PlatformRole;
import com.worknest.auth.domain.RoleAssignment;
import com.worknest.auth.domain.User;
import com.worknest.auth.domain.UserStatus;
import com.worknest.auth.dto.CompanyRegistrationRequest;
import com.worknest.auth.dto.CompanyRegistrationResponse;
import com.worknest.auth.service.CompanyRegistrationService;
import com.worknest.auth.exception.AdminEmailAlreadyExistsException;
import com.worknest.auth.exception.CompanySlugAlreadyExistsException;
import com.worknest.auth.exception.InvalidRegistrationDataException;
import com.worknest.auth.repository.CompanyRepository;
import com.worknest.auth.repository.RoleAssignmentRepository;
import com.worknest.auth.repository.UserRepository;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CompanyRegistrationServiceImpl implements CompanyRegistrationService {

    private static final String DEFAULT_TIMEZONE = "Europe/Tirane";
    private static final String DEFAULT_LOCALE = "sq";
    private static final String DEFAULT_CURRENCY = "ALL";
    private static final String DEFAULT_DATE_FORMAT = "DD/MM/YYYY";
    private static final String DEFAULT_COUNTRY_CODE = "AL";
    private static final String DEFAULT_PREFERRED_LANGUAGE = "sq";
    private static final String DEFAULT_SUBSCRIPTION_STATUS = "trial";

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final AuditLogService auditLogService;
    private final PlatformEventService platformEventService;

    @Override
    @Transactional
    public CompanyRegistrationResponse registerCompany(CompanyRegistrationRequest request) {
        validateRequest(request);

        String normalizedSlug = request.slug().trim().toLowerCase();
        String normalizedAdminEmail = request.adminEmail().trim().toLowerCase();
        String normalizedPrimaryEmail = normalizeRequiredEmail(request.primaryEmail());

        if (companyRepository.existsBySlugIgnoreCaseAndDeletedAtIsNull(normalizedSlug)) {
            throw new CompanySlugAlreadyExistsException(normalizedSlug);
        }

        Company company = new Company();
        company.setName(request.companyName().trim());
        company.setLegalName(trimToNull(request.legalName()));
        company.setSlug(normalizedSlug);
        company.setStatus(CompanyStatus.ACTIVE);
        company.setNipt(trimToNull(request.nipt()));
        company.setRegistrationNumber(trimToNull(request.registrationNumber()));
        company.setVatNumber(trimToNull(request.vatNumber()));
        company.setPrimaryEmail(normalizedPrimaryEmail);
        company.setPrimaryPhone(trimToNull(request.primaryPhone()));
        company.setWebsite(trimToNull(request.website()));
        company.setCountryCode(defaultIfBlank(request.countryCode(), DEFAULT_COUNTRY_CODE).toUpperCase());
        company.setTimezone(defaultIfBlank(request.timezone(), DEFAULT_TIMEZONE));
        company.setLocale(defaultIfBlank(request.locale(), DEFAULT_LOCALE));
        company.setCurrency(defaultIfBlank(request.currency(), DEFAULT_CURRENCY));
        company.setDateFormat(defaultIfBlank(request.dateFormat(), DEFAULT_DATE_FORMAT));
        company.setOnboardingCompletedAt(null);
        company.setSubscriptionStatus(DEFAULT_SUBSCRIPTION_STATUS);
        company.setDataRetentionDays(90);

        Company savedCompany = companyRepository.save(company);

        if (userRepository.existsByCompanyIdAndEmailIgnoreCase(savedCompany.getId(), normalizedAdminEmail)) {
            throw new AdminEmailAlreadyExistsException(normalizedAdminEmail);
        }

        User adminUser = new User();
        adminUser.setCompany(savedCompany);
        adminUser.setEmail(normalizedAdminEmail);
        adminUser.setPasswordHash(null);
        adminUser.setStatus(UserStatus.PENDING);
        adminUser.setFirstName(request.adminFirstName().trim());
        adminUser.setLastName(request.adminLastName().trim());
        adminUser.setDisplayName(buildDisplayName(request.adminFirstName(), request.adminLastName()));
        adminUser.setPreferredLanguage(defaultIfBlank(request.preferredLanguage(), DEFAULT_PREFERRED_LANGUAGE));
        adminUser.setTimezoneOverride(null);
        adminUser.setFailedLoginCount((short) 0);
        adminUser.setMfaEnabled(false);

        User savedAdminUser = userRepository.save(adminUser);

        savedCompany.setOwnerUserId(savedAdminUser.getId());
        savedCompany = companyRepository.save(savedCompany);

        RoleAssignment adminRoleAssignment = new RoleAssignment();
        adminRoleAssignment.setCompany(savedCompany);
        adminRoleAssignment.setUser(savedAdminUser);
        adminRoleAssignment.setRole(PlatformRole.ADMIN);
        adminRoleAssignment.setIsActive(true);
        adminRoleAssignment.setActivatedAt(Instant.now());
        adminRoleAssignment.setCreatedBy(null);
        applyPlatformAccess(adminRoleAssignment, PlatformAccess.WEB);

        RoleAssignment savedRoleAssignment = roleAssignmentRepository.save(adminRoleAssignment);

        platformEventService.publishEvent(new PlatformEvent(
                "COMPANY_REGISTERED",
                savedCompany.getId(),
                savedCompany.getName(),
                savedAdminUser.getId(),
                "Company workspace registered and initial admin account prepared for activation"
        ));

        auditLogService.logAction(new AuditLog(
                savedCompany.getId(),
                savedAdminUser.getId(),
                savedRoleAssignment.getId(),
                PlatformRole.ADMIN,
                null,
                "COMPANY_REGISTERED",
                "Company",
                savedCompany.getId(),
                Map.of(
                        "companyName", savedCompany.getName(),
                        "slug", savedCompany.getSlug(),
                        "status", savedCompany.getStatus().name(),
                        "subscriptionStatus", savedCompany.getSubscriptionStatus()
                ),
                Map.of(
                        "adminUserId", savedAdminUser.getId(),
                        "adminRoleAssignmentId", savedRoleAssignment.getId(),
                        "workspaceCreated", true
                ),
                null
        ));

        return new CompanyRegistrationResponse(
                savedCompany.getId(),
                savedAdminUser.getId(),
                savedRoleAssignment.getId(),
                savedCompany.getStatus(),
                savedCompany.getOnboardingCompletedAt() != null,
                "Company registered successfully"
        );
    }

    private void validateRequest(CompanyRegistrationRequest request) {
        if (request == null) {
            throw new InvalidRegistrationDataException("Registration request must not be null");
        }
        if (!StringUtils.hasText(request.companyName())) {
            throw new InvalidRegistrationDataException("companyName is required");
        }
        if (!StringUtils.hasText(request.slug())) {
            throw new InvalidRegistrationDataException("slug is required");
        }
        if (!request.slug().trim().matches("^[a-z0-9-]+$")) {
            throw new InvalidRegistrationDataException("slug must contain only lowercase letters, numbers, and hyphens");
        }
        if (!StringUtils.hasText(request.adminEmail())) {
            throw new InvalidRegistrationDataException("adminEmail is required");
        }
        if (!StringUtils.hasText(request.primaryEmail())) {
            throw new InvalidRegistrationDataException("primaryEmail is required");
        }
        if (!StringUtils.hasText(request.adminFirstName())) {
            throw new InvalidRegistrationDataException("adminFirstName is required");
        }
        if (!StringUtils.hasText(request.adminLastName())) {
            throw new InvalidRegistrationDataException("adminLastName is required");
        }
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String normalizeRequiredEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String buildDisplayName(String firstName, String lastName) {
        return (firstName.trim() + " " + lastName.trim()).trim();
    }

    private void applyPlatformAccess(RoleAssignment roleAssignment, PlatformAccess platformAccess) {
        BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(roleAssignment);
        if (beanWrapper.isWritableProperty("platformAccess")) {
            beanWrapper.setPropertyValue("platformAccess", platformAccess);
        }
    }
}
