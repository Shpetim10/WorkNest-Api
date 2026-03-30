package com.worknest.auth.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidInvitationException extends BusinessException {

    public InvalidInvitationException(String code, String message) {
        super(HttpStatus.BAD_REQUEST, code, message);
    }
}
