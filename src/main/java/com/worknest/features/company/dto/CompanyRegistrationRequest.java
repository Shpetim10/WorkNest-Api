package com.worknest.features.company.dto;

import com.worknest.domain.enums.SubscriptionPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * Request payload for registering a new company workspace.
 */
@Schema(description = "Request object for initiating a new company registration")
public record CompanyRegistrationRequest(

        // Company core

        @NotBlank
        @Size(max = 255)
        @Schema(description = "Official legal name of the organization", example = "Acme Corporation SH.P.K")
        String companyName,

        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[a-z0-9-]+$", message = "slug must contain only lowercase letters, numbers, and hyphens")
        @Schema(description = "Unique URL-friendly identifier for the workspace", example = "acme-corp")
        String slug,

        /** Tax/NIPT identifier */
        @Size(max = 30)
        @Schema(description = "Tax identification number (NIPT in Albania)", example = "L12345678Q")
        @NotBlank
        @NotNull
        String nipt,

        /** Primary contact e-mail for the company (billing, notifications). */
        @NotBlank
        @Email
        @Size(max = 255)
        @Schema(description = "Primary corporate contact e-mail", example = "info@acme.com")
        String primaryEmail,

        /** Primary contact phone number for the company. */
        @Size(max = 50)
        @Schema(description = "Primary corporate contact phone number", example = "+355 69 000 0000")
        String primaryPhone,

        /** ISO 3166-1 alpha-2 country code. Defaults to {@code AL} if omitted. */
        @Size(max = 2)
        @Schema(description = "ISO 3166-1 alpha-2 country code", example = "AL", defaultValue = "AL")
        String countryCode,

        /** IANA timezone identifier. Defaults to {@code Europe/Tirane} if omitted. */
        @Size(max = 100)
        @Schema(description = "IANA timezone identifier", example = "Europe/Tirane", defaultValue = "Europe/Tirane")
        String timezone,

        /** BCP-47 locale code. Defaults to {@code sq} if omitted. */
        @Size(max = 10)
        @Schema(description = "BCP-47 locale code", example = "sq-AL", defaultValue = "sq")
        String locale,

        /** ISO 4217 currency code. Defaults to {@code ALL} if omitted. */
        @Size(max = 10)
        @Schema(description = "ISO 4217 currency code", example = "ALL", defaultValue = "ALL")
        String currency,

        /** Display date format */
        @Size(max = 20)
        @Schema(description = "Preferred display date format", example = "DD/MM/YYYY", defaultValue = "DD/MM/YYYY")
        String dateFormat,

        /** Business sector/industry */
        @Size(max = 100)
        @Schema(description = "Official industry classification", example = "technology")
        String industry,

        // Initial administrator

        /** E-mail address of the initial ADMIN user. An invitation will be sent here. */
        @NotBlank
        @Email
        @Size(max = 255)
        @Schema(description = "E-mail of the first system administrator", example = "admin@acme.com")
        String adminEmail,

        @NotBlank
        @Size(max = 50)
        @Schema(description = "Legal first name of the administrator", example = "John")
        String adminFirstName,

        @NotBlank
        @Size(max = 50)
        @Schema(description = "Legal last name of the administrator", example = "Doe")
        String adminLastName,

        /** Admin's preferred contact phone number. */
        @Size(max = 50)
        @Schema(description = "Direct contact phone for the administrator", example = "+355 68 000 0000")
        String adminPhoneNumber,

        @Size(max = 10)
        @Schema(description = "User's preferred UI language", example = "en", defaultValue = "sq")
        String preferredLanguage,

        @Size(max = 500)
        @Schema(description = "Key of the pre-uploaded company logo", example = "public/registrations/logos/2026/04/uuid.png")
        String logoKey,

        @Size(max = 1000)
        @Schema(description = "Path/URL of the pre-uploaded company logo", example = "storage/media/public/registrations/logos/2026/04/uuid.png")
        String logoPath,

        @Schema(description = "Subscription plan chosen during signup; if provided together with paymentMethodId the trial is started immediately", example = "GROWTH")
        SubscriptionPlan plan,

        @Size(max = 255)
        @Schema(description = "Stripe PaymentMethod ID collected on the payment step; required when plan is set", example = "pm_card_visa")
        String paymentMethodId
) {
}
