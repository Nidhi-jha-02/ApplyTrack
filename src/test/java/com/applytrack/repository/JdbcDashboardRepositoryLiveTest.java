package com.applytrack.repository;

import com.applytrack.database.DatabaseConnection;
import com.applytrack.model.dto.DeadlineSummaryItem;
import com.applytrack.model.dto.InterviewSummaryItem;
import com.applytrack.repository.impl.JdbcDashboardRepository;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 5: JdbcDashboardRepository Live MariaDB Integration Tests")
class JdbcDashboardRepositoryLiveTest {

    private static JdbcDashboardRepository repository;
    private static final int DEMO_USER_ID = 1; // Alex Mercer in sample_data.sql
    private static final int NON_EXISTENT_USER_ID = 99999;

    @BeforeAll
    static void setUp() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            Assumptions.assumeTrue(conn != null && !conn.isClosed(), "MariaDB connection must be available for live integration tests");
            repository = new JdbcDashboardRepository();
        } catch (SQLException e) {
            Assumptions.abort("MariaDB not reachable: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("countTotalApplications returns correct count for demo user and 0 for stranger")
    void testCountTotalApplications() throws SQLException {
        int totalDemo = repository.countTotalApplications(DEMO_USER_ID);
        assertTrue(totalDemo >= 6, "Demo user should have at least 6 seed applications (got: " + totalDemo + ")");

        int totalStranger = repository.countTotalApplications(NON_EXISTENT_USER_ID);
        assertEquals(0, totalStranger, "Stranger should have exactly 0 applications");
    }

    @Test
    @DisplayName("countInProgressApplications counts active review and interview states")
    void testCountInProgressApplications() throws SQLException {
        int inProgressDemo = repository.countInProgressApplications(DEMO_USER_ID);
        assertTrue(inProgressDemo >= 4, "Demo user should have at least 4 in-progress applications (got: " + inProgressDemo + ")");

        int inProgressStranger = repository.countInProgressApplications(NON_EXISTENT_USER_ID);
        assertEquals(0, inProgressStranger);
    }

    @Test
    @DisplayName("countScheduledInterviews counts upcoming active rounds")
    void testCountScheduledInterviews() throws SQLException {
        int interviewsDemo = repository.countScheduledInterviews(DEMO_USER_ID);
        assertTrue(interviewsDemo >= 1, "Demo user should have at least 1 scheduled interview (Google Tech Round 1)");

        int interviewsStranger = repository.countScheduledInterviews(NON_EXISTENT_USER_ID);
        assertEquals(0, interviewsStranger);
    }

    @Test
    @DisplayName("countOffers and countRejected return exact outcomes for demo user")
    void testOutcomesCount() throws SQLException {
        int offers = repository.countOffers(DEMO_USER_ID);
        assertTrue(offers >= 1, "Demo user should have at least 1 offer (Amazon SDE I Selected)");

        int rejected = repository.countRejected(DEMO_USER_ID);
        assertTrue(rejected >= 1, "Demo user should have at least 1 rejected application (Spotify)");

        assertEquals(0, repository.countOffers(NON_EXISTENT_USER_ID));
        assertEquals(0, repository.countRejected(NON_EXISTENT_USER_ID));
    }

    @Test
    @DisplayName("getStatusDistribution returns accurate grouped status counts")
    void testStatusDistribution() throws SQLException {
        Map<String, Integer> distribution = repository.getStatusDistribution(DEMO_USER_ID);
        assertNotNull(distribution);
        assertFalse(distribution.isEmpty());

        assertTrue(distribution.containsKey("Interview Scheduled") || distribution.containsKey("Selected"));

        Map<String, Integer> strangerDistribution = repository.getStatusDistribution(NON_EXISTENT_USER_ID);
        assertNotNull(strangerDistribution);
        assertTrue(strangerDistribution.isEmpty(), "Stranger distribution must be completely empty");
    }

    @Test
    @DisplayName("getMonthlyTrends groups application volume by Year-Month")
    void testMonthlyTrends() throws SQLException {
        Map<String, Integer> trends = repository.getMonthlyTrends(DEMO_USER_ID);
        assertNotNull(trends);
        assertFalse(trends.isEmpty());

        Map<String, Integer> strangerTrends = repository.getMonthlyTrends(NON_EXISTENT_USER_ID);
        assertNotNull(strangerTrends);
        assertTrue(strangerTrends.isEmpty());
    }

    @Test
    @DisplayName("getUpcomingDeadlines returns active cutoffs sorted ascending")
    void testUpcomingDeadlines() throws SQLException {
        List<DeadlineSummaryItem> deadlines = repository.getUpcomingDeadlines(DEMO_USER_ID, 10);
        assertNotNull(deadlines);

        // Verify sorted order (deadline asc)
        for (int i = 0; i < deadlines.size() - 1; i++) {
            DeadlineSummaryItem current = deadlines.get(i);
            DeadlineSummaryItem next = deadlines.get(i + 1);
            assertTrue(current.getDeadline().compareTo(next.getDeadline()) <= 0,
                    "Deadlines must be sorted ascending by deadline date");
        }

        List<DeadlineSummaryItem> strangerDeadlines = repository.getUpcomingDeadlines(NON_EXISTENT_USER_ID, 10);
        assertNotNull(strangerDeadlines);
        assertTrue(strangerDeadlines.isEmpty());
    }

    @Test
    @DisplayName("getUpcomingInterviews returns scheduled interview rounds")
    void testUpcomingInterviews() throws SQLException {
        List<InterviewSummaryItem> interviews = repository.getUpcomingInterviews(DEMO_USER_ID, 10);
        assertNotNull(interviews);
        assertFalse(interviews.isEmpty(), "Demo user should have scheduled interview preview");

        InterviewSummaryItem first = interviews.get(0);
        assertEquals("Google", first.getCompanyName());
        assertEquals("Technical Round 1", first.getRoundName());
        assertEquals("Scheduled", first.getOutcome());

        List<InterviewSummaryItem> strangerInterviews = repository.getUpcomingInterviews(NON_EXISTENT_USER_ID, 10);
        assertNotNull(strangerInterviews);
        assertTrue(strangerInterviews.isEmpty());
    }
}

