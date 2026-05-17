package com.worknest.features.company.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Request payload for changing the company currency and converting all monetary values.")
public record CurrencyExchangeRequest(

        @NotBlank
        @Size(max = 10)
        @Schema(description = "ISO 4217 currency code to switch to", example = "EUR")
        String newCurrency,

        @NotNull
        @DecimalMin(value = "0.0001", message = "Exchange rate must be greater than zero")
        @Schema(description = "Rate applied to every stored monetary value: newAmount = oldAmount × exchangeRate", example = "0.0099")
        BigDecimal exchangeRate
) {}