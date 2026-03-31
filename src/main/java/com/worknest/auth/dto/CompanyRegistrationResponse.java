package com.worknest.auth.dto;

import com.worknest.auth.domain.CompanyStatus;
import java.util.UUID;

public record CompanyRegistrationResponse(
        UUID companyId,
        UUID adminUserId,
        UUID adminRoleAssignmentId,
        CompanyStatus status,
        Boolean onboardingCompleted,
        String message
) {
}
