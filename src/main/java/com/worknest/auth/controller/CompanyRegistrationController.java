package com.worknest.auth.controller;

import com.worknest.auth.dto.CompanyRegistrationRequest;
import com.worknest.auth.dto.CompanyRegistrationResponse;
import com.worknest.auth.service.CompanyRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyRegistrationController {

    private final CompanyRegistrationService registrationService;

    /**
     * Registers a new company with its initial administrator user.
     *
     * @param request information required for company and admin registration
     * @return the newly registered company and admin details
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyRegistrationResponse register(@RequestBody @Valid CompanyRegistrationRequest request) {
        return registrationService.registerCompany(request);
    }
}
