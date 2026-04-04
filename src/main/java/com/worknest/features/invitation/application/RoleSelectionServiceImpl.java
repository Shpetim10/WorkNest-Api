package com.worknest.features.invitation.application;

import com.worknest.domain.entities.Company;
import com.worknest.domain.enums.CompanyStatus;
import com.worknest.domain.enums.PlatformAccess;
import com.worknest.domain.entities.RefreshToken;
import com.worknest.domain.entities.RoleAssignment;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.UserStatus;
import com.worknest.features.invitation.dto.SelectRoleRequest;
import com.worknest.features.invitation.dto.SelectRoleResponse;
import com.worknest.features.auth.dto.TenantContextDto;
import com.worknest.features.auth.exception.AuthenticationFailedException;
import com.worknest.features.auth.exception.NoPlatformAccessException;
import com.worknest.features.auth.repository.RefreshTokenRepository;
import com.worknest.features.auth.repository.RoleAssignmentRepository;
import com.worknest.features.auth.repository.RefreshTokenRepository;
import com.worknest.features.auth.repository.RoleAssignmentRepository;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.invitation.application.RoleSelectionService;
import com.worknest.features.auth.utility.SecureTokenGenerator;
import com.worknest.features.auth.utility.Sha256TokenHashUtility;
import com.worknest.security.config.JwtProperties;
import com.worknest.security.service.JwtService;
import com.worknest.audit.service.AuthAuditService;
import com.worknest.audit.service.model.AuthAuditActorContext;
import com.worknest.audit.service.model.AuthSessionContext;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleSelectionServiceImpl implements RoleSelectionService {

    private final RoleAssignmentRepository roleAssignmentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final SecureTokenGenerator secureTokenGenerator;
    private final Sha256TokenHashUtility sha256TokenHashUtility;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthAuditService authAuditService;

    @Override
    @Transactional
    public SelectRoleResponse selectRole(SelectRoleRequest request, String userId, String ipAddress, String userAgent) {
        validateRequest(request, userId);

        UUID userUuid = UUID.fromString(userId);
        Instant now = Instant.now();

        log.info(
                "User {} selecting role assignment {} for platform {}",
                userId,
                request.roleAssignmentId(),
                request.platformAccess()
        );

        User user = userRepository.findById(Objects.requireNonNull(userUuid))
                .orElseThrow(() -> new AuthenticationFailedException("USER_NOT_FOUND", "Authenticated user was not found"));
        validateUser(user);

        RoleAssignment assignment = roleAssignmentRepository.findById(Objects.requireNonNull(request.roleAssignmentId()))
                .orElseThrow(() -> new AuthenticationFailedException("ROLE_ASSIGNMENT_NOT_FOUND", "Role assignment was not found"));

        validateAssignmentOwnership(assignment, userUuid);
        validateCompany(assignment.getCompany());
        validateAssignmentStatus(assignment);
        validatePlatformAccess(assignment, request.platformAccess());

        Instant accessTokenExpiresAt = now.plus(jwtProperties.getAccessTokenExpiry());
        Instant refreshTokenExpiresAt = now.plus(jwtProperties.getRefreshTokenExpiry());

        String accessToken = jwtService.generateAccessToken(user, assignment, request.platformAccess(), now, accessTokenExpiresAt);

        String rawRefreshToken = secureTokenGenerator.generateToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(sha256TokenHashUtility.hash(rawRefreshToken));
        refreshToken.setActiveRoleAssignment(assignment);
        refreshToken.setIssuedAt(now);
        refreshToken.setExpiresAt(refreshTokenExpiresAt);
        refreshToken.setIpAddress(trimToNull(ipAddress));
        refreshToken.setUserAgent(trimToNull(userAgent));
        refreshToken.setPlatformAccess(request.platformAccess());
        refreshTokenRepository.save(refreshToken);

        // Emit platform events & audit
        AuthAuditActorContext actorContext = new AuthAuditActorContext(
                assignment.getCompany().getId(),
                assignment.getCompany().getName(),
                user.getId(),
                assignment.getId(),
                assignment.getRole(),
                assignment.getJobTitle(),
                ipAddress
        );

        AuthSessionContext sessionContext = new AuthSessionContext(
                user.getId(),
                assignment.getId(),
                assignment.getRole(),
                request.platformAccess()
        );

        authAuditService.appendRoleSelected(actorContext, sessionContext, userAgent);

        return new SelectRoleResponse(
                assignment.getId(),
                assignment.getRole(),
                accessToken,
                accessTokenExpiresAt,
                rawRefreshToken,
                toTenantContext(assignment.getCompany()),
                refreshTokenExpiresAt
        );
    }

    private void validateRequest(SelectRoleRequest request, String userId) {
        if (request == null || request.roleAssignmentId() == null || request.platformAccess() == null) {
            throw new AuthenticationFailedException("INVALID_ROLE_SELECTION", "Role selection request is invalid");
        }
        if (!StringUtils.hasText(userId)) {
            throw new AuthenticationFailedException("INVALID_AUTH_CONTEXT", "Authenticated user context is missing");
        }
    }

    private void validateUser(User user) {
        if (user.getStatus() == UserStatus.PENDING) {
            throw new AuthenticationFailedException("USER_PENDING", "User account is pending activation");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AuthenticationFailedException("USER_SUSPENDED", "User account is suspended");
        }
        if (user.getStatus() == UserStatus.DEACTIVATED) {
            throw new AuthenticationFailedException("USER_DEACTIVATED", "User account is deactivated");
        }
    }

    private void validateCompany(Company company) {
        if (company.getStatus() == CompanyStatus.SUSPENDED) {
            throw new AuthenticationFailedException("COMPANY_SUSPENDED", "Company access is suspended");
        }
        if (company.getStatus() == CompanyStatus.DELETED || company.getDeletedAt() != null) {
            throw new AuthenticationFailedException("COMPANY_DELETED", "Company account is deleted");
        }
    }

    private void validateAssignmentOwnership(RoleAssignment assignment, UUID userId) {
        if (!assignment.getUser().getId().equals(userId)) {
            throw new AuthenticationFailedException(
                    "ROLE_ASSIGNMENT_FORBIDDEN",
                    "Selected role assignment does not belong to the authenticated user"
            );
        }
        if (!assignment.getCompany().getId().equals(assignment.getUser().getCompany().getId())) {
            throw new AuthenticationFailedException(
                    "ROLE_ASSIGNMENT_COMPANY_MISMATCH",
                    "Selected role assignment does not match the authenticated user's company"
            );
        }
    }

    private void validateAssignmentStatus(RoleAssignment assignment) {
        if (!Boolean.TRUE.equals(assignment.getIsActive())) {
            throw new AuthenticationFailedException("ROLE_ASSIGNMENT_INACTIVE", "Selected role assignment is not active");
        }
    }

    private void validatePlatformAccess(RoleAssignment assignment, PlatformAccess requestedPlatformAccess) {
        if (!isAssignmentValidForPlatform(assignment, requestedPlatformAccess)) {
            throw new NoPlatformAccessException(requestedPlatformAccess.name());
        }
    }

    private boolean isAssignmentValidForPlatform(RoleAssignment assignment, PlatformAccess requestedPlatformAccess) {
        if (assignment.getPlatformAccess() == null || requestedPlatformAccess == null) {
            return false;
        }

        return switch (assignment.getRole()) {
            case ADMIN, SUPERADMIN -> requestedPlatformAccess == PlatformAccess.WEB
                    && supportsRequestedPlatform(assignment.getPlatformAccess(), requestedPlatformAccess);
            case EMPLOYEE -> requestedPlatformAccess == PlatformAccess.MOBILE
                    && supportsRequestedPlatform(assignment.getPlatformAccess(), requestedPlatformAccess);
            case STAFF -> supportsRequestedPlatform(assignment.getPlatformAccess(), requestedPlatformAccess);
        };
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private boolean supportsRequestedPlatform(PlatformAccess assignmentPlatformAccess, PlatformAccess requestedPlatformAccess) {
        if (assignmentPlatformAccess == PlatformAccess.BOTH) {
            return true;
        }
        if (requestedPlatformAccess == PlatformAccess.BOTH) {
            return assignmentPlatformAccess == PlatformAccess.BOTH;
        }
        return assignmentPlatformAccess == requestedPlatformAccess;
    }

    private TenantContextDto toTenantContext(Company company) {
        return new TenantContextDto(
                company.getId(),
                company.getName(),
                company.getSlug(),
                company.getStatus(),
                company.getLogoPath(),
                company.getTimezone(),
                company.getLocale(),
                company.getCurrency(),
                company.getDateFormat(),
                company.getOnboardingCompletedAt(),
                company.getSubscriptionPlan(),
                company.getSubscriptionStatus()
        );
    }
}
