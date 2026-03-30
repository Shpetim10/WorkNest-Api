package com.worknest.auth.controller;

import com.worknest.auth.dto.ActivateInvitationRequest;
import com.worknest.auth.dto.ActivateInvitationResponse;
import com.worknest.auth.dto.CreateInvitationRequest;
import com.worknest.auth.dto.CreateInvitationResponse;
import com.worknest.auth.service.InvitationActivationService;
import com.worknest.auth.service.InvitationService;
import com.worknest.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/invitations")
@RequiredArgsConstructor
@Tag(name = "User Invitations", description = "Endpoints for managing and activating user invitations")
public class UserInvitationController {

    private final InvitationService invitationService;
    private final InvitationActivationService invitationActivationService;

    @PostMapping
    @Operation(summary = "Create a new user invitation", description = "Generates a secure invitation token and sends it to the user. Only accessible by authorized personnel.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Invitation created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Insufficient permissions",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    public ResponseEntity<ApiResponse<CreateInvitationResponse>> createInvitation(@RequestBody @Valid CreateInvitationRequest request) {
        CreateInvitationResponse response = invitationService.createInvitation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response.message(), response));
    }

    @PostMapping("/activate")
    @Operation(summary = "Activate an invitation", description = "Verifies the invitation token and allows the user to complete their registration by setting a password and profile details.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invitation activated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid or expired token",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    public ResponseEntity<ApiResponse<ActivateInvitationResponse>> activateInvitation(
            @RequestBody @Valid ActivateInvitationRequest request
    ) {
        ActivateInvitationResponse response = invitationActivationService.activateInvitation(request);
        return ResponseEntity.ok(ApiResponse.success(response.message(), response));
    }
}
