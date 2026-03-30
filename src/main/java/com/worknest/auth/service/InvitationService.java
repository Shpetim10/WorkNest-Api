package com.worknest.auth.service;

import com.worknest.auth.domain.UserInvitation;
import com.worknest.auth.dto.ActivateInvitationRequest;
import com.worknest.auth.dto.ActivateInvitationResponse;
import com.worknest.auth.dto.CreateInvitationRequest;
import com.worknest.auth.dto.CreateInvitationResponse;

public interface InvitationService {

    /**
     * Creates a new user invitation for a company.
     *
     * @param request the invitation details
     * @return the created invitation response
     */
    CreateInvitationResponse createInvitation(CreateInvitationRequest request);

    /**
     * Activates an invitation, creating a new user and marking the invitation as accepted.
     *
     * @param request the activation details containing the token
     * @return the activation response
     */
    ActivateInvitationResponse activateInvitation(ActivateInvitationRequest request);

    /**
     * Retrieves an invitation by its unique token.
     *
     * @param token the invitation token
     * @return the user invitation entity
     */
    UserInvitation getInvitationByToken(String token);
}
