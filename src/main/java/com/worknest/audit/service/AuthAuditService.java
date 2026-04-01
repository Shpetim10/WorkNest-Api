package com.worknest.audit.service;

import com.worknest.audit.service.model.AuthAuditActorContext;
import com.worknest.audit.service.model.AuthSessionContext;
import com.worknest.auth.domain.PlatformAccess;
import com.worknest.auth.domain.PlatformRole;
import java.time.Instant;
import java.util.UUID;

public interface AuthAuditService {

    void appendLoginSuccess(
            AuthAuditActorContext actorContext,
            AuthSessionContext sessionContext,
            String email,
            String userAgent
    );

    void appendLoginFailure(
            UUID companyId,
            String companyName,
            String companySlug,
            String email,
            PlatformAccess platformAccess,
            String reason,
            String ipAddress
    );

    void appendInvitationCreated(
            AuthAuditActorContext actorContext,
            UUID invitationId,
            String invitedEmail,
            PlatformRole platformRole,
            PlatformAccess platformAccess,
            String invitedJobTitle,
            Instant expiresAt
    );

    void appendInvitationActivated(
            AuthAuditActorContext actorContext,
            UUID invitationId,
            UUID activatedUserId,
            AuthSessionContext sessionContext
    );

    void appendPasswordChanged(
            AuthAuditActorContext actorContext,
            UUID userId,
            String resetMethod
    );

    void appendCompanyRegistered(
            AuthAuditActorContext actorContext,
            UUID registeredCompanyId,
            UUID adminUserId,
            UUID adminRoleAssignmentId,
            String subscriptionStatus
    );

    void appendRoleSelected(
            AuthAuditActorContext actorContext,
            AuthSessionContext sessionContext,
            String userAgent
    );

    void appendTokenRefreshed(
            AuthAuditActorContext actorContext,
            AuthSessionContext sessionContext,
            UUID previousRefreshTokenId
    );
}
