package com.worknest.features.notification.email.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvitationEmailContext {
    private String recipientName;
    private String companyName;
    private String role;
    private String activationLink;
    private String fromName;
    private String invitationKind;
}
