package com.worknest.auth.service.impl;

import com.worknest.audit.domain.AuditLog;
import com.worknest.audit.domain.PlatformEvent;
import com.worknest.audit.service.AuditLogService;
import com.worknest.audit.service.PlatformEventService;
import com.worknest.auth.domain.Company;
import com.worknest.auth.domain.CompanyStatus;
import com.worknest.auth.domain.PlatformRole;
import com.worknest.auth.domain.RoleAssignment;
import com.worknest.auth.domain.User;
import com.worknest.auth.domain.UserInvitation;
import com.worknest.auth.domain.UserStatus;
import com.worknest.auth.dto.CreateInvitationRequest;
import com.worknest.auth.dto.CreateInvitationResponse;
import com.worknest.auth.exception.InvalidInvitationRequestException;
import com.worknest.auth.exception.InvitationAlreadyExistsException;
import com.worknest.auth.exception.UserAlreadyActiveException;
import com.worknest.auth.repository.CompanyRepository;
import com.worknest.auth.repository.RoleAssignmentRepository;
import com.worknest.auth.repository.UserInvitationRepository;
import com.worknest.auth.repository.UserRepository;
import com.worknest.auth.service.InvitationService;
import com.worknest.auth.utility.SecureTokenGenerator;
import com.worknest.auth.utility.Sha256TokenHashUtility;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {

    private static final String DEFAULT_PREFERRED_LANGUAGE = "en";

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final UserInvitationRepository userInvitationRepository;
    private final SecureTokenGenerator secureTokenGenerator;
    private final Sha256TokenHashUtility sha256TokenHashUtility;
    private final AuditLogService auditLogService;
    private final PlatformEventService platformEventService;

    @Override
    @Transactional
    public CreateInvitationResponse createInvitation(CreateInvitationRequest request) {
        validateRequest(request);

        Company company = companyRepository.findById(request.companyId())
                .filter(savedCompany -> savedCompany.getDeletedAt() == null)
                .orElseThrow(() -> new InvalidInvitationRequestException("Company does not exist"));

        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new InvalidInvitationRequestException("Invitations can only be created for active companies");
        }

        String normalizedEmail = request.email().trim().toLowerCase();

        if (userInvitationRepository.existsByCompanyIdAndEmailIgnoreCaseAndUsedAtIsNullAndExpiresAtAfter(
                company.getId(),
                normalizedEmail,
                Instant.now()
        )) {
            throw new InvitationAlreadyExistsException(normalizedEmail);
        }

        User user = userRepository.findByCompanyIdAndEmailIgnoreCase(company.getId(), normalizedEmail)
                .map(existingUser -> validateExistingUser(existingUser, normalizedEmail))
                .orElseGet(() -> createPendingUser(company, normalizedEmail));

        User invitedBy = resolveCurrentAuthenticatedUser(company.getId());
        RoleAssignment inviterRoleAssignment = resolveActiveRoleAssignment(invitedBy.getId());

        String rawToken = secureTokenGenerator.generateToken();
        String tokenHash = sha256TokenHashUtility.hash(rawToken);
        Instant expiresAt = Instant.now().plusSeconds(24 * 60 * 60L);

        UserInvitation invitation = new UserInvitation();
        invitation.setCompany(company);
        invitation.setEmail(normalizedEmail);
        invitation.setTokenHash(tokenHash);
        invitation.setInvitedBy(invitedBy);
        invitation.setPlatformRole(request.platformRole());
        invitation.setExpiresAt(expiresAt);

        UserInvitation savedInvitation = userInvitationRepository.save(invitation);

        platformEventService.publishEvent(new PlatformEvent(
                "USER_INVITED",
                company.getId(),
                company.getName(),
                invitedBy.getId(),
                "User invitation created for " + normalizedEmail
        ));

        auditLogService.logAction(new AuditLog(
                company.getId(),
                invitedBy.getId(),
                inviterRoleAssignment != null ? inviterRoleAssignment.getId() : null,
                inviterRoleAssignment != null ? inviterRoleAssignment.getRole() : null,
                inviterRoleAssignment != null ? inviterRoleAssignment.getJobTitle() : null,
                "INVITATION_CREATED",
                "UserInvitation",
                savedInvitation.getId(),
                Map.of(
                        "email", savedInvitation.getEmail(),
                        "platformRole", savedInvitation.getPlatformRole().name(),
                        "expiresAt", savedInvitation.getExpiresAt().toString()
                ),
                Map.of(
                        "companyId", company.getId(),
                        "invitedByUserId", invitedBy.getId()
                ),
                null
        ));

        return new CreateInvitationResponse(
                savedInvitation.getId(),
                savedInvitation.getEmail(),
                savedInvitation.getExpiresAt(),
                "Invitation created successfully"
        );
    }

    private void validateRequest(CreateInvitationRequest request) {
        if (request == null) {
            throw new InvalidInvitationRequestException("Invitation request must not be null");
        }
        if (request.companyId() == null) {
            throw new InvalidInvitationRequestException("companyId is required");
        }
        if (!StringUtils.hasText(request.email())) {
            throw new InvalidInvitationRequestException("email is required");
        }
        if (request.platformRole() == null) {
            throw new InvalidInvitationRequestException("platformRole is required");
        }
    }

    private User validateExistingUser(User existingUser, String email) {
        if (existingUser.getStatus() == UserStatus.ACTIVE) {
            throw new UserAlreadyActiveException(email);
        }
        return existingUser;
    }

    private User createPendingUser(Company company, String email) {
        User user = new User();
        user.setCompany(company);
        user.setEmail(email);
        user.setPasswordHash(null);
        user.setStatus(UserStatus.PENDING);
        user.setPreferredLanguage(DEFAULT_PREFERRED_LANGUAGE);
        user.setFailedLoginCount((short) 0);
        return userRepository.save(user);
    }

    private User resolveCurrentAuthenticatedUser(UUID companyId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new InvalidInvitationRequestException("Authenticated inviter could not be resolved");
        }

        String principal = authentication.getName();
        if (!StringUtils.hasText(principal)) {
            throw new InvalidInvitationRequestException("Authenticated inviter could not be resolved");
        }

        return userRepository.findByCompanyIdAndEmailIgnoreCase(companyId, principal.trim().toLowerCase())
                .orElseThrow(() -> new InvalidInvitationRequestException(
                        "Authenticated inviter does not belong to the target company"
                ));
    }

    private RoleAssignment resolveActiveRoleAssignment(UUID userId) {
        return roleAssignmentRepository.findAllByUserIdAndIsActiveTrue(userId)
                .stream()
                .findFirst()
                .orElse(null);
    }
}
