package com.worknest.features.notification.email.service;

import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.User;

public interface PasswordResetEmailService {

    void sendPasswordResetEmail(
            Company company,
            User user,
            String resetLink
    );
}
