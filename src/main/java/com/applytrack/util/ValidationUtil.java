package com.applytrack.util;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Reusable utility class for input sanitization and domain validation.
 *
 * Demonstrates:
 * - Static utility methods (Unit 2)
 * - String manipulation & inspection (trim, contains, indexOf, startsWith) - Unit 2
 * - Beginner-friendly validation without fragile complex regex (Unit 1 & 2)
 * - Primitive and Reference type comparisons (Unit 1)
 */
public class ValidationUtil {

    private ValidationUtil() {}

    /**
     * Checks if a string is non-null and contains non-whitespace characters.
     */
    public static boolean isNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }

    /**
     * Beginner-friendly email validator:
     * Checks for non-empty text, presence of '@', presence of '.' after '@', and no whitespace.
     */
    public static boolean isValidEmail(String email) {
        if (!isNotEmpty(email)) {
            return false;
        }
        String trimmed = email.trim();
        if (trimmed.contains(" ") || trimmed.startsWith("@") || trimmed.endsWith("@")) {
            return false;
        }
        int atIndex = trimmed.indexOf('@');
        int lastAtIndex = trimmed.lastIndexOf('@');
        if (atIndex == -1 || atIndex != lastAtIndex) {
            return false; // Must contain exactly one '@'
        }
        int dotIndex = trimmed.lastIndexOf('.');
        return dotIndex > atIndex + 1 && dotIndex < trimmed.length() - 1;
    }

    /**
     * Validates a phone number:
     * Allows digits, optional leading '+', hyphens, parentheses, and spaces; length between 7 and 20 chars.
     */
    public static boolean isValidPhone(String phone) {
        if (!isNotEmpty(phone)) {
            return false;
        }
        String trimmed = phone.trim();
        if (trimmed.length() < 7 || trimmed.length() > 20) {
            return false;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (!Character.isDigit(ch) && ch != '+' && ch != '-' && ch != '(' && ch != ')' && ch != ' ') {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates a website or career URL:
     * Must start with http:// or https:// or www.
     */
    public static boolean isValidUrl(String url) {
        if (!isNotEmpty(url)) {
            return false;
        }
        String lower = url.trim().toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("www.");
    }

    /**
     * Checks that salary/stipend is non-negative.
     */
    public static boolean isNonNegativeSalary(BigDecimal salary) {
        if (salary == null) {
            return true; // Salary is optional in applications
        }
        return salary.compareTo(BigDecimal.ZERO) >= 0;
    }

    /**
     * Validates that deadline is after or equal to the application date.
     */
    public static boolean isValidDeadline(LocalDate deadline, LocalDate applicationDate) {
        if (deadline == null) {
            return true; // Deadline is optional
        }
        if (applicationDate == null) {
            return true;
        }
        return !deadline.isBefore(applicationDate);
    }

    /**
     * Sanitizes input string by trimming and collapsing consecutive whitespace.
     */
    public static String sanitize(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().replaceAll("\\s+", " ");
    }
}

