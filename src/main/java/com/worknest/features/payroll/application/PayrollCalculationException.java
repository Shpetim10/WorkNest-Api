package com.worknest.features.payroll.application;

import com.worknest.common.exception.BusinessException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PayrollCalculationException extends BusinessException {

    private final String code;

    public PayrollCalculationException(String code, String message) {
        super(HttpStatus.BAD_REQUEST, code, message);
        this.code = code;
    }
}
