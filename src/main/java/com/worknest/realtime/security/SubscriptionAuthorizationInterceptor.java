package com.worknest.realtime.security;

import com.worknest.domain.enums.PlatformRole;
import com.worknest.security.AuthSessionPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class SubscriptionAuthorizationInterceptor implements ChannelInterceptor {

    // Pattern groups: (1) companyId, (2) resource segment (e.g. /sites, /announcements, ...)
    private static final Pattern COMPANY_TOPIC_PATTERN = Pattern.compile(
            "^/topic/companies/([0-9a-fA-F-]{36})/([a-z-]+)(/.*)?$"
    );

    // Allow any authenticated user to subscribe to their own user queue
    private static final Pattern USER_QUEUE_PATTERN = Pattern.compile(
            "^/user/queue/.*$"
    );

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            throw new AccessDeniedException("Subscription destination is required");
        }

        Authentication auth = (Authentication) accessor.getUser();
        if (auth == null || !(auth.getPrincipal() instanceof AuthSessionPrincipal principal)) {
            log.warn("WebSocket SUBSCRIBE rejected: unauthenticated session to destination={}", destination);
            throw new AccessDeniedException("Authentication required to subscribe");
        }

        // Allow user's own private queue — no company check needed
        if (USER_QUEUE_PATTERN.matcher(destination).matches()) {
            log.info("WebSocket SUBSCRIBE accepted: user={} own queue destination={}", principal.userId(), destination);
            return message;
        }

        Matcher matcher = COMPANY_TOPIC_PATTERN.matcher(destination);
        if (matcher.matches()) {
            String companyIdStr = matcher.group(1);
            String resource = matcher.group(2);
            authorizeCompanyTopicSubscription(principal, companyIdStr, resource, destination);
        }

        return message;
    }

    private void authorizeCompanyTopicSubscription(AuthSessionPrincipal principal, String companyIdStr, String resource, String destination) {
        UUID requestedCompanyId;
        try {
            requestedCompanyId = UUID.fromString(companyIdStr);
        } catch (IllegalArgumentException e) {
            throw new AccessDeniedException("Invalid company ID in subscription destination");
        }

        if (!requestedCompanyId.equals(principal.companyId())) {
            log.warn("WebSocket SUBSCRIBE rejected: cross-company attempt user={} to destination={}", principal.userId(), destination);
            throw new AccessDeniedException("Not authorized to subscribe to this company's topics");
        }

        PlatformRole role = principal.role();

        boolean authorized = switch (resource) {
            // Admin-only resources
            case "sites", "departments", "settings" ->
                    role == PlatformRole.ADMIN || role == PlatformRole.SUPERADMIN;

            // Management resources (staff + admin)
            case "leave-requests", "employees", "attendance" ->
                    role == PlatformRole.STAFF || role == PlatformRole.ADMIN || role == PlatformRole.SUPERADMIN;

            // Company-wide resources (all members)
            case "announcements" ->
                    true; // any company member: EMPLOYEE, STAFF, ADMIN, SUPERADMIN

            default -> {
                log.warn("WebSocket SUBSCRIBE rejected: unrecognized topic resource={} user={}", resource, principal.userId());
                yield false;
            }
        };

        if (!authorized) {
            log.warn("WebSocket SUBSCRIBE rejected: insufficient role={} for user={} to destination={}", role, principal.userId(), destination);
            throw new AccessDeniedException("Insufficient role to subscribe to " + resource + " topic");
        }

        log.info("WebSocket SUBSCRIBE accepted: user={} role={} destination={}", principal.userId(), role, destination);
    }
}
