package com.worknest.features.dashboard.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.common.api.PaginationSupport;
import com.worknest.features.dashboard.application.AdminAuditLogService;
import com.worknest.features.dashboard.dto.AdminAuditLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin Dashboard", description = "Company-level admin dashboard API")
public class AdminAuditLogController {

    private final AdminAuditLogService auditLogService;

    @GetMapping("/audit-logs")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Get company audit log", description = "Returns paginated audit log entries scoped to the authenticated user's company")
    public ApiResponse<AdminAuditLogResponse> getAuditLog(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success(
                "Audit log retrieved successfully",
                auditLogService.getAuditLog(PaginationSupport.pageable(page, size))
        );
    }
}
