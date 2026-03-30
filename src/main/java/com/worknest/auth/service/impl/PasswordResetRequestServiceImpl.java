package com.worknest.auth.service.impl;

import com.worknest.auth.dto.ForgotPasswordRequest;
import com.worknest.auth.service.PasswordResetRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetRequestServiceImpl implements PasswordResetRequestService {

    @Override
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        log.info("Requesting password reset for email: {} in company: {}", request.email(), request.companySlug());
        
        // TODO: Find existing user by company and email
        // TODO: Generate password reset token
        // TODO: Create PasswordResetToken entity
        // TODO: Publish PasswordResetRequestedEvent (to trigger email notification)
        // TODO: Ensure idempotency (single request per period)
    }
}
