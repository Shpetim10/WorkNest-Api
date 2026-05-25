package com.worknest.features.subscription.dto;

import com.worknest.domain.enums.SubscriptionPlan;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Plan definition and its limits")
public record PlanDetailsResponse(

        @Schema(description = "Plan identifier")
        SubscriptionPlan plan,

        @Schema(description = "Monthly price in cents (e.g. 2900 = $29.00)")
        Integer priceMonthlyInCents,

        @Schema(description = "Maximum active employees allowed (null = unlimited)")
        Integer maxEmployees,

        @Schema(description = "Maximum managers (STAFF role) allowed (null = unlimited)")
        Integer maxManagers,

        @Schema(description = "Maximum departments allowed (null = unlimited)")
        Integer maxDepartments,

        @Schema(description = "Maximum locations (company sites) allowed (null = unlimited)")
        Integer maxLocations,

        @Schema(description = "Whether payroll features are enabled")
        Boolean payrollEnabled,

        @Schema(description = "Whether audit log features are enabled")
        Boolean auditLogsEnabled
) {
}
