package com.worknest.features.notification.email.service;

import com.worknest.common.config.MailProperties;
import com.worknest.common.i18n.Language;
import com.worknest.domain.entities.Company;
import com.worknest.domain.entities.User;
import com.worknest.features.notification.email.dto.EmailMessage;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetEmailServiceImpl implements PasswordResetEmailService {

    private final EmailI18nService emailI18nService;
    private final MailProperties mailProperties;

    @Override
    public void sendPasswordResetEmail(Company company, User user, String resetLink) {
        Locale locale = user.getPreferredLanguage() != null
                ? user.getPreferredLanguage().getLocale()
                : company.getLocale() != null ? company.getLocale().getLocale() : Language.EN.getLocale();

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("recipientName", resolveRecipientName(user));
        templateModel.put("companyName", company.getName());
        templateModel.put("resetLink", resetLink);
        templateModel.put("fromName", mailProperties.getFromName());

        EmailMessage message = EmailMessage.builder()
                .to(user.getEmail())
                .subjectKey("email.password-reset.subject")
                .subjectArgs(new Object[]{company.getName()})
                .templateName("password-reset")
                .templateModel(templateModel)
                .build();

        emailI18nService.sendMail(message, locale);
        log.info("Password reset email queued for {} in company {}", user.getEmail(), company.getSlug());
    }

    private String resolveRecipientName(User user) {
        if (StringUtils.hasText(user.getDisplayName())) {
            return user.getDisplayName().trim();
        }

        String fullName = (StringUtils.hasText(user.getFirstName()) ? user.getFirstName().trim() : "")
                + " "
                + (StringUtils.hasText(user.getLastName()) ? user.getLastName().trim() : "");

        fullName = fullName.trim();
        return StringUtils.hasText(fullName) ? fullName : user.getEmail();
    }
}
