package com.worknest.auth.service.impl;

import com.worknest.audit.domain.AuditLog;
import com.worknest.audit.domain.PlatformEvent;
import com.worknest.audit.service.AuditLogService;
import com.worknest.audit.service.PlatformEventService;
import com.worknest.auth.domain.PlatformRole;
import com.worknest.auth.domain.RoleAssignment;
import com.worknest.auth.domain.User;
import com.worknest.auth.domain.UserInvitation;
import com.worknest.auth.domain.UserStatus;
import com.worknest.auth.dto.ActivateInvitationRequest;
import com.worknest.auth.dto.ActivateInvitationResponse;
import com.worknest.auth.exception.InvitationAlreadyUsedException;
import com.worknest.auth.exception.InvalidInvitationRequestException;
import com.worknest.auth.exception.InvitationTokenExpiredException;
import com.worknest.auth.exception.InvitationTokenInvalidException;
import com.worknest.auth.exception.WeakPasswordException;
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
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final Sha256TokenHashUtility sha256TokenHashUtility;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final PlatformEventService platformEventService;

    @Override
    @Transactional
    public ActivateInvitationResponse activateInvitation(ActivateInvitationRequest request) {
        validateRequest(request);

        String tokenHash = sha256TokenHashUtility.hash(request.token());
        UserInvitation invitation = userInvitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvitationTokenInvalidException::new);

        if (invitation.isUsed()) {
            throw new InvitationAlreadyUsedException();
        }
        if (invitation.isExpired(Instant.now())) {
            throw new InvitationTokenExpiredException();
        }

        User user = userRepository.findByCompanyIdAndEmailIgnoreCase(
                        invitation.getCompany().getId(),
                        invitation.getEmail()
                )
                .orElseThrow(InvitationTokenInvalidException::new);

        validatePassword(request.password(), user.getEmail());

        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);
        User savedUser = userRepository.save(user);

        RoleAssignment roleAssignment = new RoleAssignment();
        roleAssignment.setCompany(invitation.getCompany());
        roleAssignment.setUser(savedUser);
        roleAssignment.setRole(invitation.getPlatformRole());
        roleAssignment.setJobTitle(resolveJobTitle(invitation.getPlatformRole(), request.jobTitle()));
        roleAssignment.setIsActive(true);
        roleAssignment.setActivatedAt(Instant.now());
        roleAssignment.setCreatedBy(invitation.getInvitedBy());
        RoleAssignment savedRoleAssignment = roleAssignmentRepository.save(roleAssignment);

        invitation.setUsedAt(Instant.now());
        userInvitationRepository.save(invitation);

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
                        "roleAssignmentId", savedRoleAssignment.getId()
                ),
                Map.of(
                        "invitationId", invitation.getId(),
                        "platformRole", invitation.getPlatformRole().name()
                ),
                null
        ));

        return new ActivateInvitationResponse(
                savedUser.getId(),
                savedRoleAssignment.getId(),
                savedUser.getStatus(),
                "Invitation activated successfully"
        );
    }

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
            throw new WeakPasswordException("Password must not be equal to user email");
        }
    }

    private String resolveJobTitle(PlatformRole platformRole, String requestedJobTitle) {
        if (platformRole == PlatformRole.STAFF && !StringUtils.hasText(requestedJobTitle)) {
            throw new InvalidInvitationRequestException("jobTitle is required for STAFF role assignment");
        }
        return StringUtils.hasText(requestedJobTitle) ? requestedJobTitle.trim() : null;
    }
}
