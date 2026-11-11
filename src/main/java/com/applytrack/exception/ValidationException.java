package com.applytrack.exception;

/**
 * Exception thrown when user input validation fails during registration or entity creation.
 *
 * Demonstrates:
 * - Custom Exceptions & Inheritance: extends Exception (Unit 3)
 */
public class ValidationException extends Exception {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

