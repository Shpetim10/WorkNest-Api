package com.worknest.features.notification.email.service;

import com.worknest.common.config.MailProperties;
import com.worknest.features.notification.email.dto.EmailMessage;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailI18nServiceImpl implements EmailI18nService {

    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;
    private final MessageSource messageSource;
    private final MailProperties mailProperties;

    @Async
    @Override
    public void sendMail(EmailMessage message, Locale locale) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            String subject = messageSource.getMessage(
                    message.getSubjectKey(),
                    message.getSubjectArgs(),
                    locale
            );

            Context context = new Context(locale);
            context.setVariables(message.getTemplateModel());
            String htmlContent = templateEngine.process(message.getTemplateName(), context);

            helper.setFrom(mailProperties.getFromAddress(), mailProperties.getFromName());
            helper.setReplyTo(mailProperties.getReplyTo());
            helper.setTo(message.getTo());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);
            log.info("Email sent to {} with subject: {}", message.getTo(), subject);

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send email to {}", message.getTo(), e);
            throw new RuntimeException("Email delivery failed", e);
        }
    }
}
