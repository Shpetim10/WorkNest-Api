package com.worknest.features.company.exception;

import com.worknest.common.exception.BusinessException;
import com.worknest.features.company.dto.SiteSetupIssueResponse;
import java.util.List;
import org.springframework.http.HttpStatus;

public class CompanySiteActivationBlockedException extends BusinessException {

    private final List<SiteSetupIssueResponse> issues;

    public CompanySiteActivationBlockedException(List<SiteSetupIssueResponse> issues) {
        super(HttpStatus.CONFLICT, "SITE_ACTIVATION_BLOCKED", "Site activation is blocked until setup issues are resolved");
        this.issues = issues;
    }

    public List<SiteSetupIssueResponse> getIssues() {
        return issues;
    }
}
