package com.worknest.features.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MobileProfileResponse(
        @Schema(description = "First name of the employee")
        String firstName,

        @Schema(description = "Last name of the employee")
        String lastName,

        @Schema(description = "URL of the employee's profile picture")
        String profilePictureUrl
) {}
