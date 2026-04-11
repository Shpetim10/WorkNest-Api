package com.worknest.features.companySite.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a CIDR block is syntactically invalid, when the declared
 * {@link com.worknest.domain.enums.NetworkIpVersion} does not match the
 * address family of the CIDR, or when the prefix length is out of range.
 *
 * <p>Maps to HTTP 422 Unprocessable Entity.
 */
public class InvalidCidrException extends BusinessException {

    public InvalidCidrException(String cidr, String reason) {
        super(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "INVALID_CIDR",
                "Invalid CIDR block '" + cidr + "': " + reason
        );
    }
}
