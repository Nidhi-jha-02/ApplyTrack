package com.applytrack.repository;

import com.applytrack.database.DatabaseConnection;
import com.applytrack.model.Application;
import com.applytrack.model.Company;
import com.applytrack.model.enums.ApplicationStatus;
import com.applytrack.model.enums.JobType;
import com.applytrack.repository.impl.JdbcApplicationRepository;
import com.applytrack.repository.impl.JdbcCompanyRepository;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 6: JdbcApplicationRepository Live MariaDB Integration Tests")
class JdbcApplicationRepositoryLiveTest {

    private static JdbcApplicationRepository appRepo;
    private static JdbcCompanyRepository companyRepo;

    private static final int DEMO_USER_ID = 1; // Alex Mercer
    private static final int STRANGER_USER_ID = 99999;

    @BeforeAll
    static void setUp() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            Assumptions.assumeTrue(conn != null && !conn.isClosed(), "MariaDB connection must be active");
            appRepo = new JdbcApplicationRepository();
            companyRepo = new JdbcCompanyRepository();
        } catch (SQLException e) {
            Assumptions.abort("MariaDB not available: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("findByUserId returns existing seed applications for demo user")
    void testFindByUserId() throws SQLException {
        List<Application> apps = appRepo.findByUserId(DEMO_USER_ID);
        assertNotNull(apps);
        assertTrue(apps.size() >= 6, "Demo user should have at least 6 seed applications (got: " + apps.size() + ")");

        // Stranger should have 0 applications
        List<Application> strangerApps = appRepo.findByUserId(STRANGER_USER_ID);
        assertNotNull(strangerApps);
        assertTrue(strangerApps.isEmpty());
    }

    @Test
    @DisplayName("findById retrieves application with populated company details")
    void testFindById() throws SQLException {
        Optional<Application> appOpt = appRepo.findById(1, DEMO_USER_ID);
        assertTrue(appOpt.isPresent());

        Application app = appOpt.get();
        assertEquals(1, app.getId());
        assertEquals(DEMO_USER_ID, app.getUserId());
        assertNotNull(app.getCompany());
        assertEquals("Google", app.getCompany().getCompanyName());
        assertEquals("Software Engineer - Backend", app.getJobRole());
    }

    @Test
    @DisplayName("Complete CRUD lifecycle: Insert, Read, Update, Delete with MariaDB")
    void testApplicationCrudLifecycle() throws SQLException {
        // 1. Resolve or create company
        Company company = companyRepo.findOrCreateByName("Netflix");
        assertNotNull(company);
        assertTrue(company.getId() > 0);

        // 2. Insert new application
        Application newApp = new Application(
                DEMO_USER_ID,
                company.getId(),
                null,
                "Cloud Infrastructure Engineer",
                JobType.FULL_TIME,
                LocalDate.now(),
                LocalDate.now().plusDays(25),
                new BigDecimal("155000.00"),
                ApplicationStatus.APPLIED,
                "Applied via referral link"
        );
        newApp.setJobUrl("https://jobs.netflix.com/position/998877");
        newApp.setJobDescription("Distributed systems and media encoding");

        Application saved = appRepo.save(newApp);
        assertNotNull(saved);
        assertTrue(saved.getId() > 0, "Saved application must have generated primary key ID");
        int createdId = saved.getId();

        // 3. Read back
        Optional<Application> retrievedOpt = appRepo.findById(createdId, DEMO_USER_ID);
        assertTrue(retrievedOpt.isPresent());
        assertEquals("Cloud Infrastructure Engineer", retrievedOpt.get().getJobRole());
        assertEquals("Netflix", retrievedOpt.get().getCompanyName());

        // 4. Update
        Application toUpdate = retrievedOpt.get();
        toUpdate.setJobRole("Senior Cloud Infrastructure Engineer");
        toUpdate.setStatus(ApplicationStatus.UNDER_REVIEW);
        toUpdate.setExpectedSalary(new BigDecimal("165000.00"));

        boolean updated = appRepo.update(toUpdate);
        assertTrue(updated);

        Optional<Application> updatedOpt = appRepo.findById(createdId, DEMO_USER_ID);
        assertTrue(updatedOpt.isPresent());
        assertEquals("Senior Cloud Infrastructure Engineer", updatedOpt.get().getJobRole());
        assertEquals(ApplicationStatus.UNDER_REVIEW, updatedOpt.get().getStatus());
        assertEquals(new BigDecimal("165000.00"), updatedOpt.get().getExpectedSalary());

        // 5. Delete
        boolean deleted = appRepo.delete(createdId, DEMO_USER_ID);
        assertTrue(deleted);

        // Confirm deleted
        Optional<Application> postDelete = appRepo.findById(createdId, DEMO_USER_ID);
        assertTrue(postDelete.isEmpty());
    }

    @Test
    @DisplayName("Search applications by company name and job role")
    void testSearchApplications() throws SQLException {
        // Search by company
        List<Application> googleApps = appRepo.findWithFilters(DEMO_USER_ID, "Google", null, null, "application_date", false);
        assertFalse(googleApps.isEmpty());
        assertTrue(googleApps.stream().allMatch(a -> a.getCompanyName().toLowerCase().contains("google")));

        // Search by role
        List<Application> internApps = appRepo.findWithFilters(DEMO_USER_ID, "Intern", null, null, "application_date", false);
        assertFalse(internApps.isEmpty());
        assertTrue(internApps.stream().allMatch(a -> a.getJobRole().toLowerCase().contains("intern")));
    }

    @Test
    @DisplayName("Filter applications by application status")
    void testFilterByStatus() throws SQLException {
        List<Application> offers = appRepo.findWithFilters(DEMO_USER_ID, null, ApplicationStatus.SELECTED, null, "application_date", false);
        assertFalse(offers.isEmpty());
        assertEquals("Selected", offers.get(0).getStatus().getDisplayName());

        List<Application> rejections = appRepo.findWithFilters(DEMO_USER_ID, null, ApplicationStatus.REJECTED, null, "application_date", false);
        assertFalse(rejections.isEmpty());
        assertEquals("Rejected", rejections.get(0).getStatus().getDisplayName());
    }

    @Test
    @DisplayName("Sort applications by application date, deadline and company name")
    void testSortApplications() throws SQLException {
        // Sort by company ASC
        List<Application> byCompanyAsc = appRepo.findWithFilters(DEMO_USER_ID, null, null, null, "company_name", true);
        for (int i = 0; i < byCompanyAsc.size() - 1; i++) {
            String c1 = byCompanyAsc.get(i).getCompanyName();
            String c2 = byCompanyAsc.get(i + 1).getCompanyName();
            assertTrue(c1.compareToIgnoreCase(c2) <= 0, "Companies must be sorted ASC");
        }

        // Sort by application date DESC
        List<Application> byDateDesc = appRepo.findWithFilters(DEMO_USER_ID, null, null, null, "application_date", false);
        for (int i = 0; i < byDateDesc.size() - 1; i++) {
            LocalDate d1 = byDateDesc.get(i).getApplicationDate();
            LocalDate d2 = byDateDesc.get(i + 1).getApplicationDate();
            assertTrue(d1.compareTo(d2) >= 0, "Dates must be sorted DESC");
        }
    }

    @Test
    @DisplayName("Preventing one user from accessing, updating, or deleting another user's application (SQL Isolation)")
    void testUserIsolationAtDatabaseLayer() throws SQLException {
        // App 1 belongs to DEMO_USER_ID (1)
        Optional<Application> foreignFetch = appRepo.findById(1, STRANGER_USER_ID);
        assertTrue(foreignFetch.isEmpty(), "Stranger cannot find User 1's application");

        // Stranger tries to delete App 1
        boolean deleteAttempt = appRepo.delete(1, STRANGER_USER_ID);
        assertFalse(deleteAttempt, "Stranger cannot delete User 1's application");

        // Verify App 1 is still intact
        Optional<Application> original = appRepo.findById(1, DEMO_USER_ID);
        assertTrue(original.isPresent(), "User 1's application must still exist");

        // Stranger tries to update App 1
        Application stolen = original.get();
        stolen.setUserId(STRANGER_USER_ID); // Claims to be owner
        stolen.setJobRole("Hacked SDE");
        boolean updateAttempt = appRepo.update(stolen);
        assertFalse(updateAttempt, "Stranger cannot update User 1's application even if they tamper with entity state");
    }
}

