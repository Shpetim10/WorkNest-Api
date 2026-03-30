package com.worknest.auth.controller;

import com.worknest.auth.dto.LoginRequest;
import com.worknest.auth.dto.LoginResponse;
import com.worknest.auth.dto.RefreshTokenRequest;
import com.worknest.auth.dto.RefreshTokenResponse;
import com.worknest.auth.dto.SelectRoleRequest;
import com.worknest.auth.dto.SelectRoleResponse;
import com.worknest.auth.service.AuthLoginService;
import com.worknest.auth.service.RefreshTokenService;
import com.worknest.auth.service.RoleSelectionService;
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

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user authentication and session management")
public class AuthController {

    private final AuthLoginService authLoginService;
    private final RoleSelectionService roleSelectionService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticates a user with email and password and returns a JWT token.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Invalid credentials",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        String ipAddress = servletRequest.getRemoteAddr();
        LoginResponse response = authLoginService.login(request, ipAddress);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/select-role")
    @Operation(summary = "Select User Role", description = "Allows a user to select a specific role if they have multiple roles associated with their account.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Role selected successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid role selection",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    public ResponseEntity<ApiResponse<SelectRoleResponse>> selectRole(@RequestBody @Valid SelectRoleRequest request) {
        String userId = "authenticated-user-id";
        SelectRoleResponse response = roleSelectionService.selectRole(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Role selected successfully", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Authentication Token", description = "Uses a refresh token to generate a new short-lived access token.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Invalid or expired refresh token",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(
            @RequestBody @Valid RefreshTokenRequest request,
            HttpServletRequest servletRequest
    ) {
        String ipAddress = servletRequest.getRemoteAddr();
        RefreshTokenResponse response = refreshTokenService.refresh(request, ipAddress);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }
}
