package com.worknest.auth.domain;

/**
 * Operational status of a user account for authentication and access decisions.
 */
public enum UserStatus {
    /**
     * Initialized but not yet verified or activated.
     */
    PENDING,

    /**
     * Fully authenticated and operational.
     */
    ACTIVE,

    /**
     * Locked by an administrator or system policy.
     */
    SUSPENDED,

    /**
     * User-terminated or formally closed account.
     */
    DEACTIVATED
}
