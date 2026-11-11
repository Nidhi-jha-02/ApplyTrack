package com.applytrack.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the User model entity.
 *
 * Demonstrates:
 * - Unit 3: Encapsulation, Constructor Overloading, Exception Handling
 * - Security: Verifying password protection in toString()
 */
public class UserTest {

    @Test
    @DisplayName("Verify user constructor and getters")
    public void testUserCreation() {
        User user = new User("Alex Mercer", "alex@applytrack.io", "$2a$10$hashedPassword123");
        assertEquals("Alex Mercer", user.getFullName());
        assertEquals("alex@applytrack.io", user.getEmail());
        assertEquals("$2a$10$hashedPassword123", user.getPasswordHash());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
        assertEquals("Alex Mercer (alex@applytrack.io)", user.getDisplayTitle());
    }

    @Test
    @DisplayName("Verify constructor overloading with existing ID and timestamps")
    public void testUserFullConstructor() {
        LocalDateTime created = LocalDateTime.of(2026, 1, 1, 12, 0);
        LocalDateTime updated = LocalDateTime.of(2026, 1, 5, 12, 0);
        User user = new User(42, "Jane Doe", "jane.doe@example.com", "hash456", created, updated);

        assertEquals(42, user.getUserId());
        assertEquals(42, user.getId());
        assertEquals("Jane Doe", user.getFullName());
        assertEquals("jane.doe@example.com", user.getEmail());
        assertEquals(created, user.getCreatedAt());
        assertEquals(updated, user.getUpdatedAt());
    }

    @Test
    @DisplayName("Verify validation: empty name throws IllegalArgumentException")
    public void testEmptyNameValidation() {
        assertThrows(IllegalArgumentException.class, () -> new User("", "alex@example.com", "hash123"));
        assertThrows(IllegalArgumentException.class, () -> new User("   ", "alex@example.com", "hash123"));
        assertThrows(IllegalArgumentException.class, () -> new User(null, "alex@example.com", "hash123"));
    }

    @Test
    @DisplayName("Verify validation: invalid email format throws IllegalArgumentException")
    public void testInvalidEmailValidation() {
        assertThrows(IllegalArgumentException.class, () -> new User("Alex Mercer", "plainaddress", "hash123"));
        assertThrows(IllegalArgumentException.class, () -> new User("Alex Mercer", "missing@dot", "hash123"));
        assertThrows(IllegalArgumentException.class, () -> new User("Alex Mercer", "@missingusername.com", "hash123"));
        assertThrows(IllegalArgumentException.class, () -> new User("Alex Mercer", "two@@signs.com", "hash123"));
    }

    @Test
    @DisplayName("Verify validation: empty password hash throws IllegalArgumentException")
    public void testEmptyPasswordHashValidation() {
        assertThrows(IllegalArgumentException.class, () -> new User("Alex Mercer", "alex@example.com", ""));
        assertThrows(IllegalArgumentException.class, () -> new User("Alex Mercer", "alex@example.com", null));
    }

    @Test
    @DisplayName("Verify security: toString() must NEVER expose the actual password hash")
    public void testPasswordHashHiddenInToString() {
        String secretHash = "$2a$10$verySecretBCryptHashStringThatMustNotBeLeaked";
        User user = new User("Alex Mercer", "alex@example.com", secretHash);

        String output = user.toString();
        assertFalse(output.contains(secretHash), "toString() must NEVER reveal raw password hash");
        assertTrue(output.contains("[PROTECTED]"), "toString() should output [PROTECTED]");
    }
}

