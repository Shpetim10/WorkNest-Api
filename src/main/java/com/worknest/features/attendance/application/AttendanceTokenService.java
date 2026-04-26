package com.worknest.features.attendance.application;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class AttendanceTokenService {

    private static final int TOKEN_BYTES = 32;
    private final SecureRandom random = new SecureRandom();

    public String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
