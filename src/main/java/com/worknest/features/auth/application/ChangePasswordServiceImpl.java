package com.worknest.features.auth.application;

import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.User;
import com.worknest.features.auth.dto.ChangePasswordRequest;
import com.worknest.features.auth.dto.GenericMessageResponse;
import com.worknest.features.auth.exception.WeakPasswordException;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.security.AuthSessionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangePasswordServiceImpl implements ChangePasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public GenericMessageResponse changePassword(ChangePasswordRequest request, AuthSessionPrincipal principal) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_CURRENT_PASSWORD", "Current password is incorrect");
        }

        validateNewPassword(request.newPassword(), user.getEmail());

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        return new GenericMessageResponse("Password changed successfully");
    }

    private void validateNewPassword(String password, String email) {
        if (password.length() < 8) {
            throw new WeakPasswordException("Password must be at least 8 characters long");
        }
        if (!password.chars().anyMatch(Character::isUpperCase)) {
            throw new WeakPasswordException("Password must contain at least one uppercase letter");
        }
        if (!password.chars().anyMatch(Character::isDigit)) {
            throw new WeakPasswordException("Password must contain at least one number");
        }
        if (password.equalsIgnoreCase(email)) {
            throw new WeakPasswordException("Password must not be equal to your email");
        }
    }
}