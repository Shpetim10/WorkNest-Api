package com.worknest.features.dashboard.application;

import com.worknest.features.dashboard.dto.AdminAuditLogResponse;
import org.springframework.data.domain.Pageable;

public interface AdminAuditLogService {
    AdminAuditLogResponse getAuditLog(Pageable pageable);
}
