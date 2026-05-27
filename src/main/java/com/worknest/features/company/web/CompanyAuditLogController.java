package com.worknest.features.company.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.common.api.PaginationSupport;
import com.worknest.features.company.application.CompanyAuditLogService;
import com.worknest.features.company.dto.CompanyAuditLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/companies/{companyId}/audit-log")
@Tag(name = "Company Audit Log", description = "Company-scoped audit log API")
public class CompanyAuditLogController {

    private final CompanyAuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("@teamSecurity.hasPermission(#companyId, 'AUDIT_LOG_VIEW')")
    @Operation(summary = "Get company audit log", description = "Returns paginated audit events scoped to the authenticated company")
    public ApiResponse<CompanyAuditLogResponse> getAuditLog(
            @PathVariable UUID companyId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success(
                "Audit log retrieved successfully",
                auditLogService.getAuditLog(companyId, action, from, to, PaginationSupport.pageable(page, size))
        );
    }
}
