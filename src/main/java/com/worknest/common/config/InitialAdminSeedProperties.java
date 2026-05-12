package com.worknest.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.seed.admin")
public class InitialAdminSeedProperties {

    private boolean enabled = false;
    private String email;
    private String password;
    private String firstName = "Platform";
    private String lastName = "Administrator";
    private String companyName = "WorkNest Platform";
    private String companySlug = "worknest-platform";
    private String companyEmail = "platform@worknest.local";
    private String companyPhone = "+355000000000";
}
