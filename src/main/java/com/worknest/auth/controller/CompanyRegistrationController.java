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
@Tag(name = "Company Registration", description = "Endpoints for onboarding new companies and their initial settings")
public class CompanyRegistrationController {

    private final CompanyRegistrationService companyRegistrationService;

    @PostMapping("/register")
    @Operation(
            summary = "Register a new company",
            description = "Creates a new company and its initial administrator user. This is the first step in onboarding."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Company registered successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid registration data",
            content = @Content(schema = @Schema(implementation = com.worknest.common.api.ApiErrorResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Company with this email or name already exists",
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
