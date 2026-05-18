package com.worknest.features.employee.application;

import com.worknest.common.exception.BusinessException;
import com.worknest.domain.entities.User;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.employee.dto.MobileProfileResponse;
import com.worknest.security.AuthSessionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MobileProfileServiceImpl implements MobileProfileService {

    private final UserRepository userRepository;

    @Override
    public MobileProfileResponse getMyProfile() {
        AuthSessionPrincipal principal = principal();
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found."));

        return new MobileProfileResponse(
                user.getFirstName(),
                user.getLastName(),
                user.getProfileImagePath()
        );
    }

    private AuthSessionPrincipal principal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthSessionPrincipal p)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "No authentication session found.");
        }
        return p;
    }
}
