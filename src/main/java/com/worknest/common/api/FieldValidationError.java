package com.worknest.common.api;

public record FieldValidationError(
        String field,
        String message
) {
}
