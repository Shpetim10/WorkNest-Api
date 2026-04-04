package com.worknest.features.auth.application;

import com.worknest.domain.entities.Company;
import com.worknest.domain.enums.CompanyStatus;
import com.worknest.domain.entities.RefreshToken;
import com.worknest.domain.entities.RoleAssignment;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.UserStatus;
import com.worknest.features.auth.dto.RefreshTokenRequest;
import com.worknest.features.auth.dto.RefreshTokenResponse;
import com.worknest.features.auth.exception.AuthenticationFailedException;
import com.worknest.features.auth.repository.RefreshTokenRepository;
import com.worknest.features.auth.application.RefreshTokenService;
import com.worknest.features.auth.utility.SecureTokenGenerator;
import com.worknest.features.auth.utility.Sha256TokenHashUtility;
import com.worknest.security.config.JwtProperties;
import com.worknest.security.service.JwtService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final String ROTATION_REASON = "rotation";

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final SecureTokenGenerator secureTokenGenerator;
    private final Sha256TokenHashUtility sha256TokenHashUtility;

    @Override
    @Transactional
    public RefreshTokenResponse refresh(String rawRefreshToken, String ipAddress, String userAgent) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            throw new AuthenticationFailedException("INVALID_REFRESH_TOKEN", "Refresh token is required");
        }

        Instant now = Instant.now();
        String tokenHash = sha256TokenHashUtility.hash(rawRefreshToken);

        log.info("Refreshing session using hashed refresh token lookup");

        RefreshToken existingToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthenticationFailedException(
                        "INVALID_REFRESH_TOKEN",
                        "Refresh token is invalid"
                ));

        validateRefreshToken(existingToken, now);

        RoleAssignment activeRoleAssignment = existingToken.getActiveRoleAssignment();
        validateRoleAssignment(activeRoleAssignment);

        User user = existingToken.getUser();
        validateUser(user);
        validateCompany(user.getCompany());

        Instant accessTokenExpiresAt = now.plus(jwtProperties.getAccessTokenExpiry());
        Instant refreshTokenExpiresAt = now.plus(jwtProperties.getRefreshTokenExpiry());

        String accessToken = jwtService.generateAccessToken(
                user,
                activeRoleAssignment,
                existingToken.getPlatformAccess(),
                now,
                accessTokenExpiresAt
        );

        refreshTokenRepository.revokeById(existingToken.getId(), now, ROTATION_REASON);

        String newRawRefreshToken = secureTokenGenerator.generateToken();
        RefreshToken rotatedToken = new RefreshToken();
        rotatedToken.setUser(user);
        rotatedToken.setTokenHash(sha256TokenHashUtility.hash(newRawRefreshToken));
        rotatedToken.setActiveRoleAssignment(activeRoleAssignment);
        rotatedToken.setPlatformAccess(existingToken.getPlatformAccess());
        rotatedToken.setIssuedAt(now);
        rotatedToken.setExpiresAt(refreshTokenExpiresAt);
        rotatedToken.setIpAddress(trimToNull(ipAddress));
        rotatedToken.setUserAgent(trimToNull(userAgent));
        refreshTokenRepository.save(rotatedToken);

        // TODO: Publish refresh-token rotation audit/platform event once auth audit flow is finalized.

        return new RefreshTokenResponse(
                accessToken,
                newRawRefreshToken,
                activeRoleAssignment.getId(),
                existingToken.getPlatformAccess(),
                refreshTokenExpiresAt
        );
    }

    @Transactional
    public RefreshTokenResponse refresh(RefreshTokenRequest request, String ipAddress, String userAgent) {
        if (request == null) {
            throw new AuthenticationFailedException("INVALID_REFRESH_TOKEN", "Refresh token is required");
        }
        return refresh(request.refreshToken(), ipAddress, userAgent);
    }

    private void validateRefreshToken(RefreshToken refreshToken, Instant now) {
        if (refreshToken.getRevokedAt() != null) {
            throw new AuthenticationFailedException("REFRESH_TOKEN_REVOKED", "Refresh token has been revoked");
        }
        if (refreshToken.getExpiresAt() == null || !refreshToken.getExpiresAt().isAfter(now)) {
            throw new AuthenticationFailedException("REFRESH_TOKEN_EXPIRED", "Refresh token has expired");
        }
        if (refreshToken.getPlatformAccess() == null) {
            throw new AuthenticationFailedException("INVALID_REFRESH_CONTEXT", "Refresh token is missing session context");
        }
    }

    private void validateRoleAssignment(RoleAssignment roleAssignment) {
        if (roleAssignment == null) {
            throw new AuthenticationFailedException("ROLE_ASSIGNMENT_MISSING", "Session role assignment no longer exists");
        }
        if (!Boolean.TRUE.equals(roleAssignment.getIsActive())) {
            throw new AuthenticationFailedException("ROLE_ASSIGNMENT_INACTIVE", "Session role assignment is no longer active");
        }
    }

    private void validateUser(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException("USER_INACTIVE", "User account is no longer active");
        }
    }

    private void validateCompany(Company company) {
        if (company.getStatus() != CompanyStatus.ACTIVE || company.getDeletedAt() != null) {
            throw new AuthenticationFailedException("COMPANY_INACTIVE", "Company account is no longer active");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
