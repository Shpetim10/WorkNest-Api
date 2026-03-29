package com.worknest.security.config;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    @NotBlank
    private String issuer;

    @NotBlank
    private String secretBase64;

    private Duration accessTokenExpiry = Duration.ofMinutes(30);

    private Duration refreshTokenExpiry = Duration.ofDays(7);
}
