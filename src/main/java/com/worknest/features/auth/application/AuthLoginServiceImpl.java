package com.worknest.features.auth.application;

import com.worknest.domain.entities.Company;
import com.worknest.domain.enums.CompanyStatus;
import com.worknest.domain.enums.PlatformAccess;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.domain.entities.RefreshToken;
import com.worknest.domain.entities.RoleAssignment;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.UserStatus;
import com.worknest.features.auth.dto.AvailableLoginContextDto;
import com.worknest.features.auth.dto.LoginRequest;
import com.worknest.features.auth.dto.LoginResponse;
import com.worknest.features.auth.dto.TenantContextDto;
import com.worknest.features.auth.exception.AuthenticationFailedException;
import com.worknest.features.auth.exception.InvalidCredentialsException;
import com.worknest.features.auth.exception.NoPlatformAccessException;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.auth.repository.RefreshTokenRepository;
import com.worknest.features.auth.dto.AvailableLoginContextDto;
import com.worknest.features.auth.dto.LoginRequest;
import com.worknest.features.auth.dto.LoginResponse;
import com.worknest.features.auth.dto.TenantContextDto;
import com.worknest.features.auth.exception.AuthenticationFailedException;
import com.worknest.features.auth.exception.InvalidCredentialsException;
import com.worknest.features.auth.exception.NoPlatformAccessException;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.auth.repository.RefreshTokenRepository;
import com.worknest.features.auth.repository.RoleAssignmentRepository;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.auth.application.AuthLoginService;
import com.worknest.features.auth.utility.SecureTokenGenerator;
import com.worknest.features.auth.utility.Sha256TokenHashUtility;
import com.worknest.security.config.JwtProperties;
import com.worknest.security.service.JwtService;
import com.worknest.audit.service.AuthAuditService;
import com.worknest.audit.service.model.AuthAuditActorContext;
import com.worknest.audit.service.model.AuthSessionContext;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthLoginServiceImpl implements AuthLoginService {

    private static final short MAX_FAILED_LOGIN_ATTEMPTS = 5;

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureTokenGenerator secureTokenGenerator;
    private final Sha256TokenHashUtility sha256TokenHashUtility;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthAuditService authAuditService;

    @Value("${app.security.auth.failed-login-lock-duration:PT15M}")
    private Duration failedLoginLockDuration;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        validateRequest(request);

        String normalizedSlug = request.companySlug().trim().toLowerCase();
        String normalizedEmail = request.email().trim().toLowerCase();
        Instant now = Instant.now();

        log.info("Attempting login for user: {} in company slug: {}", normalizedEmail, normalizedSlug);

        Company company = companyRepository.findBySlugIgnoreCase(normalizedSlug)
                .orElseThrow(() -> {
                    authAuditService.appendLoginFailure(
                            null, null, normalizedSlug,
                            normalizedEmail, request.platformAccess(), "COMPANY_NOT_FOUND", ipAddress
                    );
                    return new InvalidCredentialsException();
                });
        validateCompany(company, normalizedEmail, request.platformAccess(), ipAddress);

        User user = userRepository.findByCompanyIdAndEmailIgnoreCase(company.getId(), normalizedEmail)
                .orElseThrow(() -> {
                    authAuditService.appendLoginFailure(
                            company.getId(), company.getName(), normalizedSlug,
                            normalizedEmail, request.platformAccess(), "USER_NOT_FOUND", ipAddress
                    );
                    return new InvalidCredentialsException();
                });
        validateUserBeforePasswordCheck(user, normalizedEmail, request.platformAccess(), ipAddress, now);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            handleFailedLogin(user, normalizedEmail, request.platformAccess(), ipAddress, now);
            throw new InvalidCredentialsException();
        }

        List<RoleAssignment> validAssignments = roleAssignmentRepository
                .findAllByUserIdAndCompanyIdAndIsActiveTrue(user.getId(), company.getId())
                .stream()
                .filter(assignment -> isAssignmentValidForPlatform(assignment, request.platformAccess()))
                .toList();

        if (validAssignments.isEmpty()) {
            authAuditService.appendLoginFailure(
                    company.getId(), company.getName(), normalizedSlug,
                    normalizedEmail, request.platformAccess(), "NO_PLATFORM_ACCESS", ipAddress
            );
            throw new NoPlatformAccessException(request.platformAccess().name());
        }

        user.setFailedLoginCount((short) 0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        user.setLastLoginIp(trimToNull(ipAddress));
        userRepository.save(user);

        RoleAssignment sessionRoleAssignment = validAssignments.get(0);
        Instant accessTokenExpiresAt = now.plus(jwtProperties.getAccessTokenExpiry());
        Instant refreshTokenExpiresAt = now.plus(jwtProperties.getRefreshTokenExpiry());

        String accessToken = jwtService.generateAccessToken(
                user,
                sessionRoleAssignment,
                request.platformAccess(),
                now,
                accessTokenExpiresAt
        );

        String rawRefreshToken = secureTokenGenerator.generateToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(sha256TokenHashUtility.hash(rawRefreshToken));
        refreshToken.setActiveRoleAssignment(sessionRoleAssignment);
        refreshToken.setPlatformAccess(request.platformAccess());
        refreshToken.setIssuedAt(now);
        refreshToken.setExpiresAt(refreshTokenExpiresAt);
        refreshToken.setIpAddress(trimToNull(ipAddress));
        refreshToken.setUserAgent(trimToNull(userAgent));
        refreshTokenRepository.save(refreshToken);

        if (validAssignments.size() > 1) {
            return new LoginResponse(
                    true,
                    true,
                    accessToken,
                    accessTokenExpiresAt,
                    rawRefreshToken,
                    refreshTokenExpiresAt,
                    sessionRoleAssignment.getId(),
                    sessionRoleAssignment.getRole(),
                    request.platformAccess(),
                    toTenantContext(sessionRoleAssignment.getCompany()),
                    validAssignments.stream()
                            .map(assignment -> toAvailableContext(company, assignment, request.platformAccess()))
                            .toList(),
                    "Role selection required"
            );
        }

        // 5. Emit platform events & audit
        AuthAuditActorContext actorContext = new AuthAuditActorContext(
                company.getId(),
                company.getName(),
                user.getId(),
                sessionRoleAssignment.getId(),
                sessionRoleAssignment.getRole(),
                sessionRoleAssignment.getJobTitle(),
                ipAddress
        );

        AuthSessionContext sessionContext = new AuthSessionContext(
                user.getId(),
                sessionRoleAssignment.getId(),
                sessionRoleAssignment.getRole(),
                request.platformAccess()
        );

        authAuditService.appendLoginSuccess(actorContext, sessionContext, normalizedEmail, userAgent);

        return new LoginResponse(
                true,
                false,
                accessToken,
                accessTokenExpiresAt,
                rawRefreshToken,
                refreshTokenExpiresAt,
                sessionRoleAssignment.getId(),
                sessionRoleAssignment.getRole(),
                request.platformAccess(),
                toTenantContext(sessionRoleAssignment.getCompany()),
                List.of(toAvailableContext(company, sessionRoleAssignment, request.platformAccess())),
                "Login successful"
        );
    }

    private void validateRequest(LoginRequest request) {
        if (request == null) {
            throw new InvalidCredentialsException();
        }
        if (!StringUtils.hasText(request.companySlug())
                || !StringUtils.hasText(request.email())
                || !StringUtils.hasText(request.password())
                || request.platformAccess() == null) {
            throw new InvalidCredentialsException();
        }
    }

    private void validateCompany(Company company, String email, PlatformAccess platformAccess, String ipAddress) {
        if (company.getStatus() == CompanyStatus.SUSPENDED) {
            authAuditService.appendLoginFailure(
                    company.getId(), company.getName(), company.getSlug(),
                    email, platformAccess, "COMPANY_SUSPENDED", ipAddress
            );
            throw new AuthenticationFailedException("COMPANY_SUSPENDED", "Company access is suspended");
        }
        if (company.getStatus() == CompanyStatus.DELETED || company.getDeletedAt() != null) {
            authAuditService.appendLoginFailure(
                    company.getId(), company.getName(), company.getSlug(),
                    email, platformAccess, "COMPANY_DELETED", ipAddress
            );
            throw new AuthenticationFailedException("COMPANY_DELETED", "Company account is deleted");
        }
    }

    private void validateUserBeforePasswordCheck(User user, String email, PlatformAccess platformAccess, String ipAddress, Instant now) {
        if (user.getStatus() == UserStatus.PENDING) {
            authAuditService.appendLoginFailure(
                    user.getCompany().getId(), user.getCompany().getName(), user.getCompany().getSlug(),
                    email, platformAccess, "USER_PENDING", ipAddress
            );
            throw new AuthenticationFailedException("USER_PENDING", "User account is pending activation");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            authAuditService.appendLoginFailure(
                    user.getCompany().getId(), user.getCompany().getName(), user.getCompany().getSlug(),
                    email, platformAccess, "USER_SUSPENDED", ipAddress
            );
            throw new AuthenticationFailedException("USER_SUSPENDED", "User account is suspended");
        }
        if (user.getStatus() == UserStatus.DEACTIVATED) {
            authAuditService.appendLoginFailure(
                    user.getCompany().getId(), user.getCompany().getName(), user.getCompany().getSlug(),
                    email, platformAccess, "USER_DEACTIVATED", ipAddress
            );
            throw new AuthenticationFailedException("USER_DEACTIVATED", "User account is deactivated");
        }
        if (!StringUtils.hasText(user.getPasswordHash())) {
            authAuditService.appendLoginFailure(
                    user.getCompany().getId(), user.getCompany().getName(), user.getCompany().getSlug(),
                    email, platformAccess, "MISSING_PASSWORD", ipAddress
            );
            throw new InvalidCredentialsException();
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            authAuditService.appendLoginFailure(
                    user.getCompany().getId(), user.getCompany().getName(), user.getCompany().getSlug(),
                    email, platformAccess, "ACCOUNT_LOCKED", ipAddress
            );
            throw new AuthenticationFailedException("ACCOUNT_LOCKED", "Account is temporarily locked");
        }
    }

    private void handleFailedLogin(User user, String email, PlatformAccess platformAccess, String ipAddress, Instant now) {
        short failedAttempts = (short) (user.getFailedLoginCount() + 1);
        user.setFailedLoginCount(failedAttempts);

        String failureReason = "INVALID_CREDENTIALS";
        if (failedAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
            user.setLockedUntil(now.plus(failedLoginLockDuration));
            failureReason = "MAX_ATTEMPTS_REACHED";
        }

        authAuditService.appendLoginFailure(
                user.getCompany().getId(), user.getCompany().getName(), user.getCompany().getSlug(),
                email, platformAccess, failureReason, ipAddress
        );

        userRepository.save(user);
    }

    private boolean isAssignmentValidForPlatform(RoleAssignment assignment, PlatformAccess requestedPlatformAccess) {
        if (!Boolean.TRUE.equals(assignment.getIsActive())) {
            return false;
        }

        PlatformAccess assignmentPlatformAccess = assignment.getPlatformAccess();
        if (assignmentPlatformAccess == null || requestedPlatformAccess == null) {
            return false;
        }

        return switch (assignment.getRole()) {
            case ADMIN, SUPERADMIN -> requestedPlatformAccess == PlatformAccess.WEB
                    && supportsRequestedPlatform(assignmentPlatformAccess, requestedPlatformAccess);
            case EMPLOYEE -> requestedPlatformAccess == PlatformAccess.MOBILE
                    && supportsRequestedPlatform(assignmentPlatformAccess, requestedPlatformAccess);
            case STAFF -> supportsRequestedPlatform(assignmentPlatformAccess, requestedPlatformAccess);
        };
    }

    private AvailableLoginContextDto toAvailableContext(
            Company company,
            RoleAssignment assignment,
            PlatformAccess platformAccess
    ) {
        return new AvailableLoginContextDto(
                company.getId(),
                company.getName(),
                company.getSlug(),
                assignment.getId(),
                assignment.getRole(),
                assignment.getJobTitle(),
                platformAccess
        );
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

    private boolean supportsRequestedPlatform(PlatformAccess assignmentPlatformAccess, PlatformAccess requestedPlatformAccess) {
        if (assignmentPlatformAccess == PlatformAccess.BOTH) {
            return true;
        }
        if (requestedPlatformAccess == PlatformAccess.BOTH) {
            return assignmentPlatformAccess == PlatformAccess.BOTH;
        }
        return assignmentPlatformAccess == requestedPlatformAccess;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
