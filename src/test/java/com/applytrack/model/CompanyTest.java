package com.applytrack.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the Company model entity.
 */
public class CompanyTest {

    @Test
    @DisplayName("Verify company constructor and getters")
    public void testCompanyCreation() {
        Company company = new Company("Google", "https://careers.google.com", "Technology", "Mountain View, CA");
        assertEquals("Google", company.getCompanyName());
        assertEquals("https://careers.google.com", company.getWebsite());
        assertEquals("Technology", company.getIndustry());
        assertEquals("Mountain View, CA", company.getLocation());
        assertEquals("Google (Mountain View, CA)", company.getDisplayTitle());
    }

    @Test
    @DisplayName("Verify constructor overloading with minimal parameters")
    public void testCompanyMinimalConstructor() {
        Company company = new Company("Microsoft");
        assertEquals("Microsoft", company.getCompanyName());
        assertEquals(0, company.getCompanyId());
    }

    @Test
    @DisplayName("Verify validation: empty company name throws IllegalArgumentException")
    public void testEmptyCompanyName() {
        assertThrows(IllegalArgumentException.class, () -> new Company(""));
        assertThrows(IllegalArgumentException.class, () -> new Company(null));
        assertThrows(IllegalArgumentException.class, () -> new Company("   "));
    }

    @Test
    @DisplayName("Verify validation: invalid website URL throws IllegalArgumentException")
    public void testInvalidWebsite() {
        Company company = new Company("Valid Company");
        assertThrows(IllegalArgumentException.class, () -> company.setWebsite("invalid_website_without_protocol"));
    }

    @Test
    @DisplayName("Verify Searchable interface implementation on Company")
    public void testSearchableMatches() {
        Company company = new Company("Amazon", "https://amazon.jobs", "E-Commerce", "Seattle, WA");

        assertTrue(company.matches("amazon"));
        assertTrue(company.matches("AMAZON"));
        assertTrue(company.matches("commerce"));
        assertTrue(company.matches("seattle"));
        assertFalse(company.matches("Netflix"));
        assertTrue(company.matches("")); // Blank query matches all
    }
}

