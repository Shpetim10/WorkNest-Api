package com.worknest.auth.service.impl;

import com.worknest.auth.domain.Company;
import com.worknest.auth.domain.CompanyStatus;
import com.worknest.auth.domain.PlatformAccess;
import com.worknest.auth.domain.RefreshToken;
import com.worknest.auth.domain.RoleAssignment;
import com.worknest.auth.domain.User;
import com.worknest.auth.domain.UserStatus;
import com.worknest.auth.dto.AvailableLoginContextDto;
import com.worknest.auth.dto.LoginRequest;
import com.worknest.auth.dto.LoginResponse;
import com.worknest.auth.exception.AuthenticationFailedException;
import com.worknest.auth.exception.InvalidCredentialsException;
import com.worknest.auth.exception.NoPlatformAccessException;
import com.worknest.auth.repository.CompanyRepository;
import com.worknest.auth.repository.RefreshTokenRepository;
import com.worknest.auth.repository.RoleAssignmentRepository;
import com.worknest.auth.repository.UserRepository;
import com.worknest.auth.service.AuthLoginService;
import com.worknest.auth.utility.SecureTokenGenerator;
import com.worknest.auth.utility.Sha256TokenHashUtility;
import com.worknest.security.config.JwtProperties;
import com.worknest.security.service.JwtService;
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
                .orElseThrow(InvalidCredentialsException::new);
        validateCompany(company);

        User user = userRepository.findByCompanyIdAndEmailIgnoreCase(company.getId(), normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);
        validateUserBeforePasswordCheck(user, now);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            handleFailedLogin(user, now);
            throw new InvalidCredentialsException();
        }

        List<RoleAssignment> validAssignments = roleAssignmentRepository
                .findAllByUserIdAndCompanyIdAndIsActiveTrue(user.getId(), company.getId())
                .stream()
                .filter(assignment -> isAssignmentValidForPlatform(assignment, request.platformAccess()))
                .toList();

        if (validAssignments.isEmpty()) {
            throw new NoPlatformAccessException(request.platformAccess().name());
        }

        user.setFailedLoginCount((short) 0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        user.setLastLoginIp(trimToNull(ipAddress));
        userRepository.save(user);

        if (validAssignments.size() > 1) {
            return new LoginResponse(
                    true,
                    true,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    request.platformAccess(),
                    validAssignments.stream()
                            .map(assignment -> toAvailableContext(company, assignment, request.platformAccess()))
                            .toList(),
                    "Role selection required"
            );
        }

        RoleAssignment activeRoleAssignment = validAssignments.get(0);
        Instant accessTokenExpiresAt = now.plus(jwtProperties.getAccessTokenExpiry());
        Instant refreshTokenExpiresAt = now.plus(jwtProperties.getRefreshTokenExpiry());

        String accessToken = jwtService.generateAccessToken(user, activeRoleAssignment, request.platformAccess(), now, accessTokenExpiresAt);

        String rawRefreshToken = secureTokenGenerator.generateToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(sha256TokenHashUtility.hash(rawRefreshToken));
        refreshToken.setActiveRoleAssignment(activeRoleAssignment);
        refreshToken.setPlatformAccess(request.platformAccess());
        refreshToken.setIssuedAt(now);
        refreshToken.setExpiresAt(refreshTokenExpiresAt);
        refreshToken.setIpAddress(trimToNull(ipAddress));
        refreshToken.setUserAgent(trimToNull(userAgent));
        refreshTokenRepository.save(refreshToken);

        // TODO: Publish login audit/platform event

        return new LoginResponse(
                true,
                false,
                accessToken,
                accessTokenExpiresAt,
                rawRefreshToken,
                refreshTokenExpiresAt,
                activeRoleAssignment.getId(),
                activeRoleAssignment.getRole(),
                request.platformAccess(),
                List.of(toAvailableContext(company, activeRoleAssignment, request.platformAccess())),
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

    private void validateCompany(Company company) {
        if (company.getStatus() == CompanyStatus.SUSPENDED) {
            throw new AuthenticationFailedException("COMPANY_SUSPENDED", "Company access is suspended");
        }
        if (company.getStatus() == CompanyStatus.DELETED || company.getDeletedAt() != null) {
            throw new AuthenticationFailedException("COMPANY_DELETED", "Company account is deleted");
        }
    }

    private void validateUserBeforePasswordCheck(User user, Instant now) {
        if (user.getStatus() == UserStatus.PENDING) {
            throw new AuthenticationFailedException("USER_PENDING", "User account is pending activation");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AuthenticationFailedException("USER_SUSPENDED", "User account is suspended");
        }
        if (user.getStatus() == UserStatus.DEACTIVATED) {
            throw new AuthenticationFailedException("USER_DEACTIVATED", "User account is deactivated");
        }
        if (!StringUtils.hasText(user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw new AuthenticationFailedException("ACCOUNT_LOCKED", "Account is temporarily locked");
        }
    }

    private void handleFailedLogin(User user, Instant now) {
        short failedAttempts = (short) (user.getFailedLoginCount() + 1);
        user.setFailedLoginCount(failedAttempts);
        if (failedAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
            user.setLockedUntil(now.plus(failedLoginLockDuration));
        }
        userRepository.save(user);
    }

    private boolean isAssignmentValidForPlatform(RoleAssignment assignment, PlatformAccess requestedPlatformAccess) {
        if (!Boolean.TRUE.equals(assignment.getIsActive())) {
            return false;
        }
        // Match the requested platform with the assignment's allowed platform
        return assignment.getPlatformAccess() == requestedPlatformAccess;
    }

    private AvailableLoginContextDto toAvailableContext(
            Company company,
            RoleAssignment assignment,
            PlatformAccess platformAccess
    ) {
        return new AvailableLoginContextDto(
                company.getId(),
                company.getName(),
                assignment.getId(),
                assignment.getRole(),
                assignment.getJobTitle(),
                platformAccess
        );
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
