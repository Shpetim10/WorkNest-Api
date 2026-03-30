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

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request, HttpServletRequest servletRequest) {
        String ipAddress = servletRequest.getRemoteAddr();
        return authLoginService.login(request, ipAddress);
    }

    @PostMapping("/select-role")
    public SelectRoleResponse selectRole(@RequestBody @Valid SelectRoleRequest request) {
        // TODO: Extract authenticated userId from SecurityContext
        String userId = "authenticated-user-id"; 
        return roleSelectionService.selectRole(request, userId);
    }

    @PostMapping("/refresh")
    public RefreshTokenResponse refresh(@RequestBody @Valid RefreshTokenRequest request, HttpServletRequest servletRequest) {
        String ipAddress = servletRequest.getRemoteAddr();
        return refreshTokenService.refresh(request, ipAddress);
    }
}
