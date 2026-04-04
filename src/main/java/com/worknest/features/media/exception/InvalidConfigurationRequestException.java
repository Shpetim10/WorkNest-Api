package com.worknest.features.media.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidConfigurationRequestException extends BusinessException {

    public InvalidConfigurationRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_CONFIGURATION_REQUEST", message);
    }
}
