package com.worknest.features.notification.email.service;

import com.worknest.common.config.MailProperties;
import com.worknest.common.i18n.Language;
import com.worknest.domain.entities.Company;
import com.worknest.features.notification.email.dto.EmailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyStatusEmailServiceImpl implements CompanyStatusEmailService {

    private final EmailI18nService emailI18nService;
    private final MailProperties mailProperties;

    @Override
    public void sendCompanySuspendedEmail(Company company, String reason) {
        Locale locale = resolveLocale(company);

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("companyName", company.getName());
        templateModel.put("reason", reason);
        templateModel.put("fromName", mailProperties.getFromName());

        EmailMessage message = EmailMessage.builder()
                .to(company.getEmail())
                .subjectKey("email.company-suspended.subject")
                .subjectArgs(new Object[]{company.getName()})
                .templateName("company-suspended")
                .templateModel(templateModel)
                .build();

        emailI18nService.sendMail(message, locale);
        log.info("Suspension email queued for company {}", company.getSlug());
    }

    @Override
    public void sendCompanyUnsuspendedEmail(Company company) {
        Locale locale = resolveLocale(company);

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("companyName", company.getName());
        templateModel.put("fromName", mailProperties.getFromName());

        EmailMessage message = EmailMessage.builder()
                .to(company.getEmail())
                .subjectKey("email.company-unsuspended.subject")
                .subjectArgs(new Object[]{company.getName()})
                .templateName("company-unsuspended")
                .templateModel(templateModel)
                .build();

        emailI18nService.sendMail(message, locale);
        log.info("Reactivation email queued for company {}", company.getSlug());
    }

    private Locale resolveLocale(Company company) {
        return company.getLocale() != null ? company.getLocale().getLocale() : Language.EN.getLocale();
    }
}
