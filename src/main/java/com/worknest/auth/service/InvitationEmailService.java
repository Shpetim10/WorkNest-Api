package com.worknest.auth.service;

import com.worknest.auth.domain.Company;
import com.worknest.auth.domain.InvitationKind;
import com.worknest.auth.domain.PlatformRole;

public interface InvitationEmailService {

    void sendInvitationEmail(
            Company company,
            String recipientEmail,
            String recipientDisplayName,
            PlatformRole platformRole,
            InvitationKind invitationKind,
            String activationLink);
}
