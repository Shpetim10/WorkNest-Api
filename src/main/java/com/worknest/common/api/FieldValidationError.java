package com.worknest.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Details about a specific field validation failure")
public record FieldValidationError(
        @Schema(description = "The name of the field that failed validation", example = "email")
        String field,
        @Schema(description = "Description of why the validation failed", example = "must be a well-formed email address")
        String message
) {
}
