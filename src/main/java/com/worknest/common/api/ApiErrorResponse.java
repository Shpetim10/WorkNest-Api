package com.worknest.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
        boolean success,
        String code,
        String message,
        String path,
        Instant timestamp,
        List<FieldValidationError> fieldErrors
) {

    public static ApiErrorResponse of(String code, String message, String path, List<FieldValidationError> fieldErrors) {
        return new ApiErrorResponse(false, code, message, path, Instant.now(), fieldErrors);
    }
}
