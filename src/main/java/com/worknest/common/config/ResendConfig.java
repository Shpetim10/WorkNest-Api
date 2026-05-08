package com.worknest.common.config;

import com.resend.Resend;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ResendConfig {

    private final MailProperties mailProperties;

    @Bean
    public Resend resend() {
        return new Resend(mailProperties.getApiKey());
    }
}
