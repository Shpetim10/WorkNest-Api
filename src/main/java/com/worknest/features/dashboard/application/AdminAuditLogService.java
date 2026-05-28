package com.worknest.features.dashboard.application;

import com.worknest.features.dashboard.dto.AdminAuditLogResponse;
import java.time.Instant;
import org.springframework.data.domain.Pageable;

public interface AdminAuditLogService {
    AdminAuditLogResponse getAuditLog(String action, Instant from, Instant to, Pageable pageable);
}
