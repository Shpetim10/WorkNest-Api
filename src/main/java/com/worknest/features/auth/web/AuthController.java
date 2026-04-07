package com.worknest.features.auth.web;

import com.worknest.features.auth.dto.LoginRequest;
import com.worknest.features.auth.dto.LoginResponse;
import com.worknest.features.auth.application.AuthLoginService;
import com.worknest.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.worknest.features.auth.dto.LogoutRequest;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user session management, including login, role selection, and token rotation.")
public class AuthController {
 
    private final AuthLoginService authLoginService;
 
    @PostMapping("/login")
    @Operation(
            summary = "User Primary Login",
            description = "Authenticates a user with email and password. " +
                    "If successful, returns a short-lived access token and a long-lived refresh token. " +
                    "If the user has multiple identity contexts (roles or companies), 'roleSelectionRequired' will be true, " +
                    "and the user must call /select-role to initialize a specific workspace session."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Authentication successful")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Invalid email or password",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Validation failure (e.g., malformed email)",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error during authentication",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        String ipAddress = servletRequest.getRemoteAddr();
        String userAgent = servletRequest.getHeader("User-Agent");
        LoginResponse response = authLoginService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "User Logout",
            description = "Ends the current session by revoking the provided refresh token. " +
                    "After logout, the refresh token and any associated access tokens are considered invalid."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logout successful")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Malformed logout request",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error during logout",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody @Valid LogoutRequest request
    ) {
        authLoginService.logout(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }
}
