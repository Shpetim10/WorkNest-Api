package com.worknest.realtime.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.realtime")
public class RealtimeProperties {

    private String websocketEndpoint = "/ws";
    private List<String> allowedOriginPatterns = new ArrayList<>();
    private Broker broker = new Broker();

    @Getter
    @Setter
    public static class Broker {
        private boolean simpleEnabled = true;
    }
}
