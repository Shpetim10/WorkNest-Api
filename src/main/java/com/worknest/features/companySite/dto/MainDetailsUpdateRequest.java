package com.worknest.features.companySite.dto;

import com.worknest.domain.enums.SiteStatus;
import com.worknest.domain.enums.SiteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MainDetailsUpdateRequest(
        @NotBlank(message = "Site code is required.")
        @Size(max = 50, message = "Code must not exceed 50 characters.")
        String code,

        @NotBlank(message = "Site name is required.")
        @Size(max = 255, message = "Name must not exceed 255 characters.")
        String name,

        @NotNull(message = "Site type is required.")
        SiteType type,

        @NotNull(message = "Status cannot be null.")
        SiteStatus status,

        @NotBlank(message = "Country code is required.")
        @Size(min = 2, max = 2, message = "Country code must be exactly 2 characters.")
        String countryCode,

        @NotBlank(message = "Timezone is required.")
        @Size(max = 100, message = "Timezone must not exceed 100 characters.")
        String timezone,

        String notes,

        @NotNull(message = "QR Enabled flag must be provided.")
        Boolean qrEnabled,

        @NotNull(message = "Check-in Enabled flag must be provided.")
        Boolean checkInEnabled,

        @NotNull(message = "Check-out Enabled flag must be provided.")
        Boolean checkOutEnabled,

        @NotNull(message = "Version is required for optimistic locking.")
        Long version
) {}
