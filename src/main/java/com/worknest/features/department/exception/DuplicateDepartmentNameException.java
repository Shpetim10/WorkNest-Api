package com.worknest.features.department.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class DuplicateDepartmentNameException extends BusinessException {
    public DuplicateDepartmentNameException() {
        super(HttpStatus.CONFLICT, "DUPLICATE_DEPARTMENT_NAME", "A department with this name already exists in the company.");
    }
}
