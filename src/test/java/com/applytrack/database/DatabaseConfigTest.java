package com.applytrack.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests verifying configuration loading and security masking.
 * (Does not require an active database server).
 *
 * Demonstrates:
 * - JUnit 5 assertions & annotations - Unit 3
 * - Encapsulation & validation logic testing - Unit 3
 * - Security: Password protection in logging - Unit 1/3
 */
public class DatabaseConfigTest {

    @Test
    @DisplayName("Verify default configuration values are properly populated")
    public void testDefaultConfigValues() {
        DatabaseConfig config = new DatabaseConfig(null, null, null, null);
        assertNotNull(config.getUrl());
        assertTrue(config.getUrl().contains("applytrack_db"), "Default URL should point to applytrack_db");
        assertEquals("root", config.getUsername());
        assertEquals("com.mysql.cj.jdbc.Driver", config.getDriverClassName());
        assertTrue(config.isValid());
    }

    @Test
    @DisplayName("Verify custom configuration overrides defaults")
    public void testCustomConfigValues() {
        DatabaseConfig config = new DatabaseConfig(
                "jdbc:mysql://db.example.com:3306/custom_db",
                "custom_user",
                "secret_pass",
                "com.mysql.cj.jdbc.Driver"
        );
        assertEquals("jdbc:mysql://db.example.com:3306/custom_db", config.getUrl());
        assertEquals("custom_user", config.getUsername());
        assertEquals("secret_pass", config.getPassword());
        assertTrue(config.isValid());
    }

    @Test
    @DisplayName("Verify password is masked in getMaskedPassword and toString")
    public void testPasswordMasking() {
        DatabaseConfig config = new DatabaseConfig(
                "jdbc:mysql://localhost:3306/applytrack_db",
                "root",
                "super_secret_password_123",
                "com.mysql.cj.jdbc.Driver"
        );

        String masked = config.getMaskedPassword();
        assertFalse(masked.contains("super_secret_password_123"), "Masked password must not contain raw password");
        assertTrue(masked.contains("******"), "Masked password should contain asterisks");

        String toStringOutput = config.toString();
        assertFalse(toStringOutput.contains("super_secret_password_123"), "toString() must never leak cleartext passwords");
        assertTrue(toStringOutput.contains("******"), "toString() should output masked indicator");
    }

    @Test
    @DisplayName("Verify isValid handles empty and whitespace strings")
    public void testConfigValidation() {
        DatabaseConfig valid = new DatabaseConfig("jdbc:mysql://localhost:3306/db", "root", "", "com.mysql.cj.jdbc.Driver");
        assertTrue(valid.isValid());

        DatabaseConfig invalidUrl = new DatabaseConfig("   ", "root", "pass", "com.mysql.cj.jdbc.Driver");
        // Constructor sets fallback if null/empty, so URL becomes DEFAULT_URL
        assertTrue(invalidUrl.isValid());
    }
}

