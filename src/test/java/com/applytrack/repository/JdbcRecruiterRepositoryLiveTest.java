package com.applytrack.repository;

import com.applytrack.database.DatabaseConnection;
import com.applytrack.model.Application;
import com.applytrack.model.Interview;
import com.applytrack.model.Recruiter;
import com.applytrack.repository.impl.JdbcRecruiterRepository;
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

@DisplayName("Phase 8: JdbcRecruiterRepository Live MariaDB Integration Tests")
class JdbcRecruiterRepositoryLiveTest {

    private static JdbcRecruiterRepository repo;

    private static final int DEMO_USER_ID = 1; // Alex Mercer
    private static final int STRANGER_USER_ID = 99999;

    @BeforeAll
    static void setUp() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            Assumptions.assumeTrue(conn != null && !conn.isClosed(), "MariaDB connection must be active");
            repo = new JdbcRecruiterRepository();
        } catch (SQLException e) {
            Assumptions.abort("MariaDB not available: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("1. findByUserId retrieves all existing seed recruiters for demo user")
    void testFindByUserId() throws SQLException {
        List<Recruiter> recruiters = repo.findByUserId(DEMO_USER_ID);
        assertNotNull(recruiters);
        assertTrue(recruiters.size() >= 4, "Demo user should have at least 4 seed recruiters (got: " + recruiters.size() + ")");

        // Multi-tenant check: stranger should have 0 recruiters
        List<Recruiter> strangerRecruiters = repo.findByUserId(STRANGER_USER_ID);
        assertNotNull(strangerRecruiters);
        assertTrue(strangerRecruiters.isEmpty());
    }

    @Test
    @DisplayName("2. findById retrieves recruiter with populated Company and associated app count")
    void testFindById() throws SQLException {
        Optional<Recruiter> rOpt = repo.findById(1, DEMO_USER_ID);
        assertTrue(rOpt.isPresent(), "Recruiter #1 (Sarah Jenkins) should exist for demo user");

        Recruiter recruiter = rOpt.get();
        assertEquals(1, recruiter.getId());
        assertEquals("Sarah Jenkins", recruiter.getRecruiterName());
        assertEquals("sarah.jenkins@example.com", recruiter.getEmail());

        assertNotNull(recruiter.getCompany(), "Associated Company must be populated");
        assertEquals("Google", recruiter.getCompanyName());
        assertTrue(recruiter.getAssociatedApplicationsCount() >= 1, "Sarah Jenkins should be associated with at least 1 application");
    }

    @Test
    @DisplayName("3. findByCompanyId retrieves recruiters matching companyId")
    void testFindByCompanyId() throws SQLException {
        List<Recruiter> googleRecruiters = repo.findByCompanyId(1, DEMO_USER_ID);
        assertNotNull(googleRecruiters);
        assertFalse(googleRecruiters.isEmpty(), "Should have recruiters for Google (Company 1)");
        for (Recruiter r : googleRecruiters) {
            assertEquals(1, r.getCompanyId());
            assertEquals("Google", r.getCompanyName());
        }
    }

    @Test
    @DisplayName("4. save() persists a new recruiter and returns generated primary key")
    void testSavePersistsAndGeneratesKey() throws SQLException {
        Recruiter recruiter = new Recruiter(
                0,
                1, // Google
                "David Chen (Live Test)",
                "david.chen@test-recruiter.io",
                "+1-650-555-8888",
                "https://linkedin.com/in/davidchen-live",
                "Engineering hiring partner",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        Recruiter saved = repo.save(recruiter);
        assertNotNull(saved);
        assertTrue(saved.getId() > 0, "Generated ID should be positive");

        try {
            Optional<Recruiter> fetched = repo.findById(saved.getId(), DEMO_USER_ID);
            assertTrue(fetched.isPresent());
            assertEquals("David Chen (Live Test)", fetched.get().getRecruiterName());
            assertEquals("Google", fetched.get().getCompanyName());
        } finally {
            // Clean up test row
            repo.delete(saved.getId(), DEMO_USER_ID);
        }
    }

    @Test
    @DisplayName("5. update() modifies recruiter details in MariaDB")
    void testUpdateRecruiter() throws SQLException {
        Recruiter recruiter = new Recruiter(
                0, 1, "Temp Recruiter for Update",
                "temp@google.com", "+1-650-555-0000", null, "Initial note",
                LocalDateTime.now(), LocalDateTime.now()
        );
        Recruiter saved = repo.save(recruiter);

        try {
            saved.setRecruiterName("Updated Talent Lead");
            saved.setEmail("lead@google.com");
            saved.setNotes("Updated promotion note");

            boolean updated = repo.update(saved, DEMO_USER_ID);
            assertTrue(updated, "Update query must return true");

            Optional<Recruiter> verify = repo.findById(saved.getId(), DEMO_USER_ID);
            assertTrue(verify.isPresent());
            assertEquals("Updated Talent Lead", verify.get().getRecruiterName());
            assertEquals("lead@google.com", verify.get().getEmail());
            assertEquals("Updated promotion note", verify.get().getNotes());
        } finally {
            repo.delete(saved.getId(), DEMO_USER_ID);
        }
    }

    @Test
    @DisplayName("6. delete() removes recruiter record from database")
    void testDeleteRecruiter() throws SQLException {
        Recruiter recruiter = new Recruiter(
                0, 1, "Recruiter to Delete",
                "delete.me@google.com", null, null, null,
                LocalDateTime.now(), LocalDateTime.now()
        );
        Recruiter saved = repo.save(recruiter);
        int id = saved.getId();

        boolean deleted = repo.delete(id, DEMO_USER_ID);
        assertTrue(deleted, "Delete should succeed");

        Optional<Recruiter> check = repo.findById(id, DEMO_USER_ID);
        assertTrue(check.isEmpty(), "Deleted recruiter should no longer exist in MariaDB");
    }

    @Test
    @DisplayName("7. Multi-tenant security: Stranger user cannot access demo user's recruiters")
    void testMultiTenantSecurity() throws SQLException {
        Optional<Recruiter> crossRead = repo.findById(1, STRANGER_USER_ID);
        assertTrue(crossRead.isEmpty(), "Stranger should not find demo user's recruiter");

        Optional<Recruiter> demoRecruiter = repo.findById(1, DEMO_USER_ID);
        assertTrue(demoRecruiter.isPresent());

        Recruiter toHack = demoRecruiter.get();
        toHack.setRecruiterName("Hacked Recruiter");

        boolean updateResult = repo.update(toHack, STRANGER_USER_ID);
        assertFalse(updateResult, "Stranger should not be able to update demo user's recruiter");

        boolean deleteResult = repo.delete(1, STRANGER_USER_ID);
        assertFalse(deleteResult, "Stranger should not be able to delete demo user's recruiter");
    }

    @Test
    @DisplayName("8. searchByKeyword() matches recruiter name, email, company, and phone")
    void testSearchByKeyword() throws SQLException {
        List<Recruiter> sarah = repo.searchByKeyword(DEMO_USER_ID, "Sarah");
        assertFalse(sarah.isEmpty(), "Search for 'Sarah' should return matches");

        List<Recruiter> google = repo.searchByKeyword(DEMO_USER_ID, "Google");
        assertFalse(google.isEmpty(), "Search for 'Google' should return matches");

        List<Recruiter> none = repo.searchByKeyword(DEMO_USER_ID, "ZzzNonExistentKeyword");
        assertTrue(none.isEmpty(), "Non-existent keyword should return empty list");
    }

    @Test
    @DisplayName("9. findAssociatedApplications and findAssociatedInterviews return linked pipeline items")
    void testAssociatedApplicationsAndInterviews() throws SQLException {
        // Recruiter 1 (Sarah Jenkins) is assigned to Application 1 (Google SDE)
        List<Application> apps = repo.findAssociatedApplications(1, DEMO_USER_ID);
        assertNotNull(apps);
        assertFalse(apps.isEmpty(), "Sarah Jenkins should have associated applications");
        assertEquals("Google", apps.get(0).getCompanyName());

        // Application 1 has scheduled interview rounds
        List<Interview> interviews = repo.findAssociatedInterviews(1, DEMO_USER_ID);
        assertNotNull(interviews);
        assertFalse(interviews.isEmpty(), "Sarah Jenkins should have associated interviews through App 1");
    }

    @Test
    @DisplayName("10. countRecruitersByUserId and associated count methods return accurate numbers")
    void testCountMethods() throws SQLException {
        int total = repo.countRecruitersByUserId(DEMO_USER_ID);
        assertTrue(total >= 4, "Demo user total recruiters should be >= 4");

        int linkedApps = repo.countAssociatedApplications(1, DEMO_USER_ID);
        assertTrue(linkedApps >= 1, "Sarah Jenkins linked applications should be >= 1");

        int linkedInterviews = repo.countAssociatedInterviews(1, DEMO_USER_ID);
        assertTrue(linkedInterviews >= 1, "Sarah Jenkins linked interviews should be >= 1");

        int strangerCount = repo.countRecruitersByUserId(STRANGER_USER_ID);
        assertEquals(0, strangerCount, "Stranger should have 0 accessible recruiters");

        int distinctCompanies = repo.countDistinctCompaniesByUserId(DEMO_USER_ID);
        assertTrue(distinctCompanies >= 1, "Demo user should have at least 1 distinct connected company");

        int linkedAppsCount = repo.countLinkedApplicationsByUserId(DEMO_USER_ID);
        assertTrue(linkedAppsCount >= 1, "Demo user should have at least 1 linked application");

        int directContacts = repo.countDirectContactsByUserId(DEMO_USER_ID);
        assertTrue(directContacts >= 1, "Demo user should have direct contacts");
    }

    @Test
    @DisplayName("11. findWithFilters() matches keyword across notes and linkedin profile")
    void testFindWithFiltersNotesAndKeyword() throws SQLException {
        // Search by keyword that appears in notes or company
        List<Recruiter> notesMatch = repo.findWithFilters(DEMO_USER_ID, "Campus", null, "name", true);
        assertNotNull(notesMatch);
        assertFalse(notesMatch.isEmpty(), "Search for 'Campus' in notes/designation must return matches");

        // Filter by specific company
        List<Recruiter> companyFiltered = repo.findWithFilters(DEMO_USER_ID, null, 1, "name", true);
        assertNotNull(companyFiltered);
        assertFalse(companyFiltered.isEmpty(), "Filtering by Google (ID: 1) should return recruiters");
        for (Recruiter r : companyFiltered) {
            assertEquals(Integer.valueOf(1), r.getCompanyId());
        }
    }
}

