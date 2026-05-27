package com.worknest.features.dashboard.dto;

import java.util.List;

public record AdminAuditLogResponse(
        List<AuditLogEntry> content,
        long totalElements,
        int totalPages,
        int size,
        int number,
        boolean first,
        boolean last
) {

    public record AuditLogEntry(
            String id,
            String user,
            String role,
            String action,
            String details,
            String timestamp,
            String referenceId
    ) {}
}