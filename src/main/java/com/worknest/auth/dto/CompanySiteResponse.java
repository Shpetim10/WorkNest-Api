package com.worknest.auth.dto;

import com.worknest.auth.domain.GeofenceShapeType;
import com.worknest.auth.domain.SiteStatus;
import com.worknest.auth.domain.SiteType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Response containing details of a company site/location")
public record CompanySiteResponse(
        @Schema(description = "Unique ID of the site")
        UUID id,

        @Schema(description = "ID of the company this site belongs to")
        UUID companyId,

        @Schema(description = "Internal code for the site", example = "HQ-TIR")
        String code,

        @Schema(description = "Display name of the site", example = "Tirana Headquarters")
        String name,

        @Schema(description = "The functional type of the site")
        SiteType type,

        @Schema(description = "The current operational status of the site")
        SiteStatus status,

        @Schema(description = "ISO 3166-1 alpha-2 country code", example = "AL")
        String countryCode,

        @Schema(description = "IANA timezone identifier", example = "Europe/Tirane")
        String timezone,

        @Schema(description = "The shape type used for geofencing")
        GeofenceShapeType geofenceShapeType,

        @Schema(description = "Whether GPS location is required for clocking in/out at this site")
        Boolean locationRequired,

        @Schema(description = "Whether QR code scanning is enabled for this site")
        Boolean qrEnabled,

        @Schema(description = "Whether check-in operations are permitted at this site")
        Boolean checkInEnabled,

        @Schema(description = "Whether check-out operations are permitted at this site")
        Boolean checkOutEnabled,

        @Schema(description = "Additional internal notes or descriptions")
        String notes
) {
}
