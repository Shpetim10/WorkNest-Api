package com.worknest.features.auth.application;

import com.worknest.domain.entities.PasswordResetToken;
import com.worknest.domain.entities.User;
import com.worknest.features.auth.dto.GenericMessageResponse;
import com.worknest.features.auth.dto.ResetPasswordRequest;
import com.worknest.features.auth.exception.ResetTokenAlreadyUsedException;
import com.worknest.features.auth.exception.ResetTokenExpiredException;
import com.worknest.features.auth.exception.ResetTokenInvalidException;
import com.worknest.features.auth.exception.WeakPasswordException;
import com.worknest.features.auth.repository.PasswordResetTokenRepository;
import com.worknest.features.auth.repository.RefreshTokenRepository;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.auth.application.PasswordResetService;
import com.worknest.features.auth.utility.Sha256TokenHashUtility;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final String PASSWORD_RESET_REVOKE_REASON = "password_reset";

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final Sha256TokenHashUtility sha256TokenHashUtility;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public GenericMessageResponse resetPassword(ResetPasswordRequest request, String ipAddress) {
        validateRequest(request);

        Instant now = Instant.now();
        String tokenHash = sha256TokenHashUtility.hash(request.token());

        log.info("Processing password reset using hashed token lookup");

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResetTokenInvalidException("Reset token is invalid"));

        if (resetToken.getUsedAt() != null) {
            throw new ResetTokenAlreadyUsedException("Reset token has already been used");
        }
        if (resetToken.getExpiresAt() == null || !resetToken.getExpiresAt().isAfter(now)) {
            throw new ResetTokenExpiredException("Reset token has expired");
        }

        User user = resetToken.getUser();
        validatePassword(request.newPassword(), user.getEmail());

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        resetToken.setUsedAt(now);
        passwordResetTokenRepository.save(resetToken);

        refreshTokenRepository.revokeAllActiveByUserId(user.getId(), now, PASSWORD_RESET_REVOKE_REASON);

        // TODO: Publish password-changed audit/platform event and security notification hook.

        return new GenericMessageResponse("Password reset successfully");
    }

    private void validateRequest(ResetPasswordRequest request) {
        if (request == null || !StringUtils.hasText(request.token())) {
            throw new ResetTokenInvalidException("Reset token is required");
        }
        if (!StringUtils.hasText(request.newPassword())) {
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
}
