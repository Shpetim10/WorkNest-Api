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
public class AuthController {

    private final AuthLoginService authLoginService;
    private final RoleSelectionService roleSelectionService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        String ipAddress = servletRequest.getRemoteAddr();
        LoginResponse response = authLoginService.login(request, ipAddress);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/select-role")
    public ResponseEntity<ApiResponse<SelectRoleResponse>> selectRole(@RequestBody @Valid SelectRoleRequest request) {
        String userId = "authenticated-user-id";
        SelectRoleResponse response = roleSelectionService.selectRole(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Role selected successfully", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(
            @RequestBody @Valid RefreshTokenRequest request,
            HttpServletRequest servletRequest
    ) {
        String ipAddress = servletRequest.getRemoteAddr();
        RefreshTokenResponse response = refreshTokenService.refresh(request, ipAddress);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }
}
