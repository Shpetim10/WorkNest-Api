package com.worknest.auth.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvitationTokenInvalidException extends BusinessException {

    public InvitationTokenInvalidException() {
        super(HttpStatus.BAD_REQUEST, "INVITATION_TOKEN_INVALID", "Invitation token is invalid");
    }
}
