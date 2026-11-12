package com.applytrack.repository;

import com.applytrack.database.DatabaseConnection;
import com.applytrack.model.Interview;
import com.applytrack.model.enums.InterviewMode;
import com.applytrack.model.enums.InterviewOutcome;
import com.applytrack.repository.impl.JdbcInterviewRepository;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 7: JdbcInterviewRepository Live MariaDB Integration Tests")
class JdbcInterviewRepositoryLiveTest {

    private static JdbcInterviewRepository repo;

    private static final int DEMO_USER_ID = 1; // Alex Mercer
    private static final int STRANGER_USER_ID = 99999;

    @BeforeAll
    static void setUp() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            Assumptions.assumeTrue(conn != null && !conn.isClosed(), "MariaDB connection must be active");
            repo = new JdbcInterviewRepository();
        } catch (SQLException e) {
            Assumptions.abort("MariaDB not available: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("1. findByUserId retrieves all existing seed interviews for demo user")
    void testFindByUserId() throws SQLException {
        List<Interview> interviews = repo.findByUserId(DEMO_USER_ID);
        assertNotNull(interviews);
        assertTrue(interviews.size() >= 5, "Demo user should have at least 5 seed interviews (got: " + interviews.size() + ")");

        // Multi-tenant check: stranger should have 0 interviews
        List<Interview> strangerInterviews = repo.findByUserId(STRANGER_USER_ID);
        assertNotNull(strangerInterviews);
        assertTrue(strangerInterviews.isEmpty());
    }

    @Test
    @DisplayName("2. findById retrieves interview with populated Application and Company")
    void testFindById() throws SQLException {
        Optional<Interview> interviewOpt = repo.findById(1, DEMO_USER_ID);
        assertTrue(interviewOpt.isPresent(), "Interview #1 should exist for demo user");

        Interview interview = interviewOpt.get();
        assertEquals(1, interview.getId());
        assertEquals("Online Assessment", interview.getRoundName());
        assertEquals(InterviewOutcome.PASSED, interview.getOutcome());

        // Verify Application and Company relationships are populated
        assertNotNull(interview.getApplication(), "Parent Application must be populated");
        assertNotNull(interview.getApplication().getCompany(), "Company must be populated");
        assertEquals("Google", interview.getCompanyName());
    }

    @Test
    @DisplayName("3. findByApplicationId retrieves multiple rounds for the same job application")
    void testFindByApplicationIdMultiRound() throws SQLException {
        // Application 1 (Google) has 2 rounds: OA & Technical Round 1
        List<Interview> googleRounds = repo.findByApplicationId(1, DEMO_USER_ID);
        assertNotNull(googleRounds);
        assertTrue(googleRounds.size() >= 2, "Google application should have at least 2 interview rounds");

        // Application 3 (Amazon) has 3 rounds: OA, Tech 1, and Bar Raiser
        List<Interview> amazonRounds = repo.findByApplicationId(3, DEMO_USER_ID);
        assertNotNull(amazonRounds);
        assertTrue(amazonRounds.size() >= 3, "Amazon application should have at least 3 interview rounds");

        // Chronological order verification
        for (int i = 0; i < amazonRounds.size() - 1; i++) {
            LocalDateTime d1 = amazonRounds.get(i).getInterviewDate();
            LocalDateTime d2 = amazonRounds.get(i + 1).getInterviewDate();
            assertTrue(d1.isBefore(d2) || d1.isEqual(d2), "Rounds must be ordered chronologically ASC");
        }
    }

    @Test
    @DisplayName("4. save() persists a new interview and generates a primary key")
    void testSavePersistsAndGeneratesKey() throws SQLException {
        Interview interview = new Interview(
                0,
                1, // Application 1 (Google)
                "System Architecture Round (Live Test)",
                LocalDateTime.of(2026, 12, 1, 15, 30),
                InterviewMode.ONLINE,
                "Virtual",
                "https://meet.google.com/test-live-repo",
                "Distinguished Engineer",
                "Scalability and caching focus",
                InterviewOutcome.SCHEDULED,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        Interview saved = repo.save(interview);
        assertNotNull(saved);
        assertTrue(saved.getId() > 0, "Generated ID should be positive");

        try {
            Optional<Interview> fetched = repo.findById(saved.getId(), DEMO_USER_ID);
            assertTrue(fetched.isPresent());
            assertEquals("System Architecture Round (Live Test)", fetched.get().getRoundName());
            assertEquals("Google", fetched.get().getCompanyName());
        } finally {
            // Clean up test row
            repo.delete(saved.getId(), DEMO_USER_ID);
        }
    }

    @Test
    @DisplayName("5. update() modifies interview details and outcome in MariaDB")
    void testUpdateInterview() throws SQLException {
        Interview interview = new Interview(
                0, 1, "Temporary Round for Update",
                LocalDateTime.of(2026, 12, 5, 10, 0),
                InterviewMode.ONLINE, "Remote", "https://meet.google.com/temp",
                "Interviewer A", "Pending", InterviewOutcome.SCHEDULED,
                LocalDateTime.now(), LocalDateTime.now()
        );
        Interview saved = repo.save(interview);

        try {
            saved.setRoundName("Updated System Round");
            saved.setOutcome(InterviewOutcome.PASSED);
            saved.setFeedback("Candidate demonstrated strong problem-solving skills.");

            boolean updated = repo.update(saved, DEMO_USER_ID);
            assertTrue(updated, "Update query must return true");

            Optional<Interview> verify = repo.findById(saved.getId(), DEMO_USER_ID);
            assertTrue(verify.isPresent());
            assertEquals("Updated System Round", verify.get().getRoundName());
            assertEquals(InterviewOutcome.PASSED, verify.get().getOutcome());
            assertEquals("Candidate demonstrated strong problem-solving skills.", verify.get().getFeedback());
        } finally {
            repo.delete(saved.getId(), DEMO_USER_ID);
        }
    }

    @Test
    @DisplayName("6. delete() removes interview record from database")
    void testDeleteInterview() throws SQLException {
        Interview interview = new Interview(
                0, 1, "Round to Delete",
                LocalDateTime.of(2026, 12, 10, 11, 0),
                InterviewMode.ONLINE, null, null, null, null,
                InterviewOutcome.SCHEDULED, LocalDateTime.now(), LocalDateTime.now()
        );
        Interview saved = repo.save(interview);
        int id = saved.getId();

        boolean deleted = repo.delete(id, DEMO_USER_ID);
        assertTrue(deleted, "Delete should succeed");

        Optional<Interview> check = repo.findById(id, DEMO_USER_ID);
        assertTrue(check.isEmpty(), "Deleted interview should no longer exist in MariaDB");
    }

    @Test
    @DisplayName("7. Multi-tenant security: stranger user cannot read, update, or delete demo user's interviews")
    void testMultiTenantSecurity() throws SQLException {
        // Stranger cannot read demo user's interview
        Optional<Interview> crossRead = repo.findById(1, STRANGER_USER_ID);
        assertTrue(crossRead.isEmpty(), "Stranger should not find user 1's interview");

        // Stranger cannot update demo user's interview
        Optional<Interview> user1Interview = repo.findById(1, DEMO_USER_ID);
        assertTrue(user1Interview.isPresent());
        Interview toHack = user1Interview.get();
        toHack.setRoundName("Hacked Round");

        boolean updateResult = repo.update(toHack, STRANGER_USER_ID);
        assertFalse(updateResult, "Stranger should not be able to update user 1's interview");

        // Verify original data remains untouched
        Optional<Interview> afterHackAttempt = repo.findById(1, DEMO_USER_ID);
        assertTrue(afterHackAttempt.isPresent());
        assertEquals("Online Assessment", afterHackAttempt.get().getRoundName());

        // Stranger cannot delete demo user's interview
        boolean deleteResult = repo.delete(1, STRANGER_USER_ID);
        assertFalse(deleteResult, "Stranger should not be able to delete user 1's interview");
    }

    @Test
    @DisplayName("8. searchByKeyword() finds interviews matching company, role, or round")
    void testSearchByKeyword() throws SQLException {
        List<Interview> googleMatches = repo.searchByKeyword(DEMO_USER_ID, "Google");
        assertFalse(googleMatches.isEmpty(), "Search for 'Google' should find matches");

        List<Interview> dsaMatches = repo.searchByKeyword(DEMO_USER_ID, "DSA");
        assertFalse(dsaMatches.isEmpty(), "Search for 'DSA' should find matches");

        List<Interview> noMatches = repo.searchByKeyword(DEMO_USER_ID, "ZzzNonExistentKeyword");
        assertTrue(noMatches.isEmpty(), "Non-existent keyword should return empty list");
    }

    @Test
    @DisplayName("9. filterByOutcome() correctly filters interviews by outcome")
    void testFilterByOutcome() throws SQLException {
        List<Interview> passed = repo.filterByOutcome(DEMO_USER_ID, InterviewOutcome.PASSED);
        assertFalse(passed.isEmpty(), "Should have passed interviews in sample data");
        for (Interview i : passed) {
            assertEquals(InterviewOutcome.PASSED, i.getOutcome());
        }

        List<Interview> scheduled = repo.filterByOutcome(DEMO_USER_ID, InterviewOutcome.SCHEDULED);
        assertFalse(scheduled.isEmpty(), "Should have scheduled interviews in sample data");
        for (Interview i : scheduled) {
            assertEquals(InterviewOutcome.SCHEDULED, i.getOutcome());
        }
    }

    @Test
    @DisplayName("10. findUpcoming() and findCompleted() distinguish timeline accurately")
    void testFindUpcomingAndCompleted() throws SQLException {
        List<Interview> upcoming = repo.findUpcoming(DEMO_USER_ID);
        assertNotNull(upcoming);

        List<Interview> completed = repo.findCompleted(DEMO_USER_ID);
        assertNotNull(completed);
        assertFalse(completed.isEmpty(), "Sample data has multiple completed interviews");
    }

    @Test
    @DisplayName("11. countInterviewsByUserId and countUpcomingByUserId return valid metrics")
    void testCountMethods() throws SQLException {
        int total = repo.countInterviewsByUserId(DEMO_USER_ID);
        assertTrue(total >= 5, "Demo user total interviews should be >= 5");

        int upcoming = repo.countUpcomingByUserId(DEMO_USER_ID);
        assertTrue(upcoming >= 0, "Upcoming count should be >= 0");

        int strangerCount = repo.countInterviewsByUserId(STRANGER_USER_ID);
        assertEquals(0, strangerCount, "Stranger should have 0 total interviews");

        int completed = repo.countCompletedByUserId(DEMO_USER_ID);
        assertTrue(completed >= 0, "Completed count should be >= 0");

        int passed = repo.countPassedByUserId(DEMO_USER_ID);
        assertTrue(passed >= 0, "Passed count should be >= 0");
    }

    @Test
    @DisplayName("12. findWithFilters() with UI defaults 'All Outcomes' and 'All Interviews' returns all records without 0-result bug")
    void testFindWithFiltersUiDefaults() throws SQLException {
        // This directly verifies the fix for the UI bug where "All Outcomes" returned 0 records
        List<Interview> all = repo.findWithFilters(DEMO_USER_ID, null, "All Outcomes", "All Interviews", "date", true);
        assertNotNull(all);
        int totalExpected = repo.countInterviewsByUserId(DEMO_USER_ID);
        assertEquals(totalExpected, all.size(), "findWithFilters with 'All Outcomes' must return all " + totalExpected + " interviews");

        // Verify with case variations and blank strings
        List<Interview> caseInsensitive = repo.findWithFilters(DEMO_USER_ID, "", "ALL", "ALL", "date", false);
        assertEquals(totalExpected, caseInsensitive.size());
    }

    @Test
    @DisplayName("13. findWithFilters() with timeline and outcome UI strings works reliably")
    void testFindWithFiltersTimelineAndOutcome() throws SQLException {
        // UI uses "Upcoming Interviews" and "Completed / Past"
        List<Interview> upcoming = repo.findWithFilters(DEMO_USER_ID, null, "All Outcomes", "Upcoming Interviews", "date", true);
        assertNotNull(upcoming);

        List<Interview> completed = repo.findWithFilters(DEMO_USER_ID, null, "All Outcomes", "Completed / Past", "date", true);
        assertNotNull(completed);
        assertFalse(completed.isEmpty(), "Demo user should have completed interviews");

        // UI outcome selection "Passed"
        List<Interview> passed = repo.findWithFilters(DEMO_USER_ID, null, "Passed", "All Interviews", "date", true);
        assertNotNull(passed);
        for (Interview i : passed) {
            assertEquals(InterviewOutcome.PASSED, i.getOutcome());
        }

        // Search by location and feedback keywords
        List<Interview> feedbackSearch = repo.findWithFilters(DEMO_USER_ID, "Dijkstra", "All Outcomes", "All Interviews", "date", true);
        assertFalse(feedbackSearch.isEmpty(), "Search for 'Dijkstra' in feedback must return matches");
    }
}

