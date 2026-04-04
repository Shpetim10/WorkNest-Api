package com.worknest.domain.enums;

/**
 * Distinguishes between different platform access points for sessions and role assignments.
 */
public enum PlatformAccess {
    /**
     * Access via mobile applications (iOS/Android).
     */
    MOBILE,

    /**
     * Access via web applications.
     */
    WEB,
    /**
     * Access via both.
     */
    BOTH;
}
