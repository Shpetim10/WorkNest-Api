package com.worknest.features.dashboard.application;

import com.worknest.audit.domain.AuditLog;
import com.worknest.common.plan.PlanEnforcementService;
import com.worknest.features.dashboard.dto.AdminAuditLogResponse;
import com.worknest.features.dashboard.repository.AdminAuditLogQueryRepository;
import com.worknest.security.AuthSessionPrincipal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuditLogServiceImpl implements AdminAuditLogService {

    private final AdminAuditLogQueryRepository queryRepository;
    private final PlanEnforcementService planEnforcementService;

    @Override
    public AdminAuditLogResponse getAuditLog(String action, Instant from, Instant to, Pageable pageable) {
        UUID companyId = principal().companyId();
        planEnforcementService.assertAuditLogsEnabled(companyId);

        Page<AuditLog> page = queryRepository.findByCompanyId(companyId, action, from, to, pageable);

        return new AdminAuditLogResponse(
                page.getContent().stream().map(this::toEntry).toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getSize(),
                page.getNumber(),
                page.isFirst(),
                page.isLast()
        );
    }

    private AdminAuditLogResponse.AuditLogEntry toEntry(AuditLog log) {
        return new AdminAuditLogResponse.AuditLogEntry(
                log.getId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId() != null ? log.getEntityId().toString() : null,
                log.getActorUserId() != null ? log.getActorUserId().toString() : null,
                log.getActorRole() != null ? log.getActorRole().name() : "",
                log.getActorJobTitle(),
                log.getDiff(),
                log.getMetadata(),
                log.getCreatedAt().toString()
        );
    }

    private AuthSessionPrincipal principal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthSessionPrincipal p)) {
            throw new IllegalStateException("No authenticated principal");
        }
        return p;
    }
}
