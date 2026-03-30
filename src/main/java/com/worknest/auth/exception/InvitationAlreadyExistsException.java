package com.worknest.auth.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvitationAlreadyExistsException extends BusinessException {

    public InvitationAlreadyExistsException(String email) {
        super(HttpStatus.CONFLICT, "INVITATION_ALREADY_EXISTS", "An active invitation already exists for '" + email + "'");
    }
}
