package com.applytrack.model;

import com.applytrack.model.enums.ApplicationStatus;
import com.applytrack.model.enums.JobType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the Application model entity.
 */
public class ApplicationTest {

    @Test
    @DisplayName("Verify application constructor with default values")
    public void testApplicationDefaultValues() {
        LocalDate appDate = LocalDate.of(2026, 10, 15);
        Application app = new Application(1, 1, "Backend Engineer", appDate);

        assertEquals(1, app.getUserId());
        assertEquals(1, app.getCompanyId());
        assertEquals("Backend Engineer", app.getJobRole());
        assertEquals(appDate, app.getApplicationDate());
        assertEquals(JobType.FULL_TIME, app.getJobType());
        assertEquals(ApplicationStatus.APPLIED, app.getStatus());
        assertNotNull(app.getInterviews());
        assertTrue(app.getInterviews().isEmpty());
    }

    @Test
    @DisplayName("Verify validation: deadline earlier than application date throws exception")
    public void testDeadlineBeforeApplicationDate() {
        LocalDate appDate = LocalDate.of(2026, 11, 10);
        LocalDate invalidDeadline = LocalDate.of(2026, 11, 5); // Earlier

        Application app = new Application(1, 1, "SDE", appDate);
        assertThrows(IllegalArgumentException.class, () -> app.setDeadline(invalidDeadline));
    }

    @Test
    @DisplayName("Verify validation: negative salary throws exception")
    public void testNegativeSalary() {
        Application app = new Application(1, 1, "SDE", LocalDate.now());
        assertThrows(IllegalArgumentException.class, () -> app.setExpectedSalary(new BigDecimal("-1000")));
    }

    @Test
    @DisplayName("Verify 1:N interview relationship management")
    public void testInterviewRelationship() {
        Application app = new Application(1, 1, "SDE", LocalDate.now());
        app.setId(100);

        Interview round1 = new Interview(100, "Online Assessment", LocalDate.now().atTime(14, 0));
        Interview round2 = new Interview(100, "Technical Round 1", LocalDate.now().plusDays(2).atTime(16, 0));

        app.addInterview(round1);
        app.addInterview(round2);

        assertEquals(2, app.getInterviews().size());
        assertEquals(100, round1.getApplicationId());

        // Test defensive unmodifiable list: direct modification of returned list throws UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> app.getInterviews().add(
                new Interview(100, "Unauthorized", LocalDate.now().atTime(10, 0))
        ));

        app.removeInterview(round1);
        assertEquals(1, app.getInterviews().size());
    }

    @Test
    @DisplayName("Verify Searchable interface implementation on Application")
    public void testSearchableMatches() {
        Application app = new Application(1, 1, "Java Software Engineer", LocalDate.now());
        app.setStatus(ApplicationStatus.INTERVIEW_SCHEDULED);
        app.setJobType(JobType.INTERNSHIP);
        app.setNotes("Referral by alumni");

        Company company = new Company("Google");
        app.setCompany(company);

        assertTrue(app.matches("software"));
        assertTrue(app.matches("JAVA"));
        assertTrue(app.matches("interview"));
        assertTrue(app.matches("internship"));
        assertTrue(app.matches("alumni"));
        assertTrue(app.matches("google")); // Matches through company
        assertFalse(app.matches("DevOps"));
    }
}

