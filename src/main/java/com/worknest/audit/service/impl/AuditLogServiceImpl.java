package com.worknest.audit.service.impl;

import com.worknest.audit.domain.AuditLog;
import com.worknest.audit.repository.AuditLogRepository;
import com.worknest.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String action, String entityType, UUID entityId, Map<String, Object> diff, Map<String, Object> metadata) {
        log.debug("Logging action: {} on {}/{}", action, entityType, entityId);
        
        // TODO: Resolve current user and company from SecurityContext
        // TODO: Build AuditLog entity
        // TODO: Save to repository
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(AuditLog auditLog) {
        log.debug("Persisting pre-constructed audit log for action: {}", auditLog.getAction());
        auditLogRepository.save(auditLog);
    }
}
