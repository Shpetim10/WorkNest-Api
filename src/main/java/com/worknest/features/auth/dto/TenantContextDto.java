package com.worknest.features.auth.dto;

import com.worknest.common.i18n.Language;
import com.worknest.domain.enums.CompanyStatus;
import com.worknest.domain.enums.SubscriptionPlan;
import com.worknest.domain.enums.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Resolved tenant context for the authenticated session")
public record TenantContextDto(
        @Schema(description = "Unique company identifier")
        UUID companyId,

        @Schema(description = "Display name of the company")
        String companyName,

        @Schema(description = "Tenant slug of the company")
        String companySlug,

        @Schema(description = "Current company status")
        CompanyStatus companyStatus,

        @Schema(description = "Company logo path")
        String logoPath,

        @Schema(description = "Company timezone")
        String timezone,

        @Schema(description = "Company locale")
        Language locale,

        @Schema(description = "Company currency")
        String currency,

        @Schema(description = "Preferred company date format")
        String dateFormat,

        @Schema(description = "Timestamp when onboarding was completed")
        Instant onboardingCompletedAt,

        @Schema(description = "Current subscription plan")
        SubscriptionPlan subscriptionPlan,

        @Schema(description = "Current subscription status")
        SubscriptionStatus subscriptionStatus
) {
}
