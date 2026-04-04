package com.worknest.auth.service.impl;

import com.worknest.auth.domain.Company;
import com.worknest.auth.domain.InvitationKind;
import com.worknest.auth.domain.PlatformRole;
import com.worknest.auth.service.InvitationEmailService;
import com.worknest.common.config.MailProperties;
import com.worknest.common.i18n.Language;
import com.worknest.features.notification.email.dto.EmailMessage;
import com.worknest.features.notification.email.service.EmailI18nService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationEmailServiceImpl implements InvitationEmailService {

    private final EmailI18nService emailI18nService;
    private final MailProperties mailProperties;

    @Override
    public void sendInvitationEmail(
            Company company,
            String recipientEmail,
            String recipientDisplayName,
            PlatformRole platformRole,
            InvitationKind invitationKind,
            String activationLink,
            String preferredLanguage) {

        Locale locale = Language.getLocaleOrDefault(preferredLanguage);
        
        String subjectKey = invitationKind == InvitationKind.INITIAL_ADMIN_ACTIVATION
                ? "email.invitation.subject.initial_admin"
                : "email.invitation.subject.standard";

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("recipientName", recipientDisplayName);
        templateModel.put("companyName", company.getName());
        templateModel.put("role", platformRole.name());
        templateModel.put("activationLink", activationLink);
        templateModel.put("fromName", mailProperties.getFromName());
        templateModel.put("invitationKind", invitationKind.name());

        EmailMessage message = EmailMessage.builder()
                .to(recipientEmail)
                .subjectKey(subjectKey)
                .subjectArgs(new Object[]{company.getName()})
                .templateName("invitation")
                .templateModel(templateModel)
                .build();

        emailI18nService.sendMail(message, locale);
    }
}
