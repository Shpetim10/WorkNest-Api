package com.worknest.auth.domain;

/**
 * Lifecycle state of a company account in the platform.
 */
public enum CompanyStatus {
    /**
     * Fully operational company account.
     */
    ACTIVE,

    /**
     * Temporarily locked due to billing or policy violations.
     */
    SUSPENDED,

    /**
     * Logically removed or terminated account.
     */
    DELETED
}
