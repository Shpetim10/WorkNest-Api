package com.worknest.features.invitation.application;

import com.worknest.features.invitation.dto.CreateInvitationRequest;
import com.worknest.features.invitation.dto.CreateInvitationResponse;

public interface InvitationService {
    CreateInvitationResponse createInvitation(CreateInvitationRequest request);
}
