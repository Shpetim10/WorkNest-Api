package com.worknest.features.auth.application;

import com.worknest.domain.entities.Company;
import com.worknest.domain.enums.CompanyStatus;
import com.worknest.domain.entities.RefreshToken;
import com.worknest.domain.entities.RoleAssignment;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.UserStatus;
import com.worknest.features.auth.dto.RefreshTokenRequest;
import com.worknest.features.auth.dto.RefreshTokenResponse;
import com.worknest.features.auth.dto.TenantContextDto;
import com.worknest.features.auth.exception.AuthenticationFailedException;
import com.worknest.features.auth.repository.RefreshTokenRepository;
import com.worknest.features.auth.utility.SecureTokenGenerator;
import com.worknest.features.auth.utility.Sha256TokenHashUtility;
import com.worknest.security.config.JwtProperties;
import com.worknest.security.service.JwtService;
import com.worknest.audit.service.AuthAuditService;
import com.worknest.audit.service.model.AuthAuditActorContext;
import com.worknest.audit.service.model.AuthSessionContext;
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
    private final AuthAuditService authAuditService;

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
        validateCompany(activeRoleAssignment.getCompany());

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

        // 5. Emit platform events & audit
        AuthAuditActorContext actorContext = new AuthAuditActorContext(
                activeRoleAssignment.getCompany().getId(),
                activeRoleAssignment.getCompany().getName(),
                user.getId(),
                activeRoleAssignment.getId(),
                activeRoleAssignment.getRole(),
                activeRoleAssignment.getJobTitle(),
                ipAddress
        );

        AuthSessionContext sessionContext = new AuthSessionContext(
                user.getId(),
                activeRoleAssignment.getId(),
                activeRoleAssignment.getRole(),
                rotatedToken.getPlatformAccess()
        );

        authAuditService.appendTokenRefreshed(actorContext, sessionContext, existingToken.getId());

        return new RefreshTokenResponse(
                accessToken,
                newRawRefreshToken,
                activeRoleAssignment.getId(),
                existingToken.getPlatformAccess(),
                toTenantContext(activeRoleAssignment.getCompany()),
                accessTokenExpiresAt,
                refreshTokenExpiresAt
        );
    }

    @Override
    @Transactional
    public void revoke(String rawRefreshToken) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            log.warn("Logout attempt with empty refresh token.");
            return;
        }

        String tokenHash = sha256TokenHashUtility.hash(rawRefreshToken);
        log.debug("Hashed refresh token for lookup: {}", tokenHash);
        
        RefreshToken token = refreshTokenRepository.findByTokenHashWithAuditing(tokenHash)
                .orElse(null);

        if (token == null) {
            log.warn("Logout attempt: Refresh token not found in database.");
            return;
        }

        if (token.getRevokedAt() != null) {
            log.info("Logout attempt: Refresh token already revoked at {} with reason: {}", 
                    token.getRevokedAt(), token.getRevokedReason());
            return;
        }

        Instant now = Instant.now();
        refreshTokenRepository.revokeById(token.getId(), now, "logout");
        log.info("Refresh token {} successfully marked as revoked for logout.", token.getId());

        try {
            RoleAssignment assignment = token.getActiveRoleAssignment();
            if (assignment != null) {
                log.debug("Found active role assignment {} for token. Building audit context...", assignment.getId());
                
                AuthAuditActorContext actorContext = new AuthAuditActorContext(
                        assignment.getCompany().getId(),
                        assignment.getCompany().getName(),
                        token.getUser().getId(),
                        assignment.getId(),
                        assignment.getRole(),
                        assignment.getJobTitle(),
                        token.getIpAddress()
                );

                AuthSessionContext sessionContext = new AuthSessionContext(
                        token.getUser().getId(),
                        assignment.getId(),
                        assignment.getRole(),
                        token.getPlatformAccess()
                );

                log.debug("Appending logout audit for user {}", token.getUser().getEmail());
                authAuditService.appendLogout(actorContext, sessionContext);
            } else {
                log.warn("Logout performed for token {} without an active role assignment, skipping detailed audit.", token.getId());
            }
        } catch (Exception e) {
            log.error("Failed to append logout audit trail for token {}, but token was successfully revoked.", token.getId(), e);
        }

        log.info("Logout process completed for user {} (Token: {})", 
                token.getUser() != null ? token.getUser().getEmail() : "unknown", 
                token.getId());
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
