package com.worknest.common.config;

import com.worknest.common.i18n.Language;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.RoleAssignment;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.CompanyStatus;
import com.worknest.domain.enums.PlatformAccess;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.domain.enums.UserStatus;
import com.worknest.features.auth.repository.RoleAssignmentRepository;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.company.repository.CompanyRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialAdminSeeder implements ApplicationRunner {

    private final InitialAdminSeedProperties properties;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        if (!StringUtils.hasText(properties.getEmail()) || !StringUtils.hasText(properties.getPassword())) {
            log.warn("Initial admin seeding is enabled but email/password are not configured. Skipping seeding.");
            return;
        }

        if (roleAssignmentRepository.existsByRole(PlatformRole.SUPERADMIN)) {
            log.info("Skipping initial admin seed: a SUPERADMIN role assignment already exists.");
            return;
        }

        String normalizedEmail = properties.getEmail().trim().toLowerCase();
        User user = resolveOrCreateUser(normalizedEmail, properties.getPassword());
        Company company = resolveOrCreateCompany();
        createSuperAdminAssignment(user, company);
        log.info("Initial admin user seeded successfully for email '{}'.", normalizedEmail);
    }

    private User resolveOrCreateUser(String normalizedEmail, String rawPassword) {
        User user = userRepository.findAllByEmailIgnoreCase(normalizedEmail).stream()
                .filter(Objects::nonNull)
                .min(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElseGet(User::new);

        if (user.getId() == null) {
            user.setEmail(normalizedEmail);
            user.setFirstName(defaultIfBlank(properties.getFirstName(), "Platform"));
            user.setLastName(defaultIfBlank(properties.getLastName(), "Administrator"));
            user.setDisplayName((user.getFirstName() + " " + user.getLastName()).trim());
            user.setPreferredLanguage(Language.EN);
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }

    private Company resolveOrCreateCompany() {
        String slug = defaultIfBlank(properties.getCompanySlug(), "worknest-platform");
        return companyRepository.findBySlugIgnoreCase(slug)
                .map(existing -> {
                    existing.setStatus(CompanyStatus.ACTIVE);
                    return companyRepository.save(existing);
                })
                .orElseGet(() -> {
                    Company company = new Company();
                    company.setName(defaultIfBlank(properties.getCompanyName(), "WorkNest Platform"));
                    company.setSlug(slug);
                    company.setEmail(defaultIfBlank(properties.getCompanyEmail(), "platform@worknest.local"));
                    company.setPhoneNumber(defaultIfBlank(properties.getCompanyPhone(), "+355000000000"));
                    company.setStatus(CompanyStatus.ACTIVE);
                    company.setLocale(Language.EN);
                    company.setTimezone("UTC");
                    company.setCountryCode("US");
                    company.setCurrency("USD");
                    company.setDateFormat("MM/DD/YYYY");
                    return companyRepository.save(company);
                });
    }

    private void createSuperAdminAssignment(User user, Company company) {
        RoleAssignment roleAssignment = new RoleAssignment();
        roleAssignment.setUser(user);
        roleAssignment.setCompany(company);
        roleAssignment.setRole(PlatformRole.SUPERADMIN);
        roleAssignment.setPlatformAccess(PlatformAccess.WEB);
        roleAssignment.setIsActive(true);
        roleAssignment.setActivatedAt(Instant.now());
        roleAssignmentRepository.save(roleAssignment);
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
