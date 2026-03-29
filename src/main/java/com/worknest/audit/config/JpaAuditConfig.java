package com.worknest.audit.config;

import com.worknest.security.service.CurrentUserAuditorAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditConfig {

    @Bean
    public AuditorAware<String> auditorAware(ObjectProvider<CurrentUserAuditorAware> currentUserAuditorAwareProvider) {
        return currentUserAuditorAwareProvider.getIfAvailable(CurrentUserAuditorAware::new);
    }
}
