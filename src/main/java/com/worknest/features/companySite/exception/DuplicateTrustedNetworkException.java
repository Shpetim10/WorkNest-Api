package com.worknest.features.companySite.exception;

import com.worknest.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when two trusted-network rules in the same create-site request
 * carry the same {@code cidrBlock + networkType} combination, which would
 * violate the unique constraint {@code uk_site_trusted_networks_site_cidr_type}.
 *
 * <p>Maps to HTTP 409 Conflict.
 */
public class DuplicateTrustedNetworkException extends BusinessException {

    public DuplicateTrustedNetworkException(String cidrBlock) {
        super(
                HttpStatus.CONFLICT,
                "DUPLICATE_TRUSTED_NETWORK",
                "Duplicate trusted-network rule detected for CIDR '" + cidrBlock + "'."
        );
    }
}
