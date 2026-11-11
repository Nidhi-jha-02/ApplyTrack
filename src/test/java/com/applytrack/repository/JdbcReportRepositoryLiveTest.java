package com.applytrack.repository;

import com.applytrack.database.DatabaseConnection;
import com.applytrack.model.dto.ApplicationReportItem;
import com.applytrack.model.dto.DateWiseActivityItem;
import com.applytrack.model.dto.InterviewReportItem;
import com.applytrack.model.dto.RecruiterReportItem;
import com.applytrack.repository.impl.JdbcReportRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Live Database Integration Tests for JdbcReportRepository against real MariaDB instance.
 *
 * Demonstrates:
 * - Integration Testing against relational database schema (Unit 3)
 * - Multi-tenant security & user isolation enforcement (Unit 3)
 * - SQL JOINs across applications, companies, interviews, recruiters (Unit 3)
 * - Date filtering and boundary conditions (Unit 1 & Unit 3)
 */
public class JdbcReportRepositoryLiveTest {

    private static final int DEMO_USER_ID = 1; // Alex Mercer demo user
    private static final int NON_EXISTENT_USER_ID = 999999;
    private static JdbcReportRepository repository;

    @BeforeAll
    static void checkDatabaseAvailability() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            assumeTrue(conn != null && !conn.isClosed(), "MariaDB database is available for live testing.");
            repository = new JdbcReportRepository();
        } catch (SQLException e) {
            assumeTrue(false, "Skipping live tests: MariaDB connection unavailable (" + e.getMessage() + ")");
        }
    }

    @Test
    @DisplayName("Live: getApplicationReportData returns populated records for Demo User")
    void testGetApplicationReportDataLiveUser() throws SQLException {
        List<ApplicationReportItem> apps = repository.getApplicationReportData(DEMO_USER_ID, null, null);
        assertNotNull(apps);
        assertFalse(apps.isEmpty(), "Demo user should have seeded applications");

        ApplicationReportItem item = apps.get(0);
        assertNotNull(item.getCompanyName(), "Company name should be populated via JOIN");
        assertNotNull(item.getJobRole(), "Job role should be populated");
        assertNotNull(item.getStatus(), "Status should be populated");
        assertNotNull(item.getApplicationDate(), "Application date should be populated");
    }

    @Test
    @DisplayName("Live: getApplicationReportData respects date boundaries")
    void testGetApplicationReportDataDateFiltered() throws SQLException {
        LocalDate start = LocalDate.now().minusMonths(12);
        LocalDate end = LocalDate.now().plusDays(1);

        List<ApplicationReportItem> filteredApps = repository.getApplicationReportData(DEMO_USER_ID, start, end);
        assertNotNull(filteredApps);

        for (ApplicationReportItem item : filteredApps) {
            assertFalse(item.getApplicationDate().isBefore(start), "Item date should be >= start date");
            assertFalse(item.getApplicationDate().isAfter(end), "Item date should be <= end date");
        }
    }

    @Test
    @DisplayName("Live: getInterviewReportData returns populated interview rounds")
    void testGetInterviewReportDataLiveUser() throws SQLException {
        List<InterviewReportItem> interviews = repository.getInterviewReportData(DEMO_USER_ID, null, null);
        assertNotNull(interviews);

        if (!interviews.isEmpty()) {
            InterviewReportItem item = interviews.get(0);
            assertNotNull(item.getRoundName(), "Round name should be populated");
            assertNotNull(item.getCompanyName(), "Company name should be populated via multi-table JOIN");
            assertNotNull(item.getInterviewDate(), "Interview date should be populated");
            assertNotNull(item.getOutcome(), "Outcome should be populated");
        }
    }

    @Test
    @DisplayName("Live: getRecruiterReportData returns recruiter contacts with company details")
    void testGetRecruiterReportDataLiveUser() throws SQLException {
        List<RecruiterReportItem> recruiters = repository.getRecruiterReportData(DEMO_USER_ID);
        assertNotNull(recruiters);

        if (!recruiters.isEmpty()) {
            RecruiterReportItem item = recruiters.get(0);
            assertNotNull(item.getRecruiterName());
            assertNotNull(item.getCompanyName());
            assertTrue(item.getAssociatedAppsCount() >= 0);
        }
    }

    @Test
    @DisplayName("Live: getDateWiseActivityData returns combined timeline sorted descending")
    void testGetDateWiseActivityDataLiveUser() throws SQLException {
        List<DateWiseActivityItem> activities = repository.getDateWiseActivityData(DEMO_USER_ID, null, null);
        assertNotNull(activities);
        assertFalse(activities.isEmpty(), "Demo user should have combined activities");

        // Verify descending chronological order
        for (int i = 1; i < activities.size(); i++) {
            LocalDate prev = activities.get(i - 1).getActivityDate();
            LocalDate curr = activities.get(i).getActivityDate();
            assertTrue(prev.compareTo(curr) >= 0, "Activities must be sorted in descending chronological order");
        }
    }

    @Test
    @DisplayName("Security: Multi-tenant isolation returns 0 records for non-existent or unlinked user")
    void testMultiTenantSecurityIsolation() throws SQLException {
        List<ApplicationReportItem> apps = repository.getApplicationReportData(NON_EXISTENT_USER_ID, null, null);
        assertNotNull(apps);
        assertTrue(apps.isEmpty(), "Non-existent user must return zero applications");

        List<InterviewReportItem> interviews = repository.getInterviewReportData(NON_EXISTENT_USER_ID, null, null);
        assertNotNull(interviews);
        assertTrue(interviews.isEmpty(), "Non-existent user must return zero interviews");

        List<RecruiterReportItem> recruiters = repository.getRecruiterReportData(NON_EXISTENT_USER_ID);
        assertNotNull(recruiters);
        assertTrue(recruiters.isEmpty(), "Non-existent user must return zero recruiters");

        List<DateWiseActivityItem> activities = repository.getDateWiseActivityData(NON_EXISTENT_USER_ID, null, null);
        assertNotNull(activities);
        assertTrue(activities.isEmpty(), "Non-existent user must return zero activities");
    }
}
