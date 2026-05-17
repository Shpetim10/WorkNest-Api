package com.worknest.features.superAdmin.application;

import com.worknest.features.superAdmin.dto.SuperAdminAuditLogResponse;
import org.springframework.data.domain.Pageable;

public interface SuperAdminAuditLogService {

    SuperAdminAuditLogResponse getAuditLog(String search, Pageable pageable);
}