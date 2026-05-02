package com.worknest.features.company.web;

import com.worknest.common.api.ApiErrorResponse;
import com.worknest.common.api.ApiResponse;
import com.worknest.features.company.application.CompanySettingsService;
import com.worknest.features.company.dto.CompanySettingsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/companies/{companyId}/settings")
@Tag(name = "Company Settings", description = "Read company-level configuration: timezone, date format, currency, locale, and country.")
@SecurityRequirement(name = "bearerAuth")
public class CompanySettingsController {

    private final CompanySettingsService companySettingsService;

    @GetMapping
    @PreAuthorize("@companySecurity.hasCompanyRole(#companyId, 'ADMIN', 'SUPERADMIN', 'STAFF', 'EMPLOYEE')")
    @Operation(
            summary = "Get company settings",
            description = """
                    Returns the core configuration settings for the given company.

                    These settings govern how dates, times, and monetary values are formatted
                    across all features within the company workspace:

                    - **timezone** — IANA identifier (e.g. `Europe/Tirane`) used for attendance, scheduling, and display.
                    - **dateFormat** — UI date format token (e.g. `DD/MM/YYYY`).
                    - **currency** — ISO 4217 code (e.g. `ALL`, `EUR`, `USD`).
                    - **locale** — BCP 47 language code (e.g. `SQ`, `EN`).
                    - **countryCode** — ISO 3166-1 alpha-2 country code (e.g. `AL`).
                    - **logoPath** — Public URL of the company logo, or `null` if not uploaded.

                    **Authorization:** Any authenticated member of the company (ADMIN, MANAGER, EMPLOYEE).
                    Tenant isolation is enforced — callers can only read settings for their own company.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Company settings retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthenticated — missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden — caller does not belong to this company",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Company not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ApiResponse<CompanySettingsResponse> getSettings(@PathVariable UUID companyId) {
        return ApiResponse.success(
                "Company settings retrieved successfully",
                companySettingsService.getSettings(companyId)
        );
    }
}
