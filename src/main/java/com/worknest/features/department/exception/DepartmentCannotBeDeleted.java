package com.worknest.features.department.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class DepartmentCannotBeDeleted extends BusinessException {
    public DepartmentCannotBeDeleted(String message) {
        super(HttpStatus.BAD_REQUEST,"CANNOT_BE_DELETED",message);
    }
}
