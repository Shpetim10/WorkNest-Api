package com.worknest.features.invitation.application;

import com.worknest.audit.domain.AuditLog;
import com.worknest.audit.domain.PlatformEvent;
import com.worknest.audit.service.AuditLogService;
import com.worknest.audit.service.PlatformEventService;
import com.worknest.domain.entities.Company;
import com.worknest.domain.enums.CompanyStatus;
import com.worknest.domain.enums.InvitationKind;
import com.worknest.domain.entities.Permission;
import com.worknest.domain.enums.PlatformAccess;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.domain.entities.RoleAssignment;
import com.worknest.domain.entities.RoleAssignmentPermission;
import com.worknest.domain.entities.User;
import com.worknest.domain.entities.UserInvitation;
import com.worknest.domain.enums.UserStatus;
import com.worknest.common.i18n.Language;
import com.worknest.features.invitation.dto.CreateInvitationRequest;
import com.worknest.features.invitation.dto.CreateInvitationResponse;
import com.worknest.features.invitation.exception.InvalidInvitationRequestException;
import com.worknest.features.invitation.exception.InvitationAlreadyExistsException;
import com.worknest.features.auth.exception.UserAlreadyActiveException;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.auth.repository.PermissionRepository;
import com.worknest.features.auth.repository.RoleAssignmentPermissionRepository;
import com.worknest.features.auth.repository.RoleAssignmentRepository;
import com.worknest.features.invitation.repository.UserInvitationRepository;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.notification.email.service.InvitationEmailService;
import com.worknest.features.invitation.application.InvitationService;
import com.worknest.features.auth.utility.SecureTokenGenerator;
import com.worknest.features.auth.utility.Sha256TokenHashUtility;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final RoleAssignmentPermissionRepository roleAssignmentPermissionRepository;
    private final PermissionRepository permissionRepository;
    private final UserInvitationRepository userInvitationRepository;
    private final SecureTokenGenerator secureTokenGenerator;
    private final Sha256TokenHashUtility sha256TokenHashUtility;
    private final InvitationEmailService invitationEmailService;
    private final AuditLogService auditLogService;
    private final PlatformEventService platformEventService;

    @Value("${app.frontend.activation-link-base:https://app.worknest.local/activate-invitation}")
    private String activationLinkBase;

    @Override
    @Transactional
    public CreateInvitationResponse createInvitation(CreateInvitationRequest request) {
        // validate
        validateRequest(request);

        // Find company
        Company company = companyRepository.findById(Objects.requireNonNull(request.companyId()))
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new InvalidInvitationRequestException("Company does not exist"));

        // check if active
        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new InvalidInvitationRequestException("Invitations can only be created for active companies");
        }

        // find user that initialized invitation
        User invitedBy = resolveCurrentAuthenticatedUser(company.getId());
        // find role assignment
        RoleAssignment inviterRoleAssignment = resolveActiveRoleAssignment(invitedBy.getId(), company.getId());
        validateInviterAuthorization(inviterRoleAssignment);

        String normalizedEmail = request.email().trim().toLowerCase();
        PlatformAccess resolvedPlatformAccess = resolveInvitationPlatformAccess(request.platformRole(), request.platformAccess());
        InvitationKind invitationKind = resolveInvitationKind(request.platformRole());

        // check if the invitation exists
        if (userInvitationRepository.existsByCompanyIdAndEmailIgnoreCaseAndUsedAtIsNullAndExpiresAtAfter(
                company.getId(), normalizedEmail, Instant.now())) {
            throw new InvitationAlreadyExistsException(normalizedEmail);
        }

        User user = userRepository.findByCompanyIdAndEmailIgnoreCase(company.getId(), normalizedEmail)
                .map(existingUser -> validateExistingUser(existingUser, normalizedEmail))
                .orElseGet(() -> createPendingUser(company, normalizedEmail));

        applySharedIdentityFields(user, request);
        User savedUser = Objects.requireNonNull(userRepository.save(user));

        RoleAssignment roleAssignment = upsertRoleAssignment(savedUser, company, request, resolvedPlatformAccess, invitedBy);
        synchronizePermissions(roleAssignment, inviterRoleAssignment, request);

        String rawToken = secureTokenGenerator.generateToken();
        String tokenHash = sha256TokenHashUtility.hash(rawToken);
        Instant expiresAt = Instant.now().plusSeconds(24 * 60 * 60L);

        UserInvitation invitation = new UserInvitation();
        invitation.setCompany(company);
        invitation.setUser(savedUser);
        invitation.setEmail(normalizedEmail);
        invitation.setTokenHash(tokenHash);
        invitation.setInvitedBy(invitedBy);
        invitation.setPlatformRole(request.platformRole());
        invitation.setPlatformAccess(resolvedPlatformAccess);
        invitation.setInvitationKind(invitationKind);
        invitation.setInvitedJobTitle(resolveInvitedJobTitle(request.platformRole(), request.invitedJobTitle()));
        invitation.setExpiresAt(expiresAt);

        UserInvitation savedInvitation = userInvitationRepository.save(invitation);
        invitationEmailService.sendInvitationEmail(
                company,
                normalizedEmail,
                savedUser.getDisplayName(),
                savedInvitation.getPlatformRole(),
                savedInvitation.getInvitationKind(),
                buildActivationLink(rawToken),
                savedUser.getPreferredLanguage().getCode());

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
                inviterRoleAssignment.getId(),
                inviterRoleAssignment.getRole(),
                inviterRoleAssignment.getJobTitle(),
                "INVITATION_CREATED",
                "UserInvitation",
                savedInvitation.getId(),
                buildInvitationAuditDiff(savedInvitation, request, resolvedPlatformAccess),
                Map.of(
                        "companyId", company.getId(),
                        "invitedByUserId", invitedBy.getId(),
                        "userId", savedUser.getId(),
                        "roleAssignmentId", roleAssignment.getId()
                ),
                null
        ));

        return new CreateInvitationResponse(
                savedInvitation.getId(),
                savedUser.getId(),
                roleAssignment.getId(),
                savedInvitation.getEmail(),
                savedInvitation.getPlatformRole(),
                resolvedPlatformAccess,
                savedInvitation.getExpiresAt(),
                rawToken,
                "Invitation created successfully"
        );
    }

    // Validation

    private void validateRequest(CreateInvitationRequest request) {
        if (request == null) {
            throw new InvalidInvitationRequestException("Invitation request must not be null");
        }
        if (request.companyId() == null) {
            throw new InvalidInvitationRequestException("companyId is required");
        }
        if (!StringUtils.hasText(request.firstName())) {
            throw new InvalidInvitationRequestException("firstName is required");
        }
        if (!StringUtils.hasText(request.lastName())) {
            throw new InvalidInvitationRequestException("lastName is required");
        }
        if (!StringUtils.hasText(request.email())) {
            throw new InvalidInvitationRequestException("email is required");
        }
        if (request.platformRole() == null) {
            throw new InvalidInvitationRequestException("platformRole is required");
        }
        if (request.platformAccess() == null) {
            throw new InvalidInvitationRequestException("platformAccess is required");
        }
        if (request.platformRole() == PlatformRole.STAFF && !StringUtils.hasText(request.invitedJobTitle())) {
            throw new InvalidInvitationRequestException("invitedJobTitle is required for STAFF invitations");
        }
    }

    // User helpers

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
        user.setPreferredLanguage(Language.SQ);
        user.setFailedLoginCount((short) 0);
        return userRepository.save(user);
    }

    private void applySharedIdentityFields(User user, CreateInvitationRequest request) {
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setDisplayName((request.firstName().trim() + " " + request.lastName().trim()).trim());
        user.setPhoneNumber(trimToNull(request.phoneNumber()));
        
        user.setPreferredLanguage(Language.fromCode(request.preferredLanguage()));
    }

    // Role assignment

    private RoleAssignment upsertRoleAssignment(
            User user, Company company, CreateInvitationRequest request,
            PlatformAccess platformAccess, User invitedBy) {

        RoleAssignment ra = roleAssignmentRepository
                .findFirstByUserIdAndCompanyIdAndIsActiveTrue(user.getId(), company.getId())
                .orElseGet(RoleAssignment::new);

        ra.setCompany(company);
        ra.setUser(user);
        ra.setRole(request.platformRole());
        ra.setJobTitle(resolveInvitedJobTitle(request.platformRole(), request.invitedJobTitle()));
        ra.setIsActive(true);
        ra.setPlatformAccess(platformAccess);
        ra.setCreatedBy(invitedBy);
        if (ra.getActivatedAt() == null) {
            ra.setActivatedAt(Instant.now());
        }

        return roleAssignmentRepository.save(ra);
    }

    // Permission synchronization

    private void synchronizePermissions(
            RoleAssignment roleAssignment,
            RoleAssignment inviterRoleAssignment,
            CreateInvitationRequest request) {

        roleAssignmentPermissionRepository.deleteAllByRoleAssignmentId(roleAssignment.getId());

        if (request.platformRole() != PlatformRole.STAFF) {
            return;
        }

        List<String> codes = request.permissionCodes() == null
                ? List.of()
                : request.permissionCodes().stream()
                        .filter(StringUtils::hasText)
                        .map(String::trim)
                        .distinct()
                        .toList();

        if (codes.isEmpty()) {
            return;
        }

        List<Permission> permissions = permissionRepository.findAllByCodeIn(codes);
        if (permissions.size() != codes.size()) {
            throw new InvalidInvitationRequestException("One or more permission codes are invalid");
        }

        for (Permission permission : permissions) {
            RoleAssignmentPermission rap = new RoleAssignmentPermission();
            rap.setRoleAssignment(roleAssignment);
            rap.setPermission(permission);
            rap.setIsGranted(true);
            rap.setGrantedBy(inviterRoleAssignment.getUser());
            roleAssignmentPermissionRepository.save(rap);
        }
    }

    // Auth resolution

    private User resolveCurrentAuthenticatedUser(java.util.UUID companyId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new InvalidInvitationRequestException("Authenticated inviter could not be resolved");
        }
        String principal = authentication.getName();
        if (!StringUtils.hasText(principal)) {
            throw new InvalidInvitationRequestException("Authenticated inviter could not be resolved");
        }
        return userRepository.findByCompanyIdAndEmailIgnoreCase(companyId, principal.trim().toLowerCase())
                .orElseThrow(() -> new InvalidInvitationRequestException(
                        "Authenticated inviter does not belong to the target company"));
    }

    private RoleAssignment resolveActiveRoleAssignment(java.util.UUID userId, java.util.UUID companyId) {
        return roleAssignmentRepository.findFirstByUserIdAndCompanyIdAndIsActiveTrue(userId, companyId)
                .orElseThrow(() -> new InvalidInvitationRequestException("Inviter must have an active role assignment"));
    }

    private void validateInviterAuthorization(RoleAssignment inviterRoleAssignment) {
        if (inviterRoleAssignment.getRole() != PlatformRole.ADMIN
                && inviterRoleAssignment.getRole() != PlatformRole.SUPERADMIN) {
            throw new InvalidInvitationRequestException("Only admins can send invitations");
        }
    }

    // Resolvers / helpers

    private PlatformAccess resolveInvitationPlatformAccess(PlatformRole role, PlatformAccess requested) {
        return switch (role) {
            case EMPLOYEE -> PlatformAccess.MOBILE;
            case ADMIN, SUPERADMIN -> PlatformAccess.WEB;
            case STAFF -> requested;
        };
    }

    private InvitationKind resolveInvitationKind(PlatformRole role) {
        return switch (role) {
            case STAFF -> InvitationKind.STAFF_INVITATION;
            case EMPLOYEE -> InvitationKind.EMPLOYEE_INVITATION;
            case ADMIN, SUPERADMIN -> InvitationKind.INITIAL_ADMIN_ACTIVATION;
        };
    }

    private String resolveInvitedJobTitle(PlatformRole role, String requestedJobTitle) {
        if (role == PlatformRole.STAFF && !StringUtils.hasText(requestedJobTitle)) {
            throw new InvalidInvitationRequestException("invitedJobTitle is required for STAFF invitations");
        }
        return StringUtils.hasText(requestedJobTitle) ? requestedJobTitle.trim() : null;
    }

    private Map<String, Object> buildInvitationAuditDiff(
            UserInvitation invitation, CreateInvitationRequest request, PlatformAccess platformAccess) {
        Map<String, Object> diff = new LinkedHashMap<>();
        diff.put("email", invitation.getEmail());
        diff.put("platformRole", invitation.getPlatformRole().name());
        diff.put("platformAccess", platformAccess.name());
        diff.put("invitationKind", invitation.getInvitationKind().name());
        diff.put("expiresAt", invitation.getExpiresAt().toString());
        diff.put("firstName", request.firstName().trim());
        diff.put("lastName", request.lastName().trim());
        if (StringUtils.hasText(invitation.getInvitedJobTitle())) {
            diff.put("invitedJobTitle", invitation.getInvitedJobTitle());
        }
        if (request.permissionCodes() != null && !request.permissionCodes().isEmpty()) {
            diff.put("permissionCodes", request.permissionCodes());
        }
        return diff;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String buildActivationLink(String rawToken) {
        return activationLinkBase + "?token=" + rawToken;
    }
}
