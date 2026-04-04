package com.worknest.features.invitation.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvitationTokenExpiredException extends BusinessException {

    public InvitationTokenExpiredException() {
        super(HttpStatus.BAD_REQUEST, "INVITATION_TOKEN_EXPIRED", "Invitation token has expired");
    }
}
