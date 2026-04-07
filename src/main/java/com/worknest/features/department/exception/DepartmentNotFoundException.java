package com.worknest.features.department.exception;

import com.worknest.common.exception.ResourceNotFoundException;

public class DepartmentNotFoundException extends ResourceNotFoundException {
    public DepartmentNotFoundException() {
        super("DEPARTMENT_NOT_FOUND", "Department not found or access denied.");
    }
}
