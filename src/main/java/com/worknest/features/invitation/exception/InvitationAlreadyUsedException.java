package com.worknest.features.invitation.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvitationAlreadyUsedException extends BusinessException {

    public InvitationAlreadyUsedException() {
        super(HttpStatus.BAD_REQUEST, "INVITATION_ALREADY_USED", "Invitation has already been used");
    }
}
