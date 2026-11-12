package com.applytrack.service;

import com.applytrack.exception.ValidationException;
import com.applytrack.model.Application;
import com.applytrack.model.Company;
import com.applytrack.model.Interview;
import com.applytrack.model.Recruiter;
import com.applytrack.model.enums.ApplicationStatus;
import com.applytrack.model.enums.InterviewMode;
import com.applytrack.model.enums.InterviewOutcome;
import com.applytrack.model.enums.JobType;
import com.applytrack.repository.ApplicationRepository;
import com.applytrack.repository.CompanyRepository;
import com.applytrack.repository.RecruiterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 8: RecruiterService Unit Tests")
class RecruiterServiceTest {

    private InMemoryRecruiterRepository recruiterRepo;
    private InMemoryCompanyRepository companyRepo;
    private InMemoryApplicationRepository appRepo;
    private RecruiterService service;

    private static final int USER_1 = 1;
    private static final int USER_2 = 2; // Stranger

    private Company google;
    private Company microsoft;
    private Company meta;

    @BeforeEach
    void setUp() throws SQLException {
        recruiterRepo = new InMemoryRecruiterRepository();
        companyRepo = new InMemoryCompanyRepository();
        appRepo = new InMemoryApplicationRepository();
        service = new RecruiterService(recruiterRepo, companyRepo, appRepo);

        // Preload Companies
        google = new Company(1, "Google", "https://google.com", "Tech", "CA", null, null);
        microsoft = new Company(2, "Microsoft", "https://microsoft.com", "Tech", "WA", null, null);
        meta = new Company(3, "Meta", "https://meta.com", "Tech", "CA", null, null);

        companyRepo.save(google);
        companyRepo.save(microsoft);
        companyRepo.save(meta);

        // User 1 has applications with Google (1) and Microsoft (2)
        Application app1 = new Application(10, USER_1, 1, 100, "Backend Engineer", JobType.FULL_TIME,
                LocalDate.now(), null, null, null, new BigDecimal("120000"), ApplicationStatus.APPLIED, "Notes", null, null);
        app1.setCompany(google);
        appRepo.save(app1);

        Application app2 = new Application(20, USER_1, 2, null, "Frontend Engineer", JobType.FULL_TIME,
                LocalDate.now(), null, null, null, new BigDecimal("115000"), ApplicationStatus.APPLIED, "Notes", null, null);
        app2.setCompany(microsoft);
        appRepo.save(app2);

        // User 2 has application with Meta (3)
        Application app3 = new Application(30, USER_2, 3, 200, "ML Engineer", JobType.FULL_TIME,
                LocalDate.now(), null, null, null, new BigDecimal("140000"), ApplicationStatus.APPLIED, "Notes", null, null);
        app3.setCompany(meta);
        appRepo.save(app3);

        // Preload Recruiter 100 for Google (User 1 pipeline)
        Recruiter r1 = new Recruiter(100, 1, "Sarah Jenkins", "sarah@google.com", "+1-650-555-0199",
                "https://linkedin.com/in/sarahjenkins", "Campus recruiter", LocalDateTime.now(), LocalDateTime.now());
        r1.setCompany(google);
        recruiterRepo.save(r1);

        // Preload Recruiter 200 for Meta (User 2 pipeline)
        Recruiter r2 = new Recruiter(200, 3, "Mark Zuckerberg", "mark@meta.com", "+1-415-555-0100",
                "https://linkedin.com/in/zuck", "Lead talent scout", LocalDateTime.now(), LocalDateTime.now());
        r2.setCompany(meta);
        recruiterRepo.save(r2);
    }

    @Test
    @DisplayName("1. Adding a recruiter with valid details succeeds")
    void testCreateRecruiterSuccess() throws ValidationException, SQLException {
        Recruiter created = service.createRecruiter(
                USER_1, "Rohan Verma", 1, "rohan@google.com",
                "+91-98765-43210", "https://linkedin.com/in/rohanverma", "Senior campus scout"
        );

        assertNotNull(created);
        assertTrue(created.getId() > 0);
        assertEquals("Rohan Verma", created.getRecruiterName());
        assertEquals(1, created.getCompanyId());
        assertEquals("rohan@google.com", created.getEmail());
        assertEquals("+91-98765-43210", created.getPhone());
    }

    @Test
    @DisplayName("2. Fetching recruiters for a user returns only accessible recruiters")
    void testGetRecruitersForUser() {
        List<Recruiter> user1Recruiters = service.getRecruitersForUser(USER_1);
        assertEquals(1, user1Recruiters.size());
        assertEquals(100, user1Recruiters.get(0).getId());
        assertEquals("Sarah Jenkins", user1Recruiters.get(0).getRecruiterName());

        List<Recruiter> user2Recruiters = service.getRecruitersForUser(USER_2);
        assertEquals(1, user2Recruiters.size());
        assertEquals(200, user2Recruiters.get(0).getId());
        assertEquals("Mark Zuckerberg", user2Recruiters.get(0).getRecruiterName());
    }

    @Test
    @DisplayName("3. Updating a recruiter succeeds when user has permission")
    void testUpdateRecruiterSuccess() throws ValidationException, SQLException {
        Recruiter updated = service.updateRecruiter(
                100, USER_1, "Sarah Jenkins (Director)", 1,
                "sarah.director@google.com", "+1-650-555-9999",
                "https://linkedin.com/in/sarahjenkins-dir", "Promoted to Director of Talent"
        );

        assertNotNull(updated);
        assertEquals("Sarah Jenkins (Director)", updated.getRecruiterName());
        assertEquals("sarah.director@google.com", updated.getEmail());
        assertEquals("+1-650-555-9999", updated.getPhone());
    }

    @Test
    @DisplayName("4. Deleting an unassociated recruiter succeeds")
    void testDeleteUnassociatedRecruiter() throws ValidationException, SQLException {
        // Add a recruiter for Microsoft not yet linked to applications
        Recruiter unlinked = service.createRecruiter(USER_1, "John Smith", 2, "john@microsoft.com", null, null, null);
        assertTrue(unlinked.getId() > 0);

        boolean deleted = service.deleteRecruiter(unlinked.getId(), USER_1);
        assertTrue(deleted);

        Optional<Recruiter> check = service.getRecruiterById(unlinked.getId(), USER_1);
        assertTrue(check.isEmpty());
    }

    @Test
    @DisplayName("5. Deleting an associated recruiter succeeds and unlinks safely")
    void testDeleteAssociatedRecruiter() throws ValidationException, SQLException {
        // Recruiter 100 is linked to App 10
        int linkedCount = service.getAssociatedApplicationsCount(100, USER_1);
        assertTrue(linkedCount > 0, "Recruiter 100 should be associated with at least 1 application");

        boolean deleted = service.deleteRecruiter(100, USER_1);
        assertTrue(deleted);

        // Verify recruiter is deleted
        Optional<Recruiter> check = service.getRecruiterById(100, USER_1);
        assertTrue(check.isEmpty());
    }

    @Test
    @DisplayName("6. Searching by recruiter name matches case-insensitively")
    void testSearchByRecruiterName() {
        List<Recruiter> matches = service.searchRecruiters(USER_1, "sarah");
        assertEquals(1, matches.size());
        assertEquals("Sarah Jenkins", matches.get(0).getRecruiterName());

        List<Recruiter> none = service.searchRecruiters(USER_1, "NonExistentName");
        assertTrue(none.isEmpty());
    }

    @Test
    @DisplayName("7. Searching by email finds correct recruiter")
    void testSearchByEmail() {
        List<Recruiter> matches = service.searchRecruiters(USER_1, "google.com");
        assertEquals(1, matches.size());
        assertEquals("sarah@google.com", matches.get(0).getEmail());
    }

    @Test
    @DisplayName("8. Filtering by company returns recruiters matching companyId")
    void testFilterByCompany() throws ValidationException, SQLException {
        // Add recruiter for Microsoft
        service.createRecruiter(USER_1, "Satya Talent", 2, "satya@microsoft.com", null, null, null);

        List<Recruiter> googleRecruiters = service.getRecruitersByCompany(1, USER_1);
        assertEquals(1, googleRecruiters.size());
        assertEquals("Sarah Jenkins", googleRecruiters.get(0).getRecruiterName());

        List<Recruiter> msRecruiters = service.getRecruitersByCompany(2, USER_1);
        assertEquals(1, msRecruiters.size());
        assertEquals("Satya Talent", msRecruiters.get(0).getRecruiterName());
    }

    @Test
    @DisplayName("9. Invalid email format throws ValidationException")
    void testInvalidEmailThrows() {
        assertThrows(ValidationException.class, () ->
                service.createRecruiter(USER_1, "Jane Doe", 1, "invalid_email_at_google", null, null, null)
        );
        assertThrows(ValidationException.class, () ->
                service.createRecruiter(USER_1, "Jane Doe", 1, "plainaddress", null, null, null)
        );
    }

    @Test
    @DisplayName("10. Invalid phone number format throws ValidationException")
    void testInvalidPhoneThrows() {
        assertThrows(ValidationException.class, () ->
                service.createRecruiter(USER_1, "Jane Doe", 1, null, "123", null, null) // too short
        );
        assertThrows(ValidationException.class, () ->
                service.createRecruiter(USER_1, "Jane Doe", 1, null, "abc-defg-hijk", null, null) // letters
        );
    }

    @Test
    @DisplayName("11. Missing or blank recruiter name throws ValidationException")
    void testMissingRecruiterNameThrows() {
        assertThrows(ValidationException.class, () ->
                service.createRecruiter(USER_1, "", 1, null, null, null, null)
        );
        assertThrows(ValidationException.class, () ->
                service.createRecruiter(USER_1, "   ", 1, null, null, null, null)
        );
        assertThrows(ValidationException.class, () ->
                service.createRecruiter(USER_1, null, 1, null, null, null, null)
        );
    }

    @Test
    @DisplayName("12. Empty recruiter list handled gracefully for user with no applications/recruiters")
    void testEmptyRecruiterList() {
        int strangerWithoutApps = 999;
        List<Recruiter> empty = service.getRecruitersForUser(strangerWithoutApps);
        assertNotNull(empty);
        assertTrue(empty.isEmpty());
        assertEquals(0, service.getRecruiterCount(strangerWithoutApps));
    }

    @Test
    @DisplayName("13. Preventing unauthorized recruiter access: User 2 cannot access User 1's recruiter")
    void testPreventUnauthorizedRecruiterAccess() {
        Optional<Recruiter> crossAccess = service.getRecruiterById(100, USER_2);
        assertTrue(crossAccess.isEmpty(), "User 2 must not be able to read User 1's recruiter #100");
    }

    @Test
    @DisplayName("14. Preventing unauthorized recruiter updates: User 2 cannot edit User 1's recruiter")
    void testPreventUnauthorizedRecruiterUpdates() {
        ValidationException ex = assertThrows(ValidationException.class, () ->
                service.updateRecruiter(100, USER_2, "Hacked Name", 1, null, null, null, null)
        );
        assertTrue(ex.getMessage().contains("do not have permission to modify"));
    }

    @Test
    @DisplayName("15. Preventing unauthorized recruiter deletion: User 2 cannot delete User 1's recruiter")
    void testPreventUnauthorizedRecruiterDeletion() {
        ValidationException ex = assertThrows(ValidationException.class, () ->
                service.deleteRecruiter(100, USER_2)
        );
        assertTrue(ex.getMessage().contains("do not have permission to delete"));
    }

    @Test
    @DisplayName("16. Preventing unauthorized company association: Cannot associate recruiter with unapplied company")
    void testPreventUnauthorizedCompanyAssociation() {
        // User 1 has NOT applied to Meta (company 3)
        ValidationException ex = assertThrows(ValidationException.class, () ->
                service.createRecruiter(USER_1, "New Recruiter", 3, "new@meta.com", null, null, null)
        );
        assertTrue(ex.getMessage().contains("only associate recruiters with companies in your job application pipeline"));
    }

    @Test
    @DisplayName("17. Application integration: Linked applications are retrieved accurately")
    void testApplicationIntegration() {
        List<Application> linked = service.getAssociatedApplications(100, USER_1);
        assertEquals(1, linked.size());
        assertEquals(10, linked.get(0).getId());
        assertEquals("Backend Engineer", linked.get(0).getJobRole());
    }

    @Test
    @DisplayName("18. Interview integration: Linked interviews for recruiter's applications are retrieved")
    void testInterviewIntegration() {
        // Add interview for application 10
        Interview interview = new Interview(500, 10, "Technical Round 1",
                LocalDateTime.now().plusDays(2), InterviewMode.ONLINE, "Remote", null, "Sarah Jenkins",
                "Prep DSA", InterviewOutcome.SCHEDULED, LocalDateTime.now(), LocalDateTime.now());
        recruiterRepo.addInterview(interview);

        List<Interview> linkedInterviews = service.getAssociatedInterviews(100, USER_1);
        assertEquals(1, linkedInterviews.size());
        assertEquals("Technical Round 1", linkedInterviews.get(0).getRoundName());
    }

    // -------------------------------------------------------------
    // In-Memory Stubs for Fast Unit Testing
    // -------------------------------------------------------------

    private class InMemoryRecruiterRepository implements RecruiterRepository {
        private final List<Recruiter> storage = new ArrayList<>();
        private final List<Interview> interviews = new ArrayList<>();
        private int seq = 300;

        public void addInterview(Interview i) {
            interviews.add(i);
        }

        @Override
        public Recruiter save(Recruiter recruiter) {
            if (recruiter.getId() <= 0) {
                recruiter.setId(++seq);
            }
            storage.removeIf(r -> r.getId() == recruiter.getId());
            storage.add(recruiter);
            return recruiter;
        }

        @Override
        public Optional<Recruiter> findById(int recruiterId, int userId) {
            return storage.stream()
                    .filter(r -> r.getId() == recruiterId && isRecruiterAccessibleToUser(r, userId))
                    .findFirst();
        }

        @Override
        public List<Recruiter> findByUserId(int userId) {
            return storage.stream()
                    .filter(r -> isRecruiterAccessibleToUser(r, userId))
                    .sorted((a, b) -> a.getRecruiterName().compareToIgnoreCase(b.getRecruiterName()))
                    .toList();
        }

        @Override
        public List<Recruiter> findByCompanyId(int companyId, int userId) {
            return storage.stream()
                    .filter(r -> r.getCompanyId() != null && r.getCompanyId() == companyId && isRecruiterAccessibleToUser(r, userId))
                    .sorted((a, b) -> a.getRecruiterName().compareToIgnoreCase(b.getRecruiterName()))
                    .toList();
        }

        @Override
        public boolean update(Recruiter recruiter, int userId) {
            for (int i = 0; i < storage.size(); i++) {
                Recruiter existing = storage.get(i);
                if (existing.getId() == recruiter.getId() && isRecruiterAccessibleToUser(existing, userId)) {
                    storage.set(i, recruiter);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean delete(int recruiterId, int userId) {
            boolean removed = storage.removeIf(r -> r.getId() == recruiterId && isRecruiterAccessibleToUser(r, userId));
            if (removed) {
                // SET NULL on linked applications
                for (Application app : appRepo.storage) {
                    if (app.getRecruiterId() != null && app.getRecruiterId() == recruiterId) {
                        app.setRecruiterId(null);
                    }
                }
            }
            return removed;
        }

        @Override
        public List<Recruiter> searchByKeyword(int userId, String keyword) {
            return storage.stream()
                    .filter(r -> isRecruiterAccessibleToUser(r, userId) && r.matches(keyword))
                    .toList();
        }

        @Override
        public List<Recruiter> findWithFilters(int userId, String searchKeyword, Integer companyId, String sortBy, boolean ascending) {
            return storage.stream()
                    .filter(r -> isRecruiterAccessibleToUser(r, userId))
                    .filter(r -> searchKeyword == null || searchKeyword.isBlank() || r.matches(searchKeyword))
                    .filter(r -> companyId == null || (r.getCompanyId() != null && r.getCompanyId().equals(companyId)))
                    .toList();
        }

        @Override
        public int countRecruitersByUserId(int userId) {
            return (int) storage.stream().filter(r -> isRecruiterAccessibleToUser(r, userId)).count();
        }

        @Override
        public int countAssociatedApplications(int recruiterId, int userId) {
            return (int) appRepo.storage.stream()
                    .filter(a -> a.getUserId() == userId && a.getRecruiterId() != null && a.getRecruiterId() == recruiterId)
                    .count();
        }

        @Override
        public int countAssociatedInterviews(int recruiterId, int userId) {
            return (int) interviews.stream()
                    .filter(i -> {
                        Optional<Application> app = appRepo.findById(i.getApplicationId(), userId);
                        return app.isPresent() && app.get().getRecruiterId() != null && app.get().getRecruiterId() == recruiterId;
                    })
                    .count();
        }

        @Override
        public List<Application> findAssociatedApplications(int recruiterId, int userId) {
            return appRepo.storage.stream()
                    .filter(a -> a.getUserId() == userId && a.getRecruiterId() != null && a.getRecruiterId() == recruiterId)
                    .toList();
        }

        @Override
        public List<Interview> findAssociatedInterviews(int recruiterId, int userId) {
            return interviews.stream()
                    .filter(i -> {
                        Optional<Application> app = appRepo.findById(i.getApplicationId(), userId);
                        return app.isPresent() && app.get().getRecruiterId() != null && app.get().getRecruiterId() == recruiterId;
                    })
                    .toList();
        }

        @Override
        public boolean isCompanyAccessibleToUser(int companyId, int userId) {
            return appRepo.storage.stream().anyMatch(a -> a.getUserId() == userId && a.getCompanyId() == companyId);
        }

        @Override
        public int countDistinctCompaniesByUserId(int userId) {
            return (int) storage.stream()
                    .filter(r -> isRecruiterAccessibleToUser(r, userId) && r.getCompanyId() != null)
                    .map(Recruiter::getCompanyId)
                    .distinct()
                    .count();
        }

        @Override
        public int countLinkedApplicationsByUserId(int userId) {
            return (int) appRepo.storage.stream()
                    .filter(a -> a.getUserId() == userId && a.getRecruiterId() != null)
                    .count();
        }

        @Override
        public int countDirectContactsByUserId(int userId) {
            return (int) storage.stream()
                    .filter(r -> isRecruiterAccessibleToUser(r, userId))
                    .filter(r -> (r.getEmail() != null && !r.getEmail().isBlank()) || (r.getPhone() != null && !r.getPhone().isBlank()))
                    .count();
        }

        private boolean isRecruiterAccessibleToUser(Recruiter r, int userId) {
            // Check direct application link
            boolean directApp = appRepo.storage.stream()
                    .anyMatch(a -> a.getUserId() == userId && a.getRecruiterId() != null && a.getRecruiterId() == r.getId());
            if (directApp) return true;

            // Check company pipeline
            if (r.getCompanyId() != null) {
                return appRepo.storage.stream().anyMatch(a -> a.getUserId() == userId && a.getCompanyId() == r.getCompanyId());
            }
            return false;
        }
    }

    private static class InMemoryCompanyRepository implements CompanyRepository {
        private final List<Company> storage = new ArrayList<>();

        @Override
        public Optional<Company> findById(int companyId) {
            return storage.stream().filter(c -> c.getId() == companyId).findFirst();
        }

        @Override
        public Optional<Company> findByName(String companyName) {
            return storage.stream().filter(c -> c.getCompanyName().equalsIgnoreCase(companyName.trim())).findFirst();
        }

        @Override
        public Company findOrCreateByName(String companyName) {
            return findByName(companyName).orElseGet(() -> {
                Company c = new Company(companyName.trim());
                c.setId(storage.size() + 1);
                storage.add(c);
                return c;
            });
        }

        @Override
        public List<Company> findAll() {
            return new ArrayList<>(storage);
        }

        @Override
        public Company save(Company company) {
            storage.removeIf(c -> c.getId() == company.getId());
            storage.add(company);
            return company;
        }
    }

    private static class InMemoryApplicationRepository implements ApplicationRepository {
        private final List<Application> storage = new ArrayList<>();

        @Override
        public Application save(Application application) {
            storage.removeIf(a -> a.getId() == application.getId());
            storage.add(application);
            return application;
        }

        @Override
        public Optional<Application> findById(int applicationId, int userId) {
            return storage.stream().filter(a -> a.getId() == applicationId && a.getUserId() == userId).findFirst();
        }

        @Override
        public List<Application> findByUserId(int userId) {
            return storage.stream().filter(a -> a.getUserId() == userId).toList();
        }

        @Override
        public boolean update(Application application) {
            for (int i = 0; i < storage.size(); i++) {
                if (storage.get(i).getId() == application.getId() && storage.get(i).getUserId() == application.getUserId()) {
                    storage.set(i, application);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean delete(int applicationId, int userId) {
            return storage.removeIf(a -> a.getId() == applicationId && a.getUserId() == userId);
        }

        @Override
        public List<Application> searchByKeyword(int userId, String keyword) {
            return Collections.emptyList();
        }

        @Override
        public List<Application> filterByStatus(int userId, ApplicationStatus status) {
            return Collections.emptyList();
        }

        @Override
        public List<Application> findWithFilters(int userId, String searchKeyword, ApplicationStatus statusFilter, JobType jobTypeFilter, String sortBy, boolean ascending) {
            return Collections.emptyList();
        }
    }
}

