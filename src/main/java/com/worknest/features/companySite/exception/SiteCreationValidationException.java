package com.worknest.features.companySite.exception;

import com.worknest.common.api.FieldValidationError;
import com.worknest.common.exception.BusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * Field-aware validation error for the site-creation flow.
 */
public class SiteCreationValidationException extends BusinessException {

    private final List<FieldValidationError> fieldErrors;

    public SiteCreationValidationException(List<FieldValidationError> fieldErrors) {
        super(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Some site details are missing or invalid. Fix the highlighted fields and try again."
        );
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<FieldValidationError> getFieldErrors() {
        return fieldErrors;
    }
}
