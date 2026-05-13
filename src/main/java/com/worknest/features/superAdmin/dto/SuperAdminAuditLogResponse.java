package com.worknest.features.superAdmin.dto;

import java.util.List;

public record SuperAdminAuditLogResponse(
        List<AuditLogRow> content,
        AuditLogSummary summary,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public record AuditLogRow(
            String id,
            String event,
            String company,
            String description,
            String actorName,
            String timestamp,
            String severity
    ) {}

    public record AuditLogSummary(
            long infoEvents,
            long warnings,
            long errors,
            long today
    ) {}
}