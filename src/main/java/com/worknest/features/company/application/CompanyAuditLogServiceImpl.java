package com.worknest.features.company.application;

import com.worknest.audit.domain.AuditLog;
import com.worknest.audit.repository.AuditLogRepository;
import com.worknest.common.exception.BusinessException;
import com.worknest.features.company.dto.CompanyAuditLogResponse;
import com.worknest.tenant.TenantContextHolder;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyAuditLogServiceImpl implements CompanyAuditLogService {

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private final AuditLogRepository auditLogRepository;

    @Override
    public CompanyAuditLogResponse getAuditLog(UUID companyId, String action, Instant from, Instant to, Pageable pageable) {
        UUID tenantId = TenantContextHolder.get()
                .map(ctx -> ctx.companyId())
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "TENANT_CONTEXT_MISSING", "No tenant context found."));

        if (!tenantId.equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "TENANT_MISMATCH", "Access denied to the requested company.");
        }

        Instant resolvedFrom = from != null ? from : Instant.EPOCH;
        Instant resolvedTo = to != null ? to : Instant.now();

        Page<AuditLog> page = action != null && !action.isBlank()
                ? auditLogRepository.findAllByCompanyIdAndActionAndCreatedAtBetweenOrderByCreatedAtDesc(companyId, action.trim().toUpperCase(), resolvedFrom, resolvedTo, pageable)
                : auditLogRepository.findAllByCompanyIdAndCreatedAtBetweenOrderByCreatedAtDesc(companyId, resolvedFrom, resolvedTo, pageable);

        return new CompanyAuditLogResponse(
                page.getContent().stream().map(this::toRow).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    private CompanyAuditLogResponse.AuditLogRow toRow(AuditLog log) {
        return new CompanyAuditLogResponse.AuditLogRow(
                log.getId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId() != null ? log.getEntityId().toString() : null,
                log.getActorUserId() != null ? log.getActorUserId().toString() : null,
                log.getActorRole() != null ? log.getActorRole().name() : null,
                log.getActorJobTitle(),
                log.getDiff(),
                log.getMetadata(),
                TIMESTAMP_FMT.format(log.getCreatedAt())
        );
    }
}
