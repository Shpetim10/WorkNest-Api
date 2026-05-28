package com.worknest.features.dashboard.dto;

import java.util.List;
import java.util.Map;

public record AdminAuditLogResponse(
        List<AuditLogEntry> content,
        long totalElements,
        int totalPages,
        int pageSize,
        int pageNumber,
        boolean first,
        boolean last
) {

    public record AuditLogEntry(
            long id,
            String action,
            String entityType,
            String entityId,
            String actorUserId,
            String actorRole,
            String actorJobTitle,
            Map<String, Object> diff,
            Map<String, Object> metadata,
            String createdAt
    ) {}
}