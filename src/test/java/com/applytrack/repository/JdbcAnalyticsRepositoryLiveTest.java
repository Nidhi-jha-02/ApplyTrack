package com.applytrack.repository;

import com.applytrack.database.DatabaseConnection;
import com.applytrack.model.dto.CompanyApplicationData;
import com.applytrack.model.dto.MonthlyApplicationData;
import com.applytrack.repository.impl.JdbcAnalyticsRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Live MariaDB Integration Tests for JdbcAnalyticsRepository.
 *
 * Demonstrates:
 * - Integration Testing against real database schema (Unit 3)
 * - Multi-tenant isolation testing (user_id scoping) (Unit 3)
 * - SQL Aggregate verification: COUNT, DISTINCT, GROUP BY, DATE_FORMAT (Unit 3)
 * - Date-range query filtering (Unit 1 & Unit 3)
 */
public class JdbcAnalyticsRepositoryLiveTest {

    private static final int DEMO_USER_ID = 1; // Alex Mercer demo user
    private static JdbcAnalyticsRepository repository;

    @BeforeAll
    static void checkDatabaseAvailability() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            assumeTrue(conn != null && !conn.isClosed(), "MariaDB database is available for live testing.");
            repository = new JdbcAnalyticsRepository();
        } catch (SQLException e) {
            assumeTrue(false, "Skipping live tests: MariaDB connection unavailable (" + e.getMessage() + ")");
        }
    }

    @Test
    @DisplayName("Live: countTotalApplications returns positive count for Demo User")
    void testCountTotalApplications() throws SQLException {
        int total = repository.countTotalApplications(DEMO_USER_ID, null);
        assertTrue(total >= 6, "Demo user should have at least 6 seed applications.");
    }

    @Test
    @DisplayName("Live: countActiveApplications counts only ongoing non-terminal statuses")
    void testCountActiveApplications() throws SQLException {
        int active = repository.countActiveApplications(DEMO_USER_ID, null);
        assertTrue(active >= 3, "Demo user should have active applications in progress.");
        int total = repository.countTotalApplications(DEMO_USER_ID, null);
        assertTrue(active <= total, "Active count must not exceed total applications.");
    }

    @Test
    @DisplayName("Live: countOffers counts applications with status Selected")
    void testCountOffers() throws SQLException {
        int offers = repository.countOffers(DEMO_USER_ID, null);
        assertTrue(offers >= 1, "Demo user should have at least 1 offer (Amazon: Selected).");
    }

    @Test
    @DisplayName("Live: countRejected counts applications with status Rejected")
    void testCountRejected() throws SQLException {
        int rejected = repository.countRejected(DEMO_USER_ID, null);
        assertTrue(rejected >= 1, "Demo user should have at least 1 rejected application (Spotify).");
    }

    @Test
    @DisplayName("Live: countTotalInterviews counts interview rounds joined through applications")
    void testCountTotalInterviews() throws SQLException {
        int interviews = repository.countTotalInterviews(DEMO_USER_ID, null);
        assertTrue(interviews >= 4, "Demo user should have at least 4 interview rounds.");
    }

    @Test
    @DisplayName("Live: countDistinctApplicationsInterviewed counts distinct applications with rounds")
    void testCountDistinctApplicationsInterviewed() throws SQLException {
        int distinct = repository.countDistinctApplicationsInterviewed(DEMO_USER_ID, null);
        int total = repository.countTotalApplications(DEMO_USER_ID, null);
        int totalInterviews = repository.countTotalInterviews(DEMO_USER_ID, null);

        assertTrue(distinct >= 2, "Demo user should have at least 2 distinct applications with interviews.");
        assertTrue(distinct <= total, "Distinct interviewed applications cannot exceed total applications.");
        assertTrue(distinct <= totalInterviews, "Distinct interviewed applications cannot exceed total interview rounds.");
    }

    @Test
    @DisplayName("Live: countUpcomingInterviews counts scheduled rounds in the future")
    void testCountUpcomingInterviews() throws SQLException {
        int upcoming = repository.countUpcomingInterviews(DEMO_USER_ID);
        assertTrue(upcoming >= 0, "Upcoming interview count should be non-negative.");
    }

    @Test
    @DisplayName("Live: getStatusCounts returns map grouped by status")
    void testGetStatusCounts() throws SQLException {
        Map<String, Integer> map = repository.getStatusCounts(DEMO_USER_ID, null);
        assertNotNull(map);
        assertFalse(map.isEmpty(), "Status distribution should contain data for demo user.");

        int sum = map.values().stream().mapToInt(Integer::intValue).sum();
        int total = repository.countTotalApplications(DEMO_USER_ID, null);
        assertEquals(total, sum, "Sum of status breakdown counts must equal total applications.");
    }

    @Test
    @DisplayName("Live: getMonthlyApplicationCounts groups by Year-Month in chronological order")
    void testGetMonthlyApplicationCounts() throws SQLException {
        List<MonthlyApplicationData> list = repository.getMonthlyApplicationCounts(DEMO_USER_ID, null);
        assertNotNull(list);
        assertFalse(list.isEmpty(), "Monthly application counts should not be empty.");

        // Check chronological ordering
        for (int i = 0; i < list.size() - 1; i++) {
            assertTrue(list.get(i).getYearMonth().compareTo(list.get(i + 1).getYearMonth()) <= 0,
                    "Monthly records must be in ascending chronological order.");
        }
    }

    @Test
    @DisplayName("Live: getCompanyApplicationCounts returns ranking sorted by count descending")
    void testGetCompanyApplicationCounts() throws SQLException {
        List<CompanyApplicationData> list = repository.getCompanyApplicationCounts(DEMO_USER_ID, null, 5);
        assertNotNull(list);
        assertFalse(list.isEmpty());
        assertTrue(list.size() <= 5, "Result size should not exceed requested limit of 5.");

        for (int i = 0; i < list.size() - 1; i++) {
            assertTrue(list.get(i).getApplicationCount() >= list.get(i + 1).getApplicationCount(),
                    "Companies must be ordered descending by application count.");
        }
    }

    @Test
    @DisplayName("Live: getInterviewOutcomeCounts returns outcome breakdown")
    void testGetInterviewOutcomeCounts() throws SQLException {
        Map<String, Integer> outcomes = repository.getInterviewOutcomeCounts(DEMO_USER_ID, null);
        assertNotNull(outcomes);
        assertFalse(outcomes.isEmpty(), "Outcome distribution should contain records.");
        assertTrue(outcomes.containsKey("Passed") || outcomes.containsKey("Scheduled"));
    }

    @Test
    @DisplayName("Live: getMonthlyInterviewCounts returns monthly interview rounds")
    void testGetMonthlyInterviewCounts() throws SQLException {
        List<MonthlyApplicationData> list = repository.getMonthlyInterviewCounts(DEMO_USER_ID, null);
        assertNotNull(list);
        assertFalse(list.isEmpty(), "Monthly interview rounds should contain entries.");
    }

    @Test
    @DisplayName("Live: Date range filtering correctly constrains application counts")
    void testDateRangeFiltering() throws SQLException {
        LocalDate startDate = LocalDate.of(2026, 11, 1);
        int filteredCount = repository.countTotalApplications(DEMO_USER_ID, startDate);
        int allTimeCount = repository.countTotalApplications(DEMO_USER_ID, null);

        assertTrue(filteredCount <= allTimeCount, "Filtered count must be less than or equal to all-time count.");
    }

    @Test
    @DisplayName("Live: User-Specific isolation ensures nonexistent user returns zero records")
    void testUserSpecificAnalyticsIsolation() throws SQLException {
        int nonExistentUserId = 999999;

        assertEquals(0, repository.countTotalApplications(nonExistentUserId, null));
        assertEquals(0, repository.countActiveApplications(nonExistentUserId, null));
        assertEquals(0, repository.countOffers(nonExistentUserId, null));
        assertEquals(0, repository.countRejected(nonExistentUserId, null));
        assertEquals(0, repository.countTotalInterviews(nonExistentUserId, null));
        assertEquals(0, repository.countDistinctApplicationsInterviewed(nonExistentUserId, null));
        assertEquals(0, repository.countUpcomingInterviews(nonExistentUserId));

        assertTrue(repository.getStatusCounts(nonExistentUserId, null).isEmpty());
        assertTrue(repository.getMonthlyApplicationCounts(nonExistentUserId, null).isEmpty());
        assertTrue(repository.getCompanyApplicationCounts(nonExistentUserId, null, 10).isEmpty());
        assertTrue(repository.getInterviewOutcomeCounts(nonExistentUserId, null).isEmpty());
        assertTrue(repository.getMonthlyInterviewCounts(nonExistentUserId, null).isEmpty());
    }

    @Test
    @DisplayName("Live: Cross-user data leakage prevention between two distinct accounts")
    void testPreventCrossUserDataLeakageWithSecondUser() throws SQLException {
        int user2Id = 0;
        int app2Id = 0;

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Create temporary test user 2
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO users (full_name, email, password_hash) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, "Test Isolation User");
                stmt.setString(2, "isolation.test." + System.currentTimeMillis() + "@applytrack.io");
                stmt.setString(3, "$2a$10$dummyHashValueForIsolationTesting12345");
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        user2Id = rs.getInt(1);
                    }
                }
            }

            assertTrue(user2Id > 0, "Created temporary second user.");

            // Create 1 application for user 2
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO applications (user_id, company_id, job_role, application_date, status) VALUES (?, 1, 'Isolation Role', '2026-11-01', 'Applied')",
                    Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, user2Id);
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        app2Id = rs.getInt(1);
                    }
                }
            }

            // Verify User 2 sees exactly 1 application
            assertEquals(1, repository.countTotalApplications(user2Id, null));
            assertEquals(1, repository.countActiveApplications(user2Id, null));

            // Verify User 1 total does NOT include User 2's application
            int user1Total = repository.countTotalApplications(DEMO_USER_ID, null);
            int allUsersTotal = 0;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM applications")) {
                if (rs.next()) {
                    allUsersTotal = rs.getInt(1);
                }
            }

            // User 1 total must be strictly less than all users total
            assertTrue(user1Total < allUsersTotal,
                    "User 1's query must not leak or count applications belonging to User 2.");

        } finally {
            // Clean up temporary user 2 (CASCADE deletes applications)
            if (user2Id > 0) {
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
                    stmt.setInt(1, user2Id);
                    stmt.executeUpdate();
                }
            }
        }
    }
}

