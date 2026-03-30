package com.worknest.auth.service;

import com.worknest.auth.dto.LoginRequest;
import com.worknest.auth.dto.LoginResponse;

public interface AuthLoginService {
    LoginResponse login(LoginRequest request, String ipAddress);
    void logout(String userId);
}
