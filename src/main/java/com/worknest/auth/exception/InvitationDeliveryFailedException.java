package com.worknest.auth.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvitationDeliveryFailedException extends BusinessException {

    public InvitationDeliveryFailedException(String email) {
        super(
                HttpStatus.SERVICE_UNAVAILABLE,
                "INVITATION_DELIVERY_FAILED",
                "Failed to deliver invitation email to '" + email + "'");
    }
}
