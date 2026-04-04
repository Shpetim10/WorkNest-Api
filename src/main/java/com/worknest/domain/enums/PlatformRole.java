package com.worknest.domain.enums;

/**
 * Platform-level roles used for global access control across WorkNest.
 */
public enum PlatformRole {
    /**
     * Platform administrator with full access to all companies.
     */
    SUPERADMIN,

    /**
     * Managed company owner or administrator.
     */
    ADMIN,

    /**
     * Company back-office or internal management staff.
     */
    STAFF,

    /**
     * Standard company employee, typically mobile-first.
     */
    EMPLOYEE
}
