package com.worknest.features.companySite.dto;

import com.worknest.domain.enums.SiteStatus;
import com.worknest.domain.enums.SiteType;
import java.util.UUID;
import lombok.Builder;

@Builder
public record MainDetailsReadDto(
        UUID id,
        String code,
        String name,
        SiteType type,
        SiteStatus status,
        String countryCode,
        String timezone,
        String notes,
        Boolean qrEnabled,
        Boolean checkInEnabled,
        Boolean checkOutEnabled,
        Long version
) {}
