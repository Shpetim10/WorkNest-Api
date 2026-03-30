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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthLoginService authLoginService;
    private final RoleSelectionService roleSelectionService;
    private final RefreshTokenService refreshTokenService;

    /**
     * Authenticates a user within a specific company environment.
     *
     * @param request the login credentials and company context
     * @param servletRequest raw request used to capture the IP address
     * @return identifying data and initial token sets
     */
    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request, HttpServletRequest servletRequest) {
        String ipAddress = servletRequest.getRemoteAddr();
        return authLoginService.login(request, ipAddress);
    }

    /**
     * Allows an authenticated platform user to switch to a specific company/role assignment.
     *
     * @param request the role assignment details
     * @return the context-specific access and refresh tokens
     */
    @PostMapping("/select-role")
    public SelectRoleResponse selectRole(@RequestBody @Valid SelectRoleRequest request) {
        // TODO: Extract authenticated userId from SecurityContext
        String userId = "authenticated-user-id"; 
        return roleSelectionService.selectRole(request, userId);
    }

    /**
     * Rotates existing tokens using a valid refresh token.
     *
     * @param request the current refresh token
     * @param servletRequest raw request for tracking
     * @return a fresh access and refresh token pair
     */
    @PostMapping("/refresh")
    public RefreshTokenResponse refresh(@RequestBody @Valid RefreshTokenRequest request, HttpServletRequest servletRequest) {
        String ipAddress = servletRequest.getRemoteAddr();
        return refreshTokenService.refresh(request, ipAddress);
    }
}
