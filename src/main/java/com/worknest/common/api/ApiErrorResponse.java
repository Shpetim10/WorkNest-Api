package com.worknest.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(description = "Standard error response for all WorkNest APIs")
public record ApiErrorResponse(
        @Schema(description = "Indicates if the request was successful (always false in this response)")
        boolean success,
        @Schema(description = "Unique error code for programmatic handling", example = "VALIDATION_ERROR")
        String code,
        @Schema(description = "Human-readable error message", example = "Request validation failed")
        String message,
        @Schema(description = "The path where the error occurred", example = "/api/v1/auth/login")
        String path,
        @Schema(description = "Timestamp of the error")
        Instant timestamp,
        @Schema(description = "List of specific field validation errors, if applicable")
        List<FieldValidationError> fieldErrors
) {

    public static ApiErrorResponse of(String code, String message, String path, List<FieldValidationError> fieldErrors) {
        return new ApiErrorResponse(false, code, message, path, Instant.now(), fieldErrors);
    }
}
