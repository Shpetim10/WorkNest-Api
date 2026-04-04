package com.worknest.auth.service.impl;

import com.worknest.auth.domain.Company;
import com.worknest.auth.domain.InvitationKind;
import com.worknest.auth.domain.PlatformRole;
import com.worknest.auth.exception.InvitationDeliveryFailedException;
import com.worknest.auth.service.InvitationEmailService;
import com.worknest.common.config.MailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationEmailServiceImpl implements InvitationEmailService {

    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;

    @Override
    public void sendInvitationEmail(
            Company company,
            String recipientEmail,
            String recipientDisplayName,
            PlatformRole platformRole,
            InvitationKind invitationKind,
            String activationLink) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(formatFromHeader());
        message.setReplyTo(mailProperties.getReplyTo());
        message.setTo(recipientEmail);
        message.setSubject(buildSubject(company, invitationKind));
        message.setText(buildBody(company, recipientDisplayName, platformRole, invitationKind, activationLink));

        try {
            javaMailSender.send(message);
        } catch (MailException ex) {
            log.error(
                    "Failed to deliver {} email to {} for company {}",
                    invitationKind,
                    recipientEmail,
                    company.getId(),
                    ex);
            throw new InvitationDeliveryFailedException(recipientEmail);
        }
    }

    private String buildSubject(Company company, InvitationKind invitationKind) {
        if (invitationKind == InvitationKind.INITIAL_ADMIN_ACTIVATION) {
            return "Activate your WorkNest admin account for " + company.getName();
        }
        return "You've been invited to WorkNest for " + company.getName();
    }

    private String buildBody(
            Company company,
            String recipientDisplayName,
            PlatformRole platformRole,
            InvitationKind invitationKind,
            String activationLink) {

        String greetingName = StringUtils.hasText(recipientDisplayName) ? recipientDisplayName.trim() : "there";

        String intro = invitationKind == InvitationKind.INITIAL_ADMIN_ACTIVATION
                ? "Your company workspace has been created and your admin account is ready for activation."
                : "You have been invited to join your company workspace.";

        return """
                Hello %s,

                %s

                Company: %s
                Role: %s

                Use the link below to complete your account setup:
                %s

                If you did not expect this email, you can ignore it.

                %s
                """.formatted(
                greetingName,
                intro,
                company.getName(),
                platformRole.name(),
                activationLink,
                mailProperties.getFromName());
    }

    private String formatFromHeader() {
        return "%s <%s>".formatted(mailProperties.getFromName(), mailProperties.getFromAddress());
    }
}
