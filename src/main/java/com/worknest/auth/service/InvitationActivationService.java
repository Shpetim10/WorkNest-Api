package com.worknest.auth.service;

import com.worknest.auth.dto.ActivateInvitationRequest;
import com.worknest.auth.dto.ActivateInvitationResponse;

public interface InvitationActivationService {
    ActivateInvitationResponse activateInvitation(ActivateInvitationRequest request);
}
