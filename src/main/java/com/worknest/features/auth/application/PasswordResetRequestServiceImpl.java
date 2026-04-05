package com.worknest.features.auth.application;

import com.worknest.domain.entities.Company;
import com.worknest.domain.enums.CompanyStatus;
import com.worknest.domain.entities.PasswordResetToken;
import com.worknest.domain.entities.User;
import com.worknest.domain.enums.UserStatus;
import com.worknest.features.auth.dto.ForgotPasswordRequest;
import com.worknest.features.company.repository.CompanyRepository;
import com.worknest.features.auth.repository.PasswordResetTokenRepository;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.auth.application.PasswordResetRequestService;
import com.worknest.features.auth.utility.SecureTokenGenerator;
import com.worknest.features.auth.utility.Sha256TokenHashUtility;
import com.worknest.features.notification.email.service.PasswordResetEmailService;
import com.worknest.features.auth.repository.RoleAssignmentRepository;
import com.worknest.audit.service.AuthAuditService;
import com.worknest.domain.entities.RoleAssignment;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetRequestServiceImpl implements PasswordResetRequestService {

    private static final long PASSWORD_RESET_EXPIRY_SECONDS = 24 * 60 * 60L;

    private final UserRepository userRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SecureTokenGenerator secureTokenGenerator;
    private final Sha256TokenHashUtility sha256TokenHashUtility;
    private final PasswordResetEmailService passwordResetEmailService;
    private final AuthAuditService authAuditService;

    @Value("${app.frontend.reset-password-link-base:https://app.worknest.local/reset-password}")
    private String resetPasswordLinkBase;

    @Override
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request, String ipAddress) {
        if (request == null || !StringUtils.hasText(request.email())) {
            return;
        }

        String normalizedEmail = request.email().trim().toLowerCase();

        log.info("Processing global forgot-password request for email {}", normalizedEmail);

        // Find the most appropriate active user identity for this email
        userRepository.findAllByEmailIgnoreCase(normalizedEmail).stream()
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .findFirst()
                .ifPresent(user -> {
                    Company company = resolveCompanyForPasswordReset(user);
                    if (isCompanyActive(company)) {
                        createResetToken(company, user);
                        authAuditService.appendPasswordResetRequested(
                                company.getId(),
                                company.getName(),
                                user.getId(),
                                user.getEmail(),
                                ipAddress
                        );
                    }
                });
    }

    private boolean isCompanyActive(Company company) {
        if (company == null) return false;
        return company.getStatus() == CompanyStatus.ACTIVE && company.getDeletedAt() == null;
    }

    private void createResetToken(Company company, User user) {
        Instant now = Instant.now();
        invalidateOutstandingTokens(user, now);

        String rawToken = secureTokenGenerator.generateToken();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(sha256TokenHashUtility.hash(rawToken));
        resetToken.setExpiresAt(now.plusSeconds(PASSWORD_RESET_EXPIRY_SECONDS));
        passwordResetTokenRepository.save(resetToken);

        String resetLink = resetPasswordLinkBase + "?token=" + rawToken;
        passwordResetEmailService.sendPasswordResetEmail(company, user, resetLink);
    }

    private void invalidateOutstandingTokens(User user, Instant now) {
        passwordResetTokenRepository.findAllByUserIdAndUsedAtIsNull(user.getId())
                .forEach(token -> token.setUsedAt(now));
    }

    private Company resolveCompanyForPasswordReset(User user) {
        return roleAssignmentRepository.findAllByUserIdAndIsActiveTrue(user.getId())
                .stream()
                .map(RoleAssignment::getCompany)
                .filter(this::isCompanyActive)
                .findFirst()
                .orElse(null);
    }
}
