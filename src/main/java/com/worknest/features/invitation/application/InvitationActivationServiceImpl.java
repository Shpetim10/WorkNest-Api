package com.worknest.features.invitation.application;

import com.worknest.audit.service.AuthAuditService;
import com.worknest.audit.service.model.AuthAuditActorContext;
import com.worknest.audit.service.model.AuthSessionContext;
import com.worknest.domain.entities.*;
import com.worknest.domain.enums.*;
import com.worknest.common.i18n.Language;
import com.worknest.features.invitation.dto.ActivateInvitationRequest;
import com.worknest.features.invitation.dto.ActivateInvitationResponse;
import com.worknest.features.company.exception.InvalidRegistrationDataException;
import com.worknest.features.invitation.exception.InvitationAlreadyUsedException;
import com.worknest.features.invitation.exception.InvitationTokenExpiredException;
import com.worknest.features.invitation.exception.InvitationTokenInvalidException;
import com.worknest.features.auth.exception.WeakPasswordException;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.auth.repository.RoleAssignmentRepository;
import com.worknest.features.invitation.repository.UserInvitationRepository;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.auth.utility.Sha256TokenHashUtility;
import java.time.Instant;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class InvitationActivationServiceImpl implements InvitationActivationService {

    private final UserInvitationRepository userInvitationRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final Sha256TokenHashUtility sha256TokenHashUtility;
    private final PasswordEncoder passwordEncoder;
    private final AuthAuditService authAuditService;
    private final com.worknest.features.media.application.MediaStorageService mediaStorageService;

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public com.worknest.features.invitation.dto.PreflightInvitationResponse validateToken(com.worknest.features.invitation.dto.ValidateInvitationRequest request) {
        if (request == null || !org.springframework.util.StringUtils.hasText(request.token())) {
            throw new com.worknest.features.invitation.exception.InvitationTokenInvalidException();
        }

        String tokenHash = sha256TokenHashUtility.hash(request.token());

        com.worknest.domain.entities.UserInvitation invitation = userInvitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(com.worknest.features.invitation.exception.InvitationTokenInvalidException::new);

        if (invitation.isUsed()) {
            throw new com.worknest.features.invitation.exception.InvitationAlreadyUsedException();
        }
        if (invitation.isExpired(java.time.Instant.now())) {
            throw new com.worknest.features.invitation.exception.InvitationTokenExpiredException();
        }

        return new com.worknest.features.invitation.dto.PreflightInvitationResponse(
                invitation.getEmail(),
                invitation.getCompany() != null ? invitation.getCompany().getName() : null,
                invitation.getPlatformRole(),
                invitation.getInvitedJobTitle()
        );
    }

    @Override
    @Transactional
    public ActivateInvitationResponse activateInvitation(ActivateInvitationRequest request, String clientIp) {
        validateRequest(request);

        Instant now = Instant.now();
        String tokenHash = sha256TokenHashUtility.hash(request.token());

        UserInvitation invitation = userInvitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvitationTokenInvalidException::new);

        if (invitation.isUsed()) {
            throw new InvitationAlreadyUsedException();
        }
        if (invitation.isExpired(now)) {
            throw new InvitationTokenExpiredException();
        }

        boolean isInitialAdminActivation = InvitationKind.INITIAL_ADMIN_ACTIVATION
                .equals(invitation.getInvitationKind());

        // GDPR consent is mandatory for the initial admin (accepting on behalf of the company)
        if (!Boolean.TRUE.equals(request.gdprConsent())) {
            throw new InvalidRegistrationDataException(
                    "GDPR / Terms of Service consent is required to complete company registration");
        }

        User user = resolveCanonicalUser(invitation);
        invitation.setUser(user);

        if (!StringUtils.hasText(user.getPasswordHash())) {
            validatePassword(request.password(), user.getEmail());
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        // Activate user
        user.setStatus(UserStatus.ACTIVE);
        user.setPreferredLanguage(Language.fromCode(request.preferredLanguage()));
        
        // Handle profile image promotion from public/temp
        if (StringUtils.hasText(request.profileImageStorageKey())) {
            try {
                com.worknest.features.media.dto.MediaUploadResponse promotion = 
                    mediaStorageService.promoteProfileImage(
                        request.profileImageStorageKey(), 
                        invitation.getCompany().getId(), 
                        user.getId()
                    );
                user.setProfileImageKey(promotion.storageKey());
                user.setProfileImagePath(promotion.storagePath());
            } catch (Exception e) {
                // Log but don't fail activation if photo promotion fails
                // In production, you'd use a logger here
            }
        }

        // Record GDPR consent when provided
        if (Boolean.TRUE.equals(request.gdprConsent())) {
            user.setGdprConsentAt(now);
            user.setGdprConsentIp(trimToNull(clientIp));
        }

        User savedUser = userRepository.save(user);

        // Resolve role assignment
        RoleAssignment savedRoleAssignment = roleAssignmentRepository
                .findFirstByUserIdAndCompanyIdAndIsActiveTrue(savedUser.getId(), invitation.getCompany().getId())
                .orElseGet(() -> activateExistingRoleAssignmentOrCreate(invitation, savedUser, now));

        // Activate company on initial admin setup
        if (isInitialAdminActivation) {
            Company company = invitation.getCompany();
            if (CompanyStatus.PENDING.equals(company.getStatus())) {
                company.setStatus(CompanyStatus.ACTIVE);
                companyRepository.save(company);
            }
        }

        // Mark invitation as used
        invitation.setUsedAt(now);
        userInvitationRepository.save(invitation);

        // Events & audit
        // Emit platform events & audit
        AuthAuditActorContext actorContext = new AuthAuditActorContext(
                invitation.getCompany().getId(),
                invitation.getCompany().getName(),
                savedUser.getId(),
                savedRoleAssignment.getId(),
                savedRoleAssignment.getRole(),
                savedRoleAssignment.getJobTitle(),
                clientIp
        );

        AuthSessionContext sessionContext = new AuthSessionContext(
                savedUser.getId(),
                savedRoleAssignment.getId(),
                savedRoleAssignment.getRole(),
                invitation.getPlatformAccess()
        );

        authAuditService.appendInvitationActivated(actorContext, invitation.getId(), savedUser.getId(), sessionContext);

        return new ActivateInvitationResponse(
                savedUser.getId(),
                savedRoleAssignment.getId(),
                savedRoleAssignment.getRole(),
                invitation.getPlatformAccess(),
                savedUser.getStatus(),
                "Invitation activated successfully"
        );
    }

    // Validation
    private void validateRequest(ActivateInvitationRequest request) {
        if (request == null) {
            throw new InvitationTokenInvalidException();
        }
        if (!StringUtils.hasText(request.token())) {
            throw new InvitationTokenInvalidException();
        }
        if (request.password() != null && request.password().isBlank()) {
            throw new WeakPasswordException("Password is required");
        }
    }

    private void validatePassword(String password, String userEmail) {
        if (password.length() < 8) {
            throw new WeakPasswordException("Password must be at least 8 characters long");
        }
        if (!password.chars().anyMatch(Character::isUpperCase)) {
            throw new WeakPasswordException("Password must contain at least one uppercase letter");
        }
        if (!password.chars().anyMatch(Character::isDigit)) {
            throw new WeakPasswordException("Password must contain at least one number");
        }
        if (password.equalsIgnoreCase(userEmail)) {
            throw new WeakPasswordException("Password must not match the user email");
        }
    }

    // Helpers

    private RoleAssignment createRoleAssignmentFromInvitation(
            UserInvitation invitation, User user, Instant now) {
        RoleAssignment ra = new RoleAssignment();
        ra.setCompany(invitation.getCompany());
        ra.setUser(user);
        ra.setRole(invitation.getPlatformRole());
        ra.setJobTitle(StringUtils.hasText(invitation.getInvitedJobTitle())
                ? invitation.getInvitedJobTitle().trim() : null);
        ra.setIsActive(true);
        ra.setActivatedAt(now);
        ra.setCreatedBy(invitation.getInvitedBy());
        ra.setPlatformAccess(invitation.getPlatformAccess());
        return roleAssignmentRepository.save(ra);
    }

    private RoleAssignment activateExistingRoleAssignmentOrCreate(
            UserInvitation invitation, User user, Instant now) {
        return roleAssignmentRepository
                .findFirstByUserIdAndCompanyIdOrderByCreatedAtAsc(user.getId(), invitation.getCompany().getId())
                .map(existingRoleAssignment -> activateExistingRoleAssignment(existingRoleAssignment, invitation, now))
                .orElseGet(() -> createRoleAssignmentFromInvitation(invitation, user, now));
    }

    private RoleAssignment activateExistingRoleAssignment(
            RoleAssignment roleAssignment, UserInvitation invitation, Instant now) {
        roleAssignment.setRole(invitation.getPlatformRole());
        roleAssignment.setJobTitle(StringUtils.hasText(invitation.getInvitedJobTitle())
                ? invitation.getInvitedJobTitle().trim() : null);
        roleAssignment.setIsActive(true);
        roleAssignment.setActivatedAt(now);
        roleAssignment.setDeactivatedAt(null);
        roleAssignment.setPlatformAccess(invitation.getPlatformAccess());
        if (roleAssignment.getCreatedBy() == null) {
            roleAssignment.setCreatedBy(invitation.getInvitedBy());
        }
        return roleAssignmentRepository.save(roleAssignment);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private User resolveCanonicalUser(UserInvitation invitation) {
        return userRepository.findAllByEmailIgnoreCase(invitation.getEmail())
                .stream()
                .sorted(Comparator
                        .comparing((User user) -> user.getStatus() == UserStatus.ACTIVE ? 0 : 1)
                        .thenComparing(user -> StringUtils.hasText(user.getPasswordHash()) ? 0 : 1)
                        .thenComparing(User::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .findFirst()
                .orElse(invitation.getUser());
    }
}
