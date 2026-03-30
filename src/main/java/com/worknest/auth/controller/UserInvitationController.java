package com.worknest.auth.controller;

import com.worknest.auth.dto.ActivateInvitationRequest;
import com.worknest.auth.dto.ActivateInvitationResponse;
import com.worknest.auth.dto.CreateInvitationRequest;
import com.worknest.auth.dto.CreateInvitationResponse;
import com.worknest.auth.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/invitations")
@RequiredArgsConstructor
public class UserInvitationController {

    private final InvitationService invitationService;

    /**
     * Creates a new user invitation for a specific company and role.
     *
     * @param request invitation details
     * @return the created invitation link and session data
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateInvitationResponse createInvitation(@RequestBody @Valid CreateInvitationRequest request) {
        return invitationService.createInvitation(request);
    }

    /**
     * Activates an existing invitation by setting a password and establishing identity.
     *
     * @param request activation details including the unique token
     * @return the newly activated user details
     */
    @PostMapping("/activate")
    public ActivateInvitationResponse activateInvitation(@RequestBody @Valid ActivateInvitationRequest request) {
        return invitationService.activateInvitation(request);
    }
}
