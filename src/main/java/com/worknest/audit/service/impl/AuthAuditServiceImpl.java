package com.worknest.audit.service.impl;

import com.worknest.audit.domain.AuditLog;
import com.worknest.audit.domain.PlatformEvent;
import com.worknest.audit.service.AuditLogService;
import com.worknest.audit.service.AuthAuditService;
import com.worknest.audit.service.PlatformEventService;
import com.worknest.audit.service.model.AuthAuditActorContext;
import com.worknest.audit.service.model.AuthSessionContext;
import com.worknest.domain.enums.PlatformAccess;
import com.worknest.domain.enums.PlatformRole;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthAuditServiceImpl implements AuthAuditService {

    private final AuditLogService auditLogService;
    private final PlatformEventService platformEventService;

    @Override
    public void appendLoginSuccess(
            AuthAuditActorContext actorContext,
            AuthSessionContext sessionContext,
            String email,
            String userAgent
    ) {
        auditLogService.logAction(buildAuditLog(
                actorContext,
                "LOGIN_SUCCESS",
                "User",
                sessionContext.userId(),
                mapOf(
                        "email", email,
                        "roleAssignmentId", sessionContext.roleAssignmentId(),
                        "role", sessionContext.role() != null ? sessionContext.role().name() : null,
                        "platformAccess", sessionContext.platformAccess() != null ? sessionContext.platformAccess().name() : null
                ),
                mapOf("userAgent", userAgent)
        ));

        platformEventService.publishEvent(new PlatformEvent(
                "LOGIN_SUCCESS",
                actorContext.companyId(),
                actorContext.companyName(),
                actorContext.actorUserId(),
                "User login completed successfully"
        ));
    }

    @Override
    public void appendLoginFailure(
            UUID companyId,
            String companyName,
            String companySlug,
            String email,
            PlatformAccess platformAccess,
            String reason,
            String ipAddress
    ) {
        auditLogService.logAction(new AuditLog(
                companyId,
                null,
                null,
                null,
                null,
                "LOGIN_FAILURE",
                "User",
                null,
                mapOf(
                        "companySlug", companySlug,
                        "email", email,
                        "platformAccess", platformAccess != null ? platformAccess.name() : null
                ),
                mapOf("reason", reason),
                ipAddress
        ));

        platformEventService.publishEvent(new PlatformEvent(
                "LOGIN_FAILURE",
                companyId,
                companyName,
                null,
                "User login attempt failed"
        ));
    }

    @Override
    public void appendInvitationCreated(
            AuthAuditActorContext actorContext,
            UUID invitationId,
            String invitedEmail,
            PlatformRole platformRole,
            PlatformAccess platformAccess,
            String invitedJobTitle,
            Instant expiresAt
    ) {
        auditLogService.logAction(buildAuditLog(
                actorContext,
                "INVITATION_CREATED",
                "UserInvitation",
                invitationId,
                mapOf(
                        "email", invitedEmail,
                        "platformRole", platformRole != null ? platformRole.name() : null,
                        "platformAccess", platformAccess != null ? platformAccess.name() : null,
                        "invitedJobTitle", invitedJobTitle,
                        "expiresAt", expiresAt != null ? expiresAt.toString() : null
                ),
                Map.of()
        ));

        platformEventService.publishEvent(new PlatformEvent(
                "INVITATION_CREATED",
                actorContext.companyId(),
                actorContext.companyName(),
                actorContext.actorUserId(),
                "User invitation created"
        ));
    }

    @Override
    public void appendInvitationActivated(
            AuthAuditActorContext actorContext,
            UUID invitationId,
            UUID activatedUserId,
            AuthSessionContext sessionContext
    ) {
        auditLogService.logAction(buildAuditLog(
                actorContext,
                "INVITATION_ACTIVATED",
                "User",
                activatedUserId,
                mapOf(
                        "invitationId", invitationId,
                        "roleAssignmentId", sessionContext.roleAssignmentId(),
                        "role", sessionContext.role() != null ? sessionContext.role().name() : null,
                        "platformAccess", sessionContext.platformAccess() != null ? sessionContext.platformAccess().name() : null
                ),
                Map.of()
        ));

        platformEventService.publishEvent(new PlatformEvent(
                "INVITATION_ACTIVATED",
                actorContext.companyId(),
                actorContext.companyName(),
                activatedUserId,
                "Invitation activated"
        ));
    }

    @Override
    public void appendPasswordChanged(
            AuthAuditActorContext actorContext,
            UUID userId,
            String resetMethod
    ) {
        auditLogService.logAction(buildAuditLog(
                actorContext,
                "PASSWORD_CHANGED",
                "User",
                userId,
                mapOf("resetMethod", resetMethod),
                Map.of()
        ));

        platformEventService.publishEvent(new PlatformEvent(
                "PASSWORD_CHANGED",
                actorContext.companyId(),
                actorContext.companyName(),
                userId,
                "User password changed"
        ));
    }

    @Override
    public void appendCompanyRegistered(
            AuthAuditActorContext actorContext,
            UUID registeredCompanyId,
            UUID adminUserId,
            UUID adminRoleAssignmentId,
            String subscriptionStatus
    ) {
        auditLogService.logAction(buildAuditLog(
                actorContext,
                "COMPANY_REGISTERED",
                "Company",
                registeredCompanyId,
                mapOf(
                        "adminUserId", adminUserId,
                        "adminRoleAssignmentId", adminRoleAssignmentId,
                        "subscriptionStatus", subscriptionStatus
                ),
                Map.of("workspaceCreated", true)
        ));

        platformEventService.publishEvent(new PlatformEvent(
                "COMPANY_REGISTERED",
                actorContext.companyId(),
                actorContext.companyName(),
                adminUserId,
                "Company workspace registered"
        ));
    }

    @Override
    public void appendRoleSelected(
            AuthAuditActorContext actorContext,
            AuthSessionContext sessionContext,
            String userAgent
    ) {
        auditLogService.logAction(buildAuditLog(
                actorContext,
                "ROLE_SELECTED",
                "RoleAssignment",
                sessionContext.roleAssignmentId(),
                mapOf(
                        "role", sessionContext.role() != null ? sessionContext.role().name() : null,
                        "platformAccess", sessionContext.platformAccess() != null ? sessionContext.platformAccess().name() : null
                ),
                mapOf("userAgent", userAgent)
        ));

        platformEventService.publishEvent(new PlatformEvent(
                "ROLE_SELECTED",
                actorContext.companyId(),
                actorContext.companyName(),
                actorContext.actorUserId(),
                "Role selected for session"
        ));
    }

    @Override
    public void appendTokenRefreshed(
            AuthAuditActorContext actorContext,
            AuthSessionContext sessionContext,
            UUID previousRefreshTokenId
    ) {
        auditLogService.logAction(buildAuditLog(
                actorContext,
                "TOKEN_REFRESHED",
                "RefreshToken",
                previousRefreshTokenId,
                mapOf(
                        "roleAssignmentId", sessionContext.roleAssignmentId(),
                        "role", sessionContext.role() != null ? sessionContext.role().name() : null,
                        "platformAccess", sessionContext.platformAccess() != null ? sessionContext.platformAccess().name() : null
                ),
                Map.of()
        ));

        platformEventService.publishEvent(new PlatformEvent(
                "TOKEN_REFRESHED",
                actorContext.companyId(),
                actorContext.companyName(),
                actorContext.actorUserId(),
                "Refresh token rotated"
        ));
    }

    private AuditLog buildAuditLog(
            AuthAuditActorContext actorContext,
            String action,
            String entityType,
            UUID entityId,
            Map<String, Object> diff,
            Map<String, Object> metadata
    ) {
        return new AuditLog(
                actorContext.companyId(),
                actorContext.actorUserId(),
                actorContext.actorRoleAssignmentId(),
                actorContext.actorRole(),
                actorContext.actorJobTitle(),
                action,
                entityType,
                entityId,
                diff,
                metadata,
                actorContext.ipAddress()
        );
    }

    private Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            Object key = keyValues[i];
            Object value = keyValues[i + 1];
            if (key instanceof String stringKey && isMeaningful(value)) {
                values.put(stringKey, value);
            }
        }
        return values;
    }

    private boolean isMeaningful(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String stringValue) {
            return StringUtils.hasText(stringValue);
        }
        return true;
    }
}
