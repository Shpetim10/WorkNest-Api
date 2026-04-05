package com.worknest.features.company.application;

import com.worknest.audit.service.AuthAuditService;
import com.worknest.audit.service.model.AuthAuditActorContext;
import com.worknest.domain.entities.Company;
import com.worknest.domain.enums.CompanyStatus;
import com.worknest.domain.enums.InvitationKind;
import com.worknest.domain.enums.PlatformAccess;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.domain.entities.RoleAssignment;
import com.worknest.domain.enums.SubscriptionPlan;
import com.worknest.domain.enums.SubscriptionStatus;
import com.worknest.domain.entities.User;
import com.worknest.domain.entities.UserInvitation;
import com.worknest.domain.enums.UserStatus;
import com.worknest.common.i18n.Language;
import com.worknest.features.company.dto.CompanyRegistrationRequest;
import com.worknest.features.company.dto.CompanyRegistrationResponse;
import com.worknest.features.company.exception.AdminEmailAlreadyExistsException;
import com.worknest.features.company.exception.CompanySlugAlreadyExistsException;
import com.worknest.features.company.exception.InvalidRegistrationDataException;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.auth.repository.RoleAssignmentRepository;
import com.worknest.features.invitation.repository.UserInvitationRepository;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.notification.email.service.InvitationEmailService;
import com.worknest.features.auth.utility.SecureTokenGenerator;
import com.worknest.features.auth.utility.Sha256TokenHashUtility;
import com.worknest.features.media.application.MediaStorageService;
import com.worknest.features.media.dto.MediaUploadResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyRegistrationServiceImpl implements CompanyRegistrationService {

    private static final String DEFAULT_TIMEZONE = "Europe/Tirane";
    private static final String DEFAULT_CURRENCY = "ALL";
    private static final String DEFAULT_DATE_FORMAT = "DD/MM/YYYY";
    private static final String DEFAULT_COUNTRY_CODE = "AL";

    @Value("${app.frontend.activation-link-base:https://app.worknest.local/activate-invitation}")
    private String activationLinkBase;

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final UserInvitationRepository userInvitationRepository;
    private final SecureTokenGenerator secureTokenGenerator;
    private final Sha256TokenHashUtility sha256TokenHashUtility;
    private final InvitationEmailService invitationEmailService;
    private final AuthAuditService authAuditService;
    private final MediaStorageService mediaStorageService;

    @Override
    @Transactional
    public CompanyRegistrationResponse registerCompany(CompanyRegistrationRequest request) {
        validateRequest(request);

        String normalizedSlug = request.slug().trim().toLowerCase();
        String normalizedAdminEmail = request.adminEmail().trim().toLowerCase();
        String normalizedPrimaryEmail = request.primaryEmail().trim().toLowerCase();

        // Uniqueness checks
        if (companyRepository.existsBySlugIgnoreCaseAndDeletedAtIsNull(normalizedSlug)) {
            throw new CompanySlugAlreadyExistsException(normalizedSlug);
        }

        // 1. Persist Company (PENDING until admin activates invitation)
        Company company = new Company();
        company.setName(request.companyName().trim());
        company.setSlug(normalizedSlug);
        company.setStatus(CompanyStatus.PENDING);
        company.setNipt(trimToNull(request.nipt()));
        company.setEmail(normalizedPrimaryEmail);
        company.setPhoneNumber(trimToNull(request.primaryPhone()));
        company.setCountryCode(defaultIfBlank(request.countryCode(), DEFAULT_COUNTRY_CODE).toUpperCase());
        company.setTimezone(defaultIfBlank(request.timezone(), DEFAULT_TIMEZONE));
        company.setLocale(Language.fromCode(request.locale()));
        company.setCurrency(defaultIfBlank(request.currency(), DEFAULT_CURRENCY));
        company.setDateFormat(defaultIfBlank(request.dateFormat(), DEFAULT_DATE_FORMAT));
        company.setIndustry(trimToNull(request.industry()));
        company.setOnboardingCompletedAt(null);
        company.setSubscriptionPlan(SubscriptionPlan.BASIC);
        company.setSubscriptionStatus(SubscriptionStatus.TRIAL);
        company.setDataRetentionDays(90);

        // Handle pre-uploaded logo during registration
        if (StringUtils.hasText(request.logoKey())) {
            company.setLogoKey(request.logoKey());
            company.setLogoPath(request.logoPath());
        }

        Company savedCompany = companyRepository.save(company);

        // 2. Create admin User (no password — awaiting invitation activation)
        if (userRepository.existsByCompanyIdAndEmailIgnoreCase(savedCompany.getId(), normalizedAdminEmail)) {
            throw new AdminEmailAlreadyExistsException(normalizedAdminEmail);
        }

        User adminUser = new User();
        adminUser.setCompany(savedCompany);
        adminUser.setEmail(normalizedAdminEmail);
        adminUser.setPasswordHash(null);
        adminUser.setStatus(UserStatus.PENDING);
        adminUser.setFirstName(trimToEmpty(request.adminFirstName()));
        adminUser.setLastName(trimToEmpty(request.adminLastName()));
        adminUser.setDisplayName(buildDisplayName(request.adminFirstName(), request.adminLastName()));
        adminUser.setPhoneNumber(trimToNull(request.adminPhoneNumber()));
        adminUser.setPreferredLanguage(Language.fromCode(request.preferredLanguage()));
        adminUser.setTimezoneOverride(null);
        adminUser.setFailedLoginCount((short) 0);
        adminUser.setMfaEnabled(false);

        User savedAdminUser = userRepository.save(adminUser);

        // 3. Create RoleAssignment for ADMIN
        RoleAssignment adminRoleAssignment = new RoleAssignment();
        adminRoleAssignment.setCompany(savedCompany);
        adminRoleAssignment.setUser(savedAdminUser);
        adminRoleAssignment.setRole(PlatformRole.ADMIN);
        adminRoleAssignment.setIsActive(false);
        adminRoleAssignment.setPlatformAccess(PlatformAccess.WEB);

        RoleAssignment savedRoleAssignment = roleAssignmentRepository.save(adminRoleAssignment);

        // 4. Issue INITIAL_ADMIN_ACTIVATION invitation
        String rawActivationToken = secureTokenGenerator.generateToken();
        UserInvitation adminInvitation = new UserInvitation();
        adminInvitation.setCompany(savedCompany);
        adminInvitation.setUser(savedAdminUser);
        adminInvitation.setEmail(normalizedAdminEmail);
        adminInvitation.setTokenHash(sha256TokenHashUtility.hash(rawActivationToken));
        adminInvitation.setInvitedBy(null);
        adminInvitation.setPlatformRole(PlatformRole.ADMIN);
        adminInvitation.setPlatformAccess(PlatformAccess.WEB);
        adminInvitation.setInvitationKind(InvitationKind.INITIAL_ADMIN_ACTIVATION);
        adminInvitation.setInvitedJobTitle(null);

        UserInvitation savedInvitation = userInvitationRepository.save(adminInvitation);

        // 5. If logo was provided, promote it from public/temp to company directory
        if (StringUtils.hasText(request.logoKey())) {
            try {
                MediaUploadResponse promotion = mediaStorageService.promoteLogo(request.logoKey(), savedCompany.getId());
                savedCompany.setLogoKey(promotion.storageKey());
                savedCompany.setLogoPath(promotion.storagePath());
                companyRepository.save(savedCompany);
            } catch (Exception e) {
                // Log but don't fail registration if logo promotion fails
                log.error("Failed to promote registration logo for company {}: {}", savedCompany.getId(), e.getMessage());
            }
        }

        // The raw token is dispatched via email — not returned in the response body.
        String activationLink = activationLinkBase + "?token=" + rawActivationToken;
        invitationEmailService.sendInvitationEmail(
                savedCompany,
                normalizedAdminEmail,
                savedAdminUser.getDisplayName(),
                PlatformRole.ADMIN,
                InvitationKind.INITIAL_ADMIN_ACTIVATION,
                activationLink,
                request.preferredLanguage());

        // 6. Emit platform events & audit
        AuthAuditActorContext actorContext = new AuthAuditActorContext(
                savedCompany.getId(),
                savedCompany.getName(),
                savedAdminUser.getId(),
                savedRoleAssignment.getId(),
                PlatformRole.ADMIN,
                null,
                null // IP address not available in this context
        );

        authAuditService.appendCompanyRegistered(
                actorContext,
                savedCompany.getId(),
                savedAdminUser.getId(),
                savedRoleAssignment.getId(),
                savedCompany.getSubscriptionStatus().name()
        );

        return new CompanyRegistrationResponse(
                savedCompany.getId(),
                savedAdminUser.getId(),
                savedRoleAssignment.getId(),
                savedInvitation.getId(),
                savedCompany.getStatus(),
                false,
                true,
                "Company registered successfully. An activation invitation has been sent to " + normalizedAdminEmail + "."
        );
    }

    // Validation

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
        if (!StringUtils.hasText(request.primaryEmail())) {
            throw new InvalidRegistrationDataException("primaryEmail is required");
        }
        if (!StringUtils.hasText(request.adminEmail())) {
            throw new InvalidRegistrationDataException("adminEmail is required");
        }
        if (!StringUtils.hasText(request.adminFirstName())) {
            throw new InvalidRegistrationDataException("adminFirstName is required");
        }
        if (!StringUtils.hasText(request.adminLastName())) {
            throw new InvalidRegistrationDataException("adminLastName is required");
        }
    }

    // Helpers

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String trimToEmpty(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String buildDisplayName(String firstName, String lastName) {
        return (trimToEmpty(firstName) + " " + trimToEmpty(lastName)).trim();
    }
}
