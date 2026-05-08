package com.worknest.realtime.config;

import com.worknest.realtime.security.StompAuthChannelInterceptor;
import com.worknest.realtime.security.SubscriptionAuthorizationInterceptor;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@Slf4j
@RequiredArgsConstructor
public class RealtimeWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final List<String> DEFAULT_NATIVE_ORIGIN_PATTERNS = List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "http://10.0.2.2:*",
            "http://10.0.3.2:*",
            "exp://*",
            "exps://*",
            "null"
    );

    private final RealtimeProperties realtimeProperties;
    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final SubscriptionAuthorizationInterceptor subscriptionAuthorizationInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] originPatterns = resolveAllowedOriginPatterns();
        registry.addEndpoint(realtimeProperties.getWebsocketEndpoint())
                .setAllowedOriginPatterns(originPatterns)
                .withSockJS();
        // Raw WebSocket endpoint for native clients (React Native, etc.) that don't use SockJS
        registry.addEndpoint(realtimeProperties.getWebsocketEndpoint() + "/websocket")
                .setAllowedOriginPatterns(originPatterns);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor, subscriptionAuthorizationInterceptor);
    }

    private String[] resolveAllowedOriginPatterns() {
        LinkedHashSet<String> originPatterns = new LinkedHashSet<>(DEFAULT_NATIVE_ORIGIN_PATTERNS);
        originPatterns.addAll(realtimeProperties.getAllowedOriginPatterns());
        log.info("Realtime WebSocket allowed origin patterns: {}", originPatterns);
        return originPatterns.toArray(new String[0]);
    }
}
