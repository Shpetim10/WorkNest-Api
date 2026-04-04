package com.worknest.auth.domain;

/**
 * Lifecycle state of a company account in the platform.
 */
public enum CompanyStatus {

    /**
     * Company workspace has been created but the initial admin has not yet
     * completed their invitation activation. No users can log in.
     */
    PENDING,

    /**
     * Fully operational company account. Set automatically when the initial
     * admin completes their invitation activation.
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
