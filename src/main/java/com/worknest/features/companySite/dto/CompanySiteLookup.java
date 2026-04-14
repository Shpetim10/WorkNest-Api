package com.worknest.features.companySite.dto;

import com.worknest.domain.entities.CompanySite;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Lightweight company site projection used for dropdowns and selection lists")
public record CompanySiteLookup(

        @Schema(description = "Site ID")
        UUID id,

        @Schema(description = "Short site code (e.g. HQ, TIRANA-1)")
        String code,

        @Schema(description = "Human-readable site name")
        String name
) {
    public static CompanySiteLookup fromEntity(CompanySite site) {
        return new CompanySiteLookup(
                site.getId(),
                site.getCode(),
                site.getName()
        );
    }
}
