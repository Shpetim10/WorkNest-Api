package com.worknest.auth.controller;

import com.worknest.auth.dto.CompanyRegistrationRequest;
import com.worknest.auth.dto.CompanyRegistrationResponse;
import com.worknest.auth.service.CompanyRegistrationService;
import com.worknest.common.api.ApiResponse;
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
public class CompanyRegistrationController {

    private final CompanyRegistrationService companyRegistrationService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<CompanyRegistrationResponse>> registerCompany(
            @Valid @RequestBody CompanyRegistrationRequest request
    ) {
        CompanyRegistrationResponse response = companyRegistrationService.registerCompany(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response.message(), response));
    }
}
