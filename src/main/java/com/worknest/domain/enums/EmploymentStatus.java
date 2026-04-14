package com.worknest.domain.enums;

/**
 * Status of an employee's employment within a company.
 */
public enum EmploymentStatus {
    /**
     * Account is created but the employee has not yet logged in or activated it.
     */
    PENDING,

    /**
     * Actively employed and working.
     */
    ACTIVE,
    
    /**
     * Temporarily inactive but still employed (e.g., maternity leave, sabbatical).
     */
    ON_LEAVE,
    
    /**
     * Employment has ended.
     */
    TERMINATED,
    
    /**
     * In probationary period.
     */
    PROBATION
}
