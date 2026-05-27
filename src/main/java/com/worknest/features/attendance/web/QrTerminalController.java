package com.worknest.features.attendance.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.attendance.application.AttendanceQrService;
import com.worknest.features.attendance.dto.CreateQrTerminalRequest;
import com.worknest.features.attendance.dto.QrCurrentTokenResponse;
import com.worknest.features.attendance.dto.QrTerminalSummaryDto;
import com.worknest.features.attendance.dto.QrValidateRequest;
import com.worknest.features.attendance.dto.QrValidateResponse;
import com.worknest.security.AuthSessionPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Attendance QR Terminals", description = "Manage and render dynamic QR tokens for attendance.")
public class QrTerminalController {

    private final AttendanceQrService attendanceQrService;

    @PostMapping("/api/v1/sites/{siteId}/qr-terminals")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@teamSecurity.hasCurrentCompanyPermission('QR_TERMINAL_CREATE')")
    @Operation(summary = "Create QR terminal for a site")
    public ApiResponse<QrTerminalSummaryDto> createTerminal(
            @PathVariable UUID siteId,
            @Valid @RequestBody CreateQrTerminalRequest request
    ) {
        UUID companyId = currentPrincipal().companyId();
        return ApiResponse.success("QR terminal created successfully", attendanceQrService.createTerminal(companyId, siteId, request));
    }

    @GetMapping("/api/v1/qr-terminals/{terminalId}/current")
    @PreAuthorize("@teamSecurity.hasCurrentCompanyPermission('QR_TERMINAL_VIEW')")
    @Operation(summary = "Get current QR token for terminal display")
    public ApiResponse<QrCurrentTokenResponse> currentToken(@PathVariable UUID terminalId) {
        UUID companyId = currentPrincipal().companyId();
        return ApiResponse.success("Current QR token loaded", attendanceQrService.currentToken(companyId, terminalId));
    }

    @PostMapping("/api/v1/qr-terminals/{terminalId}/refresh")
    @PreAuthorize("@teamSecurity.hasCurrentCompanyPermission('QR_TERMINAL_UPDATE')")
    @Operation(summary = "Force-refresh terminal QR token")
    public ApiResponse<QrCurrentTokenResponse> refreshToken(@PathVariable UUID terminalId) {
        UUID companyId = currentPrincipal().companyId();
        return ApiResponse.success("QR token refreshed", attendanceQrService.forceRefresh(companyId, terminalId));
    }

    @PostMapping("/api/v1/qr/validate")
    @PreAuthorize("@companySecurity.hasCurrentCompanyRole('EMPLOYEE', 'STAFF', 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Pre-validate QR token (non-consuming helper)")
    public ApiResponse<QrValidateResponse> validate(@Valid @RequestBody QrValidateRequest request) {
        UUID companyId = currentPrincipal().companyId();
        return ApiResponse.success(
                "QR validation completed",
                attendanceQrService.validateToken(companyId, request.siteId(), request.qrToken())
        );
    }

    private AuthSessionPrincipal currentPrincipal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthSessionPrincipal principal)) {
            throw new IllegalStateException("No authenticated principal found");
        }
        return principal;
    }
}
