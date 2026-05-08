package com.worknest.features.notification.email.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.worknest.common.config.MailProperties;
import com.worknest.features.notification.email.dto.EmailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailI18nServiceImpl implements EmailI18nService {

    private final Resend resend;
    private final SpringTemplateEngine templateEngine;
    private final MessageSource messageSource;
    private final MailProperties mailProperties;

    @Async
    @Override
    public void sendMail(EmailMessage message, Locale locale) {
        try {
            String subject = messageSource.getMessage(
                    message.getSubjectKey(),
                    message.getSubjectArgs(),
                    locale
            );

            Context context = new Context(locale);
            context.setVariables(message.getTemplateModel());
            String htmlContent = templateEngine.process(message.getTemplateName(), context);

            CreateEmailOptions email = CreateEmailOptions.builder()
                    .from(formatFromHeader())
                    .replyTo(mailProperties.getReplyTo())
                    .to(message.getTo())
                    .subject(subject)
                    .html(htmlContent)
                    .build();

            resend.emails().send(email);
            log.info("Email sent to {} with subject: {}", message.getTo(), subject);

        } catch (ResendException | RuntimeException e) {
            log.error("Failed to send email to {}", message.getTo(), e);
            throw new RuntimeException("Email delivery failed", e);
        }
    }

    private String formatFromHeader() {
        return "%s <%s>".formatted(mailProperties.getFromName(), mailProperties.getFromAddress());
    }
}
