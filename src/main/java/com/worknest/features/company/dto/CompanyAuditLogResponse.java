package com.worknest.features.company.dto;

import java.util.List;
import java.util.Map;

public record CompanyAuditLogResponse(
        List<AuditLogRow> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public record AuditLogRow(
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
