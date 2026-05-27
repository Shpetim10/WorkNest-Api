package com.worknest.features.dashboard.application;

import com.worknest.audit.domain.AuditLog;
import com.worknest.common.plan.PlanEnforcementService;
import com.worknest.domain.entities.User;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.dashboard.dto.AdminAuditLogResponse;
import com.worknest.features.dashboard.repository.AdminAuditLogQueryRepository;
import com.worknest.security.AuthSessionPrincipal;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm 'UTC'", Locale.ENGLISH).withZone(ZoneOffset.UTC);

    private final AdminAuditLogQueryRepository queryRepository;
    private final UserRepository userRepository;
    private final PlanEnforcementService planEnforcementService;

    @Override
    public AdminAuditLogResponse getAuditLog(Pageable pageable) {
        UUID companyId = principal().companyId();
        planEnforcementService.assertAuditLogsEnabled(companyId);

        Page<AuditLog> page = queryRepository.findByCompanyId(companyId, pageable);

        Set<UUID> actorIds = page.getContent().stream()
                .map(AuditLog::getActorUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, String> actorNames = userRepository.findAllById(actorIds).stream()
                .collect(Collectors.toMap(User::getId, this::resolveDisplayName));

        List<AdminAuditLogResponse.AuditLogEntry> content = page.getContent().stream()
                .map(log -> toEntry(log, actorNames))
                .toList();

        return new AdminAuditLogResponse(
                content,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getSize(),
                page.getNumber(),
                page.isFirst(),
                page.isLast()
        );
    }

    private AdminAuditLogResponse.AuditLogEntry toEntry(AuditLog log, Map<UUID, String> actorNames) {
        String actor = log.getActorUserId() != null
                ? actorNames.getOrDefault(log.getActorUserId(), "System")
                : "System";

        String role = log.getActorRole() != null ? log.getActorRole().name() : "";
        String referenceId = log.getEntityId() != null ? log.getEntityId().toString() : null;

        return new AdminAuditLogResponse.AuditLogEntry(
                String.valueOf(log.getId()),
                actor,
                role,
                log.getAction(),
                buildDetails(log),
                TIMESTAMP_FMT.format(log.getCreatedAt()),
                referenceId
        );
    }

    private String buildDetails(AuditLog log) {
        if (log.getDiff() != null && !log.getDiff().isEmpty()) {
            return "Modified: " + String.join(", ", log.getDiff().keySet());
        }
        if (log.getMetadata() != null && !log.getMetadata().isEmpty()) {
            return String.join(", ", log.getMetadata().keySet());
        }
        return log.getEntityType();
    }

    private String resolveDisplayName(User user) {
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName();
        }
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }

    private AuthSessionPrincipal principal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthSessionPrincipal p)) {
            throw new IllegalStateException("No authenticated principal");
        }
        return p;
    }
}
