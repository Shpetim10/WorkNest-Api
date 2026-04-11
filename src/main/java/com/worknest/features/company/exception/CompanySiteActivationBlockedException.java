package com.worknest.features.company.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import java.util.List;

/**
 * Thrown when a company site cannot be activated due to unresolved 
 * setup issues (e.g. missing location, missing trusted networks).
 */
public class CompanySiteActivationBlockedException extends BusinessException {

    private final List<ActivationIssue> issues;

    public CompanySiteActivationBlockedException(List<ActivationIssue> issues) {
        super(
            HttpStatus.UNPROCESSABLE_ENTITY, 
            "ACTIVATION_BLOCKED", 
            "The site cannot be activated until all setup requirements are met."
        );
        this.issues = issues;
    }

    public List<ActivationIssue> getIssues() {
        return issues;
    }

    /**
     * Represents a specific issue blocking activation.
     */
    public record ActivationIssue(String field, String message) {
    }
}
