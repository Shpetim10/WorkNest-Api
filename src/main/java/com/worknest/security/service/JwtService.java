package com.worknest.security.service;

import com.worknest.domain.enums.PlatformAccess;
import com.worknest.domain.enums.PlatformRole;
import com.worknest.domain.entities.RoleAssignment;
import com.worknest.domain.entities.User;
import com.worknest.security.AuthSessionPrincipal;
import io.jsonwebtoken.Claims;
import com.worknest.security.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.getSecretBase64());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(User user, RoleAssignment assignment, PlatformAccess access, Instant issuedAt, Instant expiresAt) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("companyId", assignment.getCompany().getId().toString());
        claims.put("companySlug", assignment.getCompany().getSlug());
        claims.put("role", assignment.getRole().name());
        claims.put("roleAssignmentId", assignment.getId().toString());
        claims.put("platformAccess", access.name());

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractTokenFromRequest(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Authentication toAuthentication(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        AuthSessionPrincipal principal = new AuthSessionPrincipal(
                UUID.fromString(claims.get("userId", String.class)),
                claims.getSubject(),
                UUID.fromString(claims.get("companyId", String.class)),
                claims.get("companySlug", String.class),
                UUID.fromString(claims.get("roleAssignmentId", String.class)),
                PlatformRole.valueOf(claims.get("role", String.class)),
                PlatformAccess.valueOf(claims.get("platformAccess", String.class))
        );

        return new UsernamePasswordAuthenticationToken(
                principal,
                token,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + principal.role().name()))
        );
    }

    public long getAccessTokenExpirationMs() {
        return jwtProperties.getAccessTokenExpiry().toMillis();
    }
}
