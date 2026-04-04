package com.worknest.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
public record ApiResponse<T>(
        @Schema(description = "Indicates if the request was successful", example = "true")
        boolean success,
        
        @Schema(description = "Human-readable status or result message", example = "Operation completed successfully")
        String message,
        
        @Schema(description = "The actual response payload (generic)")
        T data,
        
        @Schema(description = "ISO-8601 timestamp of the response")
        Instant timestamp
) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now());
    }
}
