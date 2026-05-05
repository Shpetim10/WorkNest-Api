package com.worknest.features.company.web;

import com.worknest.common.api.ApiErrorResponse;
import com.worknest.common.api.ApiResponse;
import com.worknest.features.company.application.CompanySettingsService;
import com.worknest.features.company.dto.CompanySettingsResponse;
import com.worknest.features.company.dto.UpdateCompanySettingsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/companies/{companyId}/settings")
@Tag(name = "Company Settings", description = "Read and update company-level configuration.")
@SecurityRequirement(name = "bearerAuth")
public class CompanySettingsController {

    private final CompanySettingsService companySettingsService;

    @GetMapping
    @PreAuthorize("@companySecurity.hasCompanyRole(#companyId, 'ADMIN', 'SUPERADMIN', 'STAFF', 'EMPLOYEE')")
    @Operation(summary = "Get company settings")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Company settings retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Company not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ApiResponse<CompanySettingsResponse> getSettings(@PathVariable UUID companyId) {
        return ApiResponse.success("Company settings retrieved successfully", companySettingsService.getSettings(companyId));
    }

    @PutMapping
    @PreAuthorize("@companySecurity.hasCompanyRole(#companyId, 'ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Update company settings", description = "Updates editable company fields. Email cannot be changed here.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Company settings updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Company not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "NIPT already in use", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ApiResponse<CompanySettingsResponse> updateSettings(
            @PathVariable UUID companyId,
            @Valid @RequestBody UpdateCompanySettingsRequest request
    ) {
        return ApiResponse.success("Company settings updated successfully", companySettingsService.updateSettings(companyId, request));
    }
}