package com.worknest.features.invitation.web;

import com.worknest.features.invitation.dto.ActivateInvitationRequest;
import com.worknest.features.invitation.dto.ActivateInvitationResponse;
import com.worknest.features.invitation.dto.CreateInvitationRequest;
import com.worknest.features.invitation.dto.CreateInvitationResponse;
import com.worknest.features.invitation.application.InvitationActivationService;
import com.worknest.features.invitation.application.InvitationService;
import com.worknest.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/invitations")
@RequiredArgsConstructor
@Tag(name = "User Onboarding", description = "Endpoints for user invitations and account activation flows.")
public class UserInvitationController {

    private final InvitationService invitationService;
    private final InvitationActivationService invitationActivationService;

    @PostMapping
    @Operation(
            summary = "Create User Invitation",
            description = "Generates a secure invitation token and dispatches it via e-mail. Only accessible by ADMIN or SUPERADMIN roles. " +
                    "This is the first step in adding new members to an organization."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Invitation created and e-mail dispatched")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Insufficient permissions (ADMIN/SUPERADMIN required)",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal error during invitation generation or e-mail dispatch",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<ApiResponse<CreateInvitationResponse>> createInvitation(
            @RequestBody @Valid CreateInvitationRequest request) {
        CreateInvitationResponse response = invitationService.createInvitation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response.message(), response));
    }

    @PostMapping("/activate")
    @Operation(
            summary = "Activate User Account",
            description = "Completes the onboarding flow by validating an invitation token, setting the user's password, " +
                    "and transitioning the user (and potentially the company) to ACTIVE status."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account and organization activated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid/expired token, weak password, or missing consent",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal error during account security finalization",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    public ResponseEntity<ApiResponse<ActivateInvitationResponse>> activateInvitation(
            @RequestBody @Valid ActivateInvitationRequest request,
            HttpServletRequest httpServletRequest
    ) {
        String clientIp = resolveClientIp(httpServletRequest);
        ActivateInvitationResponse response = invitationActivationService.activateInvitation(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success(response.message(), response));
    }

    /**
     * Resolves the real client IP, respecting common reverse-proxy headers.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // The first address in the chain is the originating client
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
