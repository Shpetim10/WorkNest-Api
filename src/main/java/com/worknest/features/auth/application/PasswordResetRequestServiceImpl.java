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
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetRequestServiceImpl implements PasswordResetRequestService {

    private static final long PASSWORD_RESET_EXPIRY_SECONDS = 24 * 60 * 60L;

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SecureTokenGenerator secureTokenGenerator;
    private final Sha256TokenHashUtility sha256TokenHashUtility;

    @Override
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request, String ipAddress) {
        if (request == null || !StringUtils.hasText(request.companySlug()) || !StringUtils.hasText(request.email())) {
            return;
        }

        String normalizedCompanySlug = request.companySlug().trim().toLowerCase();
        String normalizedEmail = request.email().trim().toLowerCase();

        log.info("Processing forgot-password request for company {} and email {}", normalizedCompanySlug, normalizedEmail);

        companyRepository.findBySlugIgnoreCase(normalizedCompanySlug)
                .filter(this::isCompanyActive)
                .flatMap(company -> findActiveUser(company, normalizedEmail))
                .ifPresent(this::createResetToken);

        // TODO: Publish password-reset-requested audit/platform event and hand off raw token to notification flow.
    }

    private java.util.Optional<User> findActiveUser(Company company, String normalizedEmail) {
        return userRepository.findByCompanyIdAndEmailIgnoreCase(company.getId(), normalizedEmail)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE);
    }

    private boolean isCompanyActive(Company company) {
        return company.getStatus() == CompanyStatus.ACTIVE && company.getDeletedAt() == null;
    }

    private void createResetToken(User user) {
        Instant now = Instant.now();
        String rawToken = secureTokenGenerator.generateToken();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(sha256TokenHashUtility.hash(rawToken));
        resetToken.setExpiresAt(now.plusSeconds(PASSWORD_RESET_EXPIRY_SECONDS));
        passwordResetTokenRepository.save(resetToken);
    }
}
