package com.worknest.auth.service;

import com.worknest.auth.domain.UserInvitation;
import com.worknest.auth.dto.ActivateInvitationRequest;
import com.worknest.auth.dto.ActivateInvitationResponse;
import com.worknest.auth.dto.CreateInvitationRequest;
import com.worknest.auth.dto.CreateInvitationResponse;

public interface InvitationService {
    CreateInvitationResponse createInvitation(CreateInvitationRequest request);
    ActivateInvitationResponse activateInvitation(ActivateInvitationRequest request);
    UserInvitation getInvitationByToken(String token);
}
