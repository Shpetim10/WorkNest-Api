package com.worknest.realtime.config;

import com.worknest.realtime.security.StompAuthChannelInterceptor;
import com.worknest.realtime.security.SubscriptionAuthorizationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class RealtimeWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final RealtimeProperties realtimeProperties;
    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final SubscriptionAuthorizationInterceptor subscriptionAuthorizationInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] originPatterns = realtimeProperties.getAllowedOriginPatterns().toArray(new String[0]);
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
}
