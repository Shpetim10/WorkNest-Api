package com.worknest.auth.dto;

import com.worknest.auth.domain.CompanyStatus;
import java.util.UUID;

public record CompanyRegistrationResponse(
        UUID companyId,
        UUID adminUserId,
        String companyName,
        String companySlug,
        CompanyStatus companyStatus
) {
}
