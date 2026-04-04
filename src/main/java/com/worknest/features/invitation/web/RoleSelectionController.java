package com.worknest.features.invitation.web;

import com.worknest.features.invitation.dto.SelectRoleRequest;
import com.worknest.features.invitation.dto.SelectRoleResponse;
import com.worknest.features.invitation.application.RoleSelectionService;
import com.worknest.common.api.ApiResponse;
import com.worknest.security.AuthSessionPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user session management, including login, role selection, and token rotation.")
public class RoleSelectionController {

    private final RoleSelectionService roleSelectionService;

    @PostMapping("/select-role")
    @Operation(
            summary = "Initialize Workspace Session",
            description = "Allows a user to select a specific identity context (role/company) after a multi-context primary login. " +
                    "This initializes the primary session role and context required for accessing protected platform resources. " +
                    "Requires a valid partial-session access token from the initial login."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Workspace session initialized successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "User is not authenticated or session has expired",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid role selection or context (e.g., user is not associated with the requested company)",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error during role selection",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    public ResponseEntity<ApiResponse<SelectRoleResponse>> selectRole(
            @RequestBody @Valid SelectRoleRequest request,
            Principal principal,
            HttpServletRequest servletRequest
    ) {
        AuthSessionPrincipal sessionPrincipal = (AuthSessionPrincipal) principal;
        String ipAddress = servletRequest.getRemoteAddr();
        String userAgent = servletRequest.getHeader("User-Agent");
        SelectRoleResponse response = roleSelectionService.selectRole(
                request,
                sessionPrincipal.userId().toString(),
                ipAddress,
                userAgent
        );
        return ResponseEntity.ok(ApiResponse.success("Role selected successfully", response));
    }
}
