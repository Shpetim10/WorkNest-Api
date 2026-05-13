package com.worknest.features.superAdmin.application;

import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.RoleAssignment;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.CompanyStatus;
import com.worknest.domain.enums.PlatformAccess;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.domain.enums.SubscriptionStatus;
import com.worknest.domain.enums.UserStatus;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminSeeder implements ApplicationRunner {

    @Value("${APP_SEED_ADMIN_EMAIL:}")
    private String superAdminEmail;

    @Value("${APP_SEED_ADMIN_PASSWORD:}")
    private String superAdminPassword;

    private final EntityManager entityManager;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(superAdminEmail) || !StringUtils.hasText(superAdminPassword)) {
            log.warn("SUPERADMIN_EMAIL or SUPERADMIN_PASSWORD not configured — skipping super-admin seed.");
            return;
        }

        Company platform = getOrCreatePlatformCompany();
        User admin = getOrCreateSuperAdminUser();
        getOrCreateRoleAssignment(admin, platform);

        log.info("Super-admin ready: {}", superAdminEmail);
    }

    private Company getOrCreatePlatformCompany() {
        List<Company> existing = entityManager.createQuery(
                        "SELECT c FROM Company c WHERE c.slug = :slug", Company.class)
                .setParameter("slug", "worknest-platform")
                .getResultList();

        if (!existing.isEmpty()) return existing.get(0);

        Company company = new Company();
        company.setName("WorkNest Platform");
        company.setSlug("worknest-platform");
        company.setEmail("platform@worknest.internal");
        company.setPhoneNumber("+000000000");
        company.setStatus(CompanyStatus.ACTIVE);
        company.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        entityManager.persist(company);
        entityManager.flush();
        return company;
    }

    private User getOrCreateSuperAdminUser() {
        String email = superAdminEmail.trim().toLowerCase();

        List<User> existing = entityManager.createQuery(
                        "SELECT u FROM User u WHERE LOWER(u.email) = :email", User.class)
                .setParameter("email", email)
                .getResultList();

        if (!existing.isEmpty()) {
            User user = existing.get(0);
            if (user.getStatus() != UserStatus.ACTIVE) {
                user.setStatus(UserStatus.ACTIVE);
            }
            return user;
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(superAdminPassword));
        user.setStatus(UserStatus.ACTIVE);
        user.setDisplayName("Super Admin");
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    private void getOrCreateRoleAssignment(User user, Company company) {
        List<RoleAssignment> existing = entityManager.createQuery(
                        "SELECT ra FROM RoleAssignment ra WHERE ra.user = :user AND ra.role = :role AND ra.isActive = true",
                        RoleAssignment.class)
                .setParameter("user", user)
                .setParameter("role", PlatformRole.SUPERADMIN)
                .getResultList();

        if (!existing.isEmpty()) return;

        RoleAssignment ra = new RoleAssignment();
        ra.setUser(user);
        ra.setCompany(company);
        ra.setRole(PlatformRole.SUPERADMIN);
        ra.setPlatformAccess(PlatformAccess.WEB);
        ra.setIsActive(true);
        ra.setActivatedAt(Instant.now());
        entityManager.persist(ra);
        entityManager.flush();
    }
}