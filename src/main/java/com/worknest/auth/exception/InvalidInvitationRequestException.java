package com.worknest.auth.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidInvitationRequestException extends BusinessException {

    public InvalidInvitationRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_INVITATION_REQUEST", message);
    }
}
