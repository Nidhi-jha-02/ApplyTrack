package com.applytrack.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the Recruiter model entity.
 */
public class RecruiterTest {

    @Test
    @DisplayName("Verify recruiter creation and getters")
    public void testRecruiterCreation() {
        Recruiter recruiter = new Recruiter("Sarah Jenkins", 1, "sarah@example.com", "+1-650-555-0199",
                "https://linkedin.com/in/sarahjenkins", "University talent partner");

        assertEquals("Sarah Jenkins", recruiter.getRecruiterName());
        assertEquals(1, recruiter.getCompanyId());
        assertEquals("sarah@example.com", recruiter.getEmail());
        assertEquals("+1-650-555-0199", recruiter.getPhone());
        assertEquals("https://linkedin.com/in/sarahjenkins", recruiter.getLinkedinUrl());
    }

    @Test
    @DisplayName("Verify company association updates companyId")
    public void testCompanyAssociation() {
        Company company = new Company(10, "Spotify", "https://spotify.com", "Audio", "Stockholm", null, null);
        Recruiter recruiter = new Recruiter("David", "david@example.com");
        recruiter.setCompany(company);

        assertEquals(10, recruiter.getCompanyId());
        assertEquals("David @ Spotify", recruiter.getDisplayTitle());
    }

    @Test
    @DisplayName("Verify validation: empty recruiter name throws exception")
    public void testEmptyRecruiterName() {
        assertThrows(IllegalArgumentException.class, () -> new Recruiter("", "valid@example.com"));
        assertThrows(IllegalArgumentException.class, () -> new Recruiter("   ", "valid@example.com"));
        assertThrows(IllegalArgumentException.class, () -> new Recruiter(null, "valid@example.com"));
    }

    @Test
    @DisplayName("Verify Searchable interface implementation on Recruiter")
    public void testSearchableMatches() {
        Company company = new Company("Google");
        Recruiter recruiter = new Recruiter("Sarah Jenkins", "sarah@google.com");
        recruiter.setCompany(company);

        assertTrue(recruiter.matches("sarah"));
        assertTrue(recruiter.matches("SARAH"));
        assertTrue(recruiter.matches("jenkins"));
        assertTrue(recruiter.matches("google")); // Matches through associated company
        assertFalse(recruiter.matches("Microsoft"));
    }

    @Test
    @DisplayName("Verify convenience getters and associated applications count")
    public void testConvenienceGetters() {
        Recruiter recruiter = new Recruiter("Alex", "alex@example.com");
        assertEquals("Unaffiliated", recruiter.getCompanyName());
        assertEquals(0, recruiter.getAssociatedApplicationsCount());
        assertEquals("0 Applications", recruiter.getFormattedApplicationsCount());

        Company google = new Company("Google");
        recruiter.setCompany(google);
        assertEquals("Google", recruiter.getCompanyName());

        recruiter.setAssociatedApplicationsCount(1);
        assertEquals(1, recruiter.getAssociatedApplicationsCount());
        assertEquals("1 Application", recruiter.getFormattedApplicationsCount());

        recruiter.setAssociatedApplicationsCount(3);
        assertEquals("3 Applications", recruiter.getFormattedApplicationsCount());
    }
}

