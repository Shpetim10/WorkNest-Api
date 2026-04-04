package com.worknest.features.invitation.application;

import com.worknest.features.invitation.dto.ActivateInvitationRequest;
import com.worknest.features.invitation.dto.ActivateInvitationResponse;

public interface InvitationActivationService {

    /**
     * Activates a pending invitation identified by the raw token inside {@code request}.
     *
     * @param request  the activation payload (token, password, GDPR consent, optional profile)
     * @param clientIp the IP address of the caller — recorded for GDPR audit purposes
     */
    ActivateInvitationResponse activateInvitation(ActivateInvitationRequest request, String clientIp);
}
