package com.applytrack.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for ValidationUtil.
 */
public class ValidationUtilTest {

    @Test
    @DisplayName("Verify isNotEmpty")
    public void testIsNotEmpty() {
        assertTrue(ValidationUtil.isNotEmpty("Hello"));
        assertFalse(ValidationUtil.isNotEmpty(""));
        assertFalse(ValidationUtil.isNotEmpty("   "));
        assertFalse(ValidationUtil.isNotEmpty(null));
    }

    @Test
    @DisplayName("Verify isValidEmail")
    public void testIsValidEmail() {
        assertTrue(ValidationUtil.isValidEmail("student@university.edu"));
        assertTrue(ValidationUtil.isValidEmail("alex.mercer@applytrack.io"));
        assertFalse(ValidationUtil.isValidEmail("plainaddress"));
        assertFalse(ValidationUtil.isValidEmail("missing.at.domain"));
        assertFalse(ValidationUtil.isValidEmail("@missingusername.com"));
        assertFalse(ValidationUtil.isValidEmail("missingdomain@"));
        assertFalse(ValidationUtil.isValidEmail("two@@at.com"));
        assertFalse(ValidationUtil.isValidEmail("space in@domain.com"));
        assertFalse(ValidationUtil.isValidEmail(null));
    }

    @Test
    @DisplayName("Verify isValidPhone")
    public void testIsValidPhone() {
        assertTrue(ValidationUtil.isValidPhone("+1-650-555-0199"));
        assertTrue(ValidationUtil.isValidPhone("+91-98765-43210"));
        assertTrue(ValidationUtil.isValidPhone("9876543210"));
        assertFalse(ValidationUtil.isValidPhone("123")); // Too short
        assertFalse(ValidationUtil.isValidPhone("phone_with_letters"));
        assertFalse(ValidationUtil.isValidPhone(null));
    }

    @Test
    @DisplayName("Verify isValidUrl")
    public void testIsValidUrl() {
        assertTrue(ValidationUtil.isValidUrl("https://careers.google.com"));
        assertTrue(ValidationUtil.isValidUrl("http://amazon.jobs"));
        assertTrue(ValidationUtil.isValidUrl("www.atlassian.com"));
        assertFalse(ValidationUtil.isValidUrl("invalid_url_string"));
        assertFalse(ValidationUtil.isValidUrl(null));
    }

    @Test
    @DisplayName("Verify isNonNegativeSalary")
    public void testSalary() {
        assertTrue(ValidationUtil.isNonNegativeSalary(null)); // Optional
        assertTrue(ValidationUtil.isNonNegativeSalary(BigDecimal.ZERO));
        assertTrue(ValidationUtil.isNonNegativeSalary(new BigDecimal("120000.00")));
        assertFalse(ValidationUtil.isNonNegativeSalary(new BigDecimal("-500.00")));
    }

    @Test
    @DisplayName("Verify isValidDeadline")
    public void testDeadline() {
        LocalDate appDate = LocalDate.of(2026, 11, 10);
        assertTrue(ValidationUtil.isValidDeadline(null, appDate)); // Optional
        assertTrue(ValidationUtil.isValidDeadline(appDate, appDate)); // Same day
        assertTrue(ValidationUtil.isValidDeadline(appDate.plusDays(7), appDate)); // Future
        assertFalse(ValidationUtil.isValidDeadline(appDate.minusDays(1), appDate)); // Earlier
    }

    @Test
    @DisplayName("Verify sanitize")
    public void testSanitize() {
        assertEquals("Java Developer", ValidationUtil.sanitize("   Java    Developer   "));
        assertEquals("", ValidationUtil.sanitize(null));
    }
}

