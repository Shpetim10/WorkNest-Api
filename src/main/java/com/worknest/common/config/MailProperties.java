package com.worknest.common.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    @NotBlank
    @Email
    private String fromAddress;

    @NotBlank
    private String fromName;

    @NotBlank
    @Email
    private String replyTo;
}
