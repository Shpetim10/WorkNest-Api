package com.worknest.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.seed.dev-data")
public class DevDataSeedProperties {

    private boolean enabled = false;
    private boolean eraseAllBeforeSeed = false;
    private boolean forceReseed = false;
    private boolean runBaseScript = true;
    private boolean generateBulkData = true;
    private int bulkEmployees = 60;
    private int attendanceDays = 180;
    private int leaveRequestsPerEmployee = 8;
    private int payrollMonths = 12;
}
