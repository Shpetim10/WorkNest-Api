package com.worknest.features.auth.exception;

/**
 * Exception thrown when a user attempts to log in or select a role for a platform (WEB/MOBILE) 
 * that they are not authorized to use based on their assigned role.
 */
public class NoPlatformAccessException extends AuthenticationFailedException {

    public NoPlatformAccessException() {
        super("NO_PLATFORM_ACCESS", "No active role assignment grants access for the requested platform");
    }

    public NoPlatformAccessException(String platform) {
        super("NO_PLATFORM_ACCESS", String.format("User is not authorized for %s access", platform));
    }
}
