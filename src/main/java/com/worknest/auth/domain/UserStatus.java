package com.worknest.auth.domain;

/**
 * Operational status of a user account for authentication and access decisions.
 */
public enum UserStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    DEACTIVATED
}
