package com.worknest.features.payroll.web;

import com.worknest.common.api.ApiResponse;
import com.worknest.features.payroll.application.PayrollSettingsService;
import com.worknest.features.payroll.dto.PayrollDtos.PayrollSettingsResponse;
import com.worknest.features.payroll.dto.PayrollDtos.PublicHolidayRequest;
import com.worknest.features.payroll.dto.PayrollDtos.PublicHolidayResponse;
import com.worknest.features.payroll.dto.PayrollDtos.ReplaceTaxBracketsRequest;
import com.worknest.features.payroll.dto.PayrollDtos.TaxBracketResponse;
import com.worknest.features.payroll.dto.PayrollDtos.UpsertPayrollSettingsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/payroll/settings")
@Tag(name = "Admin Payroll Settings", description = "Company-level payroll configuration: work-week, statutory rates, tax brackets, public holidays.")
@PreAuthorize("@companySecurity.hasCurrentCompanyRole('ADMIN', 'SUPERADMIN')")
public class AdminPayrollSettingsController {

    private final PayrollSettingsService settingsService;

    @GetMapping
    @Operation(summary = "Get current company payroll settings (returns system defaults when not configured)")
    public ApiResponse<PayrollSettingsResponse> getSettings() {
        return ApiResponse.success("Payroll settings retrieved", settingsService.getSettings());
    }

    @PutMapping
    @Operation(summary = "Create or update company payroll settings (work-week, daily hours, statutory rates)")
    public ApiResponse<PayrollSettingsResponse> upsertSettings(
            @Valid @RequestBody UpsertPayrollSettingsRequest request) {
        return ApiResponse.success("Payroll settings updated", settingsService.upsertSettings(request));
    }

    @GetMapping("/tax-brackets")
    @Operation(summary = "Get progressive income tax brackets for the company")
    public ApiResponse<List<TaxBracketResponse>> getTaxBrackets() {
        return ApiResponse.success("Tax brackets retrieved", settingsService.getTaxBrackets());
    }

    @PutMapping("/tax-brackets")
    @Operation(summary = "Replace all company tax brackets (must be contiguous, ordered, open-ended top bracket)")
    public ApiResponse<List<TaxBracketResponse>> replaceTaxBrackets(
            @Valid @RequestBody ReplaceTaxBracketsRequest request) {
        return ApiResponse.success("Tax brackets replaced", settingsService.replaceTaxBrackets(request));
    }

    @GetMapping("/holidays")
    @Operation(summary = "List resolved public holidays for a year (recurring entries expanded)")
    public ApiResponse<List<PublicHolidayResponse>> getHolidays(
            @RequestParam int year) {
        return ApiResponse.success("Public holidays retrieved", settingsService.getHolidays(year));
    }

    @PostMapping("/holidays")
    @Operation(summary = "Create a public holiday entry for the company")
    public ApiResponse<PublicHolidayResponse> createHoliday(
            @Valid @RequestBody PublicHolidayRequest request) {
        return ApiResponse.success("Public holiday created", settingsService.createHoliday(request));
    }

    @PutMapping("/holidays/{id}")
    @Operation(summary = "Update a public holiday entry")
    public ApiResponse<PublicHolidayResponse> updateHoliday(
            @PathVariable UUID id,
            @Valid @RequestBody PublicHolidayRequest request) {
        return ApiResponse.success("Public holiday updated", settingsService.updateHoliday(id, request));
    }

    @DeleteMapping("/holidays/{id}")
    @Operation(summary = "Delete a public holiday entry (blocked if any locked payroll period consumed it)")
    public ApiResponse<Void> deleteHoliday(@PathVariable UUID id) {
        settingsService.deleteHoliday(id);
        return ApiResponse.success("Public holiday deleted", null);
    }
}
