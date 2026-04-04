package com.worknest.features.notification.email.service;

import com.worknest.features.notification.email.dto.EmailMessage;
import java.util.Locale;

public interface EmailI18nService {
    void sendMail(EmailMessage message, Locale locale);
}
