package com.worknest.security.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    public String extractTokenFromRequest(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    public boolean validateToken(String token) {
        // TODO Implement JWT signature, expiry, issuer, and claim validation.
        return false;
    }

    public Authentication toAuthentication(String token) {
        // TODO Load claims and map them to principal/authorities.
        return new UsernamePasswordAuthenticationToken("anonymous", token, Collections.emptyList());
    }
}
