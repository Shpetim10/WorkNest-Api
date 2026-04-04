package com.worknest.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A standard generic message response")
public record GenericMessageResponse(
        @Schema(description = "The message content", example = "Operation completed successfully")
        String message
) {
}
