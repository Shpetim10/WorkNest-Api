package com.worknest.audit.service;

import com.worknest.audit.domain.AuditLog;
import java.util.Map;
import java.util.UUID;

public interface AuditLogService {

    /**
     * Logs a platform action for audit purposes.
     *
     * @param action the action name
     * @param entityType the type of entity being acted upon
     * @param entityId the ID of the entity
     * @param diff the changes made (JSON)
     * @param metadata additional context (JSON)
     */
    void logAction(String action, String entityType, UUID entityId, Map<String, Object> diff, Map<String, Object> metadata);

    /**
     * Persists a pre-constructed AuditLog entity.
     *
     * @param auditLog the audit log entity
     */
    void logAction(AuditLog auditLog);
}
