package com.worknest.auth.controller;

import com.worknest.auth.dto.ActivateInvitationRequest;
import com.worknest.auth.dto.ActivateInvitationResponse;
import com.worknest.auth.dto.CreateInvitationRequest;
import com.worknest.auth.dto.CreateInvitationResponse;
import com.worknest.auth.service.InvitationActivationService;
import com.worknest.auth.service.InvitationService;
import com.worknest.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/invitations")
@RequiredArgsConstructor
public class UserInvitationController {

    private final InvitationService invitationService;
    private final InvitationActivationService invitationActivationService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateInvitationResponse>> createInvitation(@RequestBody @Valid CreateInvitationRequest request) {
        CreateInvitationResponse response = invitationService.createInvitation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response.message(), response));
    }

    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<ActivateInvitationResponse>> activateInvitation(
            @RequestBody @Valid ActivateInvitationRequest request
    ) {
        ActivateInvitationResponse response = invitationActivationService.activateInvitation(request);
        return ResponseEntity.ok(ApiResponse.success(response.message(), response));
    }
}
