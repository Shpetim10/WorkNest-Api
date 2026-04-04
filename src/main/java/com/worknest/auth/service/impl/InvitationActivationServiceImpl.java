package com.worknest.auth.service.impl;

import com.worknest.audit.domain.AuditLog;
import com.worknest.audit.domain.PlatformEvent;
import com.worknest.audit.service.AuditLogService;
import com.worknest.audit.service.PlatformEventService;
import com.worknest.auth.domain.*;
import com.worknest.common.i18n.Language;
import com.worknest.auth.dto.ActivateInvitationRequest;
import com.worknest.auth.dto.ActivateInvitationResponse;
import com.worknest.auth.exception.InvalidRegistrationDataException;
import com.worknest.auth.exception.InvitationAlreadyUsedException;
import com.worknest.auth.exception.InvitationTokenExpiredException;
import com.worknest.auth.exception.InvitationTokenInvalidException;
import com.worknest.auth.exception.WeakPasswordException;
import com.worknest.auth.repository.CompanyRepository;
import com.worknest.auth.repository.RoleAssignmentRepository;
import com.worknest.auth.repository.UserInvitationRepository;
import com.worknest.auth.repository.UserRepository;
import com.worknest.auth.service.InvitationActivationService;
import com.worknest.auth.utility.Sha256TokenHashUtility;
import java.time.Instant;
import java.util.Map;
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
    private final AuditLogService auditLogService;
    private final PlatformEventService platformEventService;

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
        if (isInitialAdminActivation && !Boolean.TRUE.equals(request.gdprConsent())) {
            throw new InvalidRegistrationDataException(
                    "GDPR / Terms of Service consent is required to complete company registration");
        }

        User user = invitation.getUser();

        validatePassword(request.password(), user.getEmail());

        // Activate user
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);
        user.setPreferredLanguage(Language.fromCode(request.preferredLanguage()));
        user.setPhoneNumber(trimToNull(request.phoneNumber()));
        user.setProfileImageKey(trimToNull(request.profileImageStorageKey()));
        user.setProfileImagePath(trimToNull(request.profileImageStoragePath()));

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
        platformEventService.publishEvent(new PlatformEvent(
                "INVITATION_ACTIVATED",
                invitation.getCompany().getId(),
                invitation.getCompany().getName(),
                savedUser.getId(),
                "Invitation activated for " + savedUser.getEmail()
        ));

        auditLogService.logAction(new AuditLog(
                invitation.getCompany().getId(),
                savedUser.getId(),
                savedRoleAssignment.getId(),
                savedRoleAssignment.getRole(),
                savedRoleAssignment.getJobTitle(),
                "INVITATION_ACTIVATED",
                "User",
                savedUser.getId(),
                Map.of(
                        "status", savedUser.getStatus().name(),
                        "roleAssignmentId", savedRoleAssignment.getId(),
                        "platformAccess", invitation.getPlatformAccess().name(),
                        "gdprConsentRecorded", Boolean.TRUE.equals(request.gdprConsent()),
                        "companyActivated", isInitialAdminActivation
                ),
                Map.of(
                        "invitationId", invitation.getId(),
                        "platformRole", invitation.getPlatformRole().name(),
                        "invitationKind", invitation.getInvitationKind().name()
                ),
                null
        ));

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
        if (!StringUtils.hasText(request.password())) {
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

    // ── Helpers ───────────────────────────────────────────────────────────────────

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
}
