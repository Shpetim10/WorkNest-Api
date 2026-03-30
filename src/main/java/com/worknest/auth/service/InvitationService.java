package com.worknest.auth.service;

import com.worknest.auth.dto.CreateInvitationRequest;
import com.worknest.auth.dto.CreateInvitationResponse;

public interface InvitationService {
    CreateInvitationResponse createInvitation(CreateInvitationRequest request);
}
