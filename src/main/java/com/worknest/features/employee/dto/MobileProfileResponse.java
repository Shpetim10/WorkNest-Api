package com.worknest.features.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record MobileProfileResponse(
        @Schema(description = "First name of the employee")
        String firstName,

        @Schema(description = "Last name of the employee")
        String lastName,

        @Schema(description = "URL of the employee's profile picture")
        String profilePictureUrl,

        @Schema(description = "Job title or position of the employee")
        String jobTitle,

        @Schema(description = "Department name of the employee")
        String department,

        @Schema(description = "Location or city of the employee")
        String location,

        @Schema(description = "Platform role of the employee")
        String role,

        @Schema(description = "Email of the employee")
        String email,

        @Schema(description = "Department name of the employee")
        String departmentName,

        @Schema(description = "Site name of the employee")
        String siteName,

        @Schema(description = "Contract type of the employee")
        String contractType,

        @Schema(description = "Contract status of the employee")
        String contractStatus,

        @Schema(description = "Contract start date of the employee")
        LocalDate contractStartDate,

        @Schema(description = "Contract end date of the employee")
        LocalDate contractEndDate
) {}
