package com.worknest.auth.service.impl;

import com.worknest.audit.domain.PlatformEvent;
import com.worknest.audit.repository.PlatformEventRepository;
import com.worknest.auth.domain.Company;
import com.worknest.auth.domain.CompanyStatus;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CompanyRegistrationServiceImpl implements CompanyRegistrationService {

    private static final String DEFAULT_TIMEZONE = "UTC";
    private static final String DEFAULT_LOCALE = "en";
    private static final String DEFAULT_PREFERRED_LANGUAGE = "en";

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final PlatformEventRepository platformEventRepository;

    @Override
    @Transactional
    public CompanyRegistrationResponse registerCompany(CompanyRegistrationRequest request) {
        validateRequest(request);

        String normalizedSlug = request.slug().trim().toLowerCase();
        String normalizedAdminEmail = request.adminEmail().trim().toLowerCase();
        String normalizedContactEmail = normalizeNullableEmail(request.contactEmail(), normalizedAdminEmail);

        if (companyRepository.existsBySlugIgnoreCaseAndDeletedAtIsNull(normalizedSlug)) {
            throw new CompanySlugAlreadyExistsException(normalizedSlug);
        }

        Company company = new Company();
        company.setName(request.companyName().trim());
        company.setSlug(normalizedSlug);
        company.setStatus(CompanyStatus.ACTIVE);
        company.setContactEmail(normalizedContactEmail);
        company.setTimezone(defaultIfBlank(request.timezone(), DEFAULT_TIMEZONE));
        company.setLocale(defaultIfBlank(request.locale(), DEFAULT_LOCALE));
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
        adminUser.setPreferredLanguage(defaultIfBlank(request.preferredLanguage(), DEFAULT_PREFERRED_LANGUAGE));
        adminUser.setFailedLoginCount((short) 0);

        User savedAdminUser = userRepository.save(adminUser);

        RoleAssignment adminRoleAssignment = new RoleAssignment();
        adminRoleAssignment.setCompany(savedCompany);
        adminRoleAssignment.setUser(savedAdminUser);
        adminRoleAssignment.setRole(PlatformRole.ADMIN);
        adminRoleAssignment.setIsActive(true);
        adminRoleAssignment.setActivatedAt(Instant.now());
        adminRoleAssignment.setCreatedBy(null);

        RoleAssignment savedRoleAssignment = roleAssignmentRepository.save(adminRoleAssignment);

        PlatformEvent platformEvent = new PlatformEvent(
                "COMPANY_REGISTERED",
                savedCompany.getId(),
                savedCompany.getName(),
                savedAdminUser.getId(),
                "Company workspace registered and initial admin account prepared for activation"
        );
        platformEventRepository.save(platformEvent);

        return new CompanyRegistrationResponse(
                savedCompany.getId(),
                savedAdminUser.getId(),
                savedRoleAssignment.getId(),
                savedCompany.getStatus(),
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
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String normalizeNullableEmail(String email, String fallback) {
        return StringUtils.hasText(email) ? email.trim().toLowerCase() : fallback;
    }
}
