package com.worknest.features.company.application;

import com.worknest.features.company.dto.CompanyAuditLogResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface CompanyAuditLogService {

    CompanyAuditLogResponse getAuditLog(UUID companyId, String action, Instant from, Instant to, Pageable pageable);
}
