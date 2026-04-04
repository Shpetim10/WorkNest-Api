package com.worknest.auth.controller;

import com.worknest.auth.dto.CompanyRegistrationRequest;
import com.worknest.auth.dto.CompanyRegistrationResponse;
import com.worknest.auth.service.CompanyRegistrationService;
import com.worknest.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
@Tag(name = "Company Registration", description = "Step 1 of the business onboarding flow")
public class CompanyRegistrationController {

    private final CompanyRegistrationService companyRegistrationService;

    @PostMapping("/register")
    @Operation(
            summary = "Register a new company workspace",
            description = """
                    **Step 1 of 2 in the business onboarding flow.**

                    Creates the company workspace and an initial admin user, both in `PENDING` status.
                    A secure activation invitation is dispatched via e-mail to `adminEmail`.

                    The admin must complete Step 2 (`POST /api/v1/auth/invitations/activate`) to:
                    - set their account password
                    - accept the Terms of Service / GDPR consent
                    - transition the company to `ACTIVE`

                    The raw activation token is **never** returned in this response body —
                    it travels exclusively via the invitation e-mail.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Company workspace created and activation invitation dispatched"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Validation error — missing or invalid fields",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "A company with this slug already exists",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error during company registration",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    public ResponseEntity<ApiResponse<CompanyRegistrationResponse>> registerCompany(
            @Valid @RequestBody CompanyRegistrationRequest request
    ) {
        CompanyRegistrationResponse response = companyRegistrationService.registerCompany(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response.message(), response));
    }
}
