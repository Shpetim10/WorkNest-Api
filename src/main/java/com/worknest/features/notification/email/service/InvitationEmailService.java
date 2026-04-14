package com.worknest.features.notification.email.service;

import com.worknest.domain.entities.Company;
import com.worknest.domain.enums.InvitationKind;
import com.worknest.domain.enums.PlatformRole;

public interface InvitationEmailService {

    void sendInvitationEmail(
            Company company,
            String recipientEmail,
            String recipientDisplayName,
            PlatformRole platformRole,
            InvitationKind invitationKind,
            String activationLink,
            String preferredLanguage);

    void sendProvisioningEmail(
            Company company,
            String recipientEmail,
            String recipientDisplayName,
            PlatformRole platformRole,
            String activationLink,
            String preferredLanguage);

    void sendNewPositionEmail(
            Company company,
            String recipientEmail,
            String recipientDisplayName,
            PlatformRole platformRole,
            String preferredLanguage);
}
