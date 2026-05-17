package com.worknest.features.superAdmin.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.common.api.PaginationSupport;
import com.worknest.features.superAdmin.application.SuperAdminAuditLogService;
import com.worknest.features.superAdmin.dto.SuperAdminAuditLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/super-admin")
@RequiredArgsConstructor
@Tag(name = "Super Admin", description = "Platform-level super admin API")
public class SuperAdminAuditLogController {

    private final SuperAdminAuditLogService auditLogService;

    @GetMapping("/audit-log")
    @PreAuthorize("@superAdminSecurity.isSuperAdmin()")
    @Operation(summary = "Get audit log", description = "Returns paginated platform-level audit events with optional search")
    public ApiResponse<SuperAdminAuditLogResponse> getAuditLog(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.success(
                "Audit log retrieved successfully",
                auditLogService.getAuditLog(search, PaginationSupport.pageable(page, size))
        );
    }
}