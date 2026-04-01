package com.worknest.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.tenant.session")
public class TenantSessionProperties {

    private String currentCompanySetting = "app.current_company_id";
}
