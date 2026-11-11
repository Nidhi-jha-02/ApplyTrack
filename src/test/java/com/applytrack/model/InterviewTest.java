package com.applytrack.model;

import com.applytrack.model.enums.InterviewMode;
import com.applytrack.model.enums.InterviewOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the Interview model entity.
 */
public class InterviewTest {

    @Test
    @DisplayName("Verify interview creation and default values")
    public void testInterviewCreation() {
        LocalDateTime dt = LocalDateTime.of(2026, 11, 15, 14, 30);
        Interview interview = new Interview(1, "Technical Round 1", dt);

        assertEquals(1, interview.getApplicationId());
        assertEquals("Technical Round 1", interview.getRoundName());
        assertEquals(dt, interview.getInterviewDate());
        assertEquals(InterviewMode.ONLINE, interview.getInterviewMode());
        assertEquals(InterviewOutcome.SCHEDULED, interview.getOutcome());
        assertEquals("Technical Round 1 [Online] - Scheduled", interview.getDisplayTitle());
    }

    @Test
    @DisplayName("Verify validation: empty round name throws exception")
    public void testEmptyRoundName() {
        LocalDateTime dt = LocalDateTime.now();
        assertThrows(IllegalArgumentException.class, () -> new Interview(1, "", dt));
        assertThrows(IllegalArgumentException.class, () -> new Interview(1, "   ", dt));
        assertThrows(IllegalArgumentException.class, () -> new Interview(1, null, dt));
    }

    @Test
    @DisplayName("Verify validation: null date throws exception")
    public void testNullDate() {
        assertThrows(IllegalArgumentException.class, () -> new Interview(1, "Technical Round", null));
    }

    @Test
    @DisplayName("Verify validation: invalid meeting URL throws exception")
    public void testInvalidMeetingUrl() {
        Interview interview = new Interview(1, "Technical Round", LocalDateTime.now());
        assertThrows(IllegalArgumentException.class, () -> interview.setMeetingLink("not_a_valid_url"));
    }

    @Test
    @DisplayName("Verify Searchable interface implementation on Interview")
    public void testSearchableMatches() {
        Interview interview = new Interview(1, "System Design & Concurrency", LocalDateTime.now());
        interview.setInterviewerName("David Chen");
        interview.setFeedback("Good grasp on thread pools");
        interview.setOutcome(InterviewOutcome.PASSED);

        assertTrue(interview.matches("system"));
        assertTrue(interview.matches("DAVID"));
        assertTrue(interview.matches("thread"));
        assertTrue(interview.matches("passed"));
        assertFalse(interview.matches("Python"));
    }

    @Test
    @DisplayName("Verify convenience getters and application-aware search")
    public void testConvenienceGettersAndAppSearch() {
        LocalDateTime dt = LocalDateTime.of(2026, 11, 20, 14, 30);
        Interview interview = new Interview(10, "Technical Round 2", dt);

        // Without application attached
        assertEquals("Unknown Company", interview.getCompanyName());
        assertEquals("Unknown Role", interview.getJobRole());
        assertEquals("Nov 20, 2026", interview.getFormattedDate());
        assertEquals("02:30 PM", interview.getFormattedTime());
        assertEquals("Nov 20, 2026 • 02:30 PM", interview.getFormattedDateTime());

        // Attach application with company
        Application app = new Application(1, 100, "Backend Engineer", java.time.LocalDate.now());
        app.setId(10);
        Company comp = new Company("Stripe Technologies");
        comp.setId(100);
        app.setCompany(comp);
        interview.setApplication(app);

        assertEquals("Stripe Technologies", interview.getCompanyName());
        assertEquals("Backend Engineer", interview.getJobRole());

        // Application-aware search matches company and role
        assertTrue(interview.matches("Stripe"));
        assertTrue(interview.matches("backend"));
    }
}

