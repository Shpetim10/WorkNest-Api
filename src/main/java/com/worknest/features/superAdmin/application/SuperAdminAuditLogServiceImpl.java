package com.worknest.features.superAdmin.application;

import com.worknest.audit.domain.PlatformEvent;
import com.worknest.domain.entities.User;
import com.worknest.features.auth.repository.UserRepository;
import com.worknest.features.superAdmin.dto.SuperAdminAuditLogResponse;
import com.worknest.features.superAdmin.repository.SuperAdminAuditLogQueryRepository;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuperAdminAuditLogServiceImpl implements SuperAdminAuditLogService {

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private final SuperAdminAuditLogQueryRepository auditLogQueryRepository;
    private final UserRepository userRepository;

    @Override
    public SuperAdminAuditLogResponse getAuditLog(String search, Pageable pageable) {
        Page<PlatformEvent> page = auditLogQueryRepository.findEvents(search, pageable);

        Set<UUID> actorIds = page.getContent().stream()
                .filter(e -> e.getActorUserId() != null)
                .map(PlatformEvent::getActorUserId)
                .collect(Collectors.toSet());

        Map<UUID, String> actorNames = userRepository.findAllById(actorIds).stream()
                .collect(Collectors.toMap(User::getId, this::resolveDisplayName));

        List<SuperAdminAuditLogResponse.AuditLogRow> rows = page.getContent().stream()
                .map(e -> toRow(e, actorNames))
                .toList();

        long todayCount = auditLogQueryRepository.countToday();
        SuperAdminAuditLogResponse.AuditLogSummary summary = new SuperAdminAuditLogResponse.AuditLogSummary(
                page.getTotalElements(), 0L, 0L, todayCount);

        return new SuperAdminAuditLogResponse(
                rows,
                summary,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    private SuperAdminAuditLogResponse.AuditLogRow toRow(PlatformEvent event, Map<UUID, String> actorNames) {
        String actor = event.getActorUserId() != null
                ? actorNames.getOrDefault(event.getActorUserId(), "System")
                : "System";

        return new SuperAdminAuditLogResponse.AuditLogRow(
                String.valueOf(event.getId()),
                event.getEventType(),
                event.getCompanyName() != null ? event.getCompanyName() : "",
                event.getDescription() != null ? event.getDescription() : "",
                actor,
                TIMESTAMP_FMT.format(event.getCreatedAt()),
                "info"
        );
    }

    private String resolveDisplayName(User user) {
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName();
        }
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }
}
