package com.applytrack.exception;

/**
 * Exception thrown when authentication fails (invalid credentials, unknown user, etc.).
 *
 * Demonstrates:
 * - Custom Exceptions & Inheritance: extends Exception (Unit 3)
 * - Constructor chaining with super() (Unit 3)
 */
public class AuthenticationException extends Exception {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}

