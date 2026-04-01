package com.worknest.auth.controller;

import com.worknest.auth.dto.RefreshTokenRequest;
import com.worknest.auth.dto.RefreshTokenResponse;
import com.worknest.auth.service.RefreshTokenService;
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
@Tag(name = "Authentication")
public class RefreshTokenController {

    private final RefreshTokenService refreshTokenService;

    @PostMapping("/refresh")
    @Operation(
            summary = "Rotate Authentication Tokens",
            description = "Allows a client to rotate and renew a short-lived access token using a persistent refresh token. " +
                    "This extends the user session without requiring a password re-entry. " +
                    "A successful refresh returns a new access token and a new refresh token (token rotation)."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tokens rotated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Refresh token is invalid, expired, or has been revoked (e.g., from another session)",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Malformed refresh request",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(
            @RequestBody @Valid RefreshTokenRequest request,
            HttpServletRequest servletRequest
    ) {
        String ipAddress = servletRequest.getRemoteAddr();
        String userAgent = servletRequest.getHeader("User-Agent");
        RefreshTokenResponse response = refreshTokenService.refresh(request.refreshToken(), ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }
}
