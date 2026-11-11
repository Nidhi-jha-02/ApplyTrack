package com.applytrack.service;

import com.applytrack.exception.ValidationException;
import com.applytrack.model.Application;
import com.applytrack.model.Company;
import com.applytrack.model.enums.ApplicationStatus;
import com.applytrack.model.enums.JobType;
import com.applytrack.repository.ApplicationRepository;
import com.applytrack.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 6: ApplicationService Unit Tests")
class ApplicationServiceTest {

    private InMemoryApplicationRepository appRepo;
    private InMemoryCompanyRepository companyRepo;
    private ApplicationService service;

    private static final int USER_1 = 1;
    private static final int USER_2 = 2;

    @BeforeEach
    void setUp() {
        appRepo = new InMemoryApplicationRepository();
        companyRepo = new InMemoryCompanyRepository();
        service = new ApplicationService(appRepo, companyRepo);

        // Preload an existing application belonging to USER_1
        Company google = new Company(1, "Google", "https://google.com", "Tech", "CA", null, null);
        companyRepo.save(google);

        Application app1 = new Application(1, USER_1, 1, null, "SDE Backend", JobType.FULL_TIME,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1), null, null,
                new BigDecimal("120000"), ApplicationStatus.APPLIED, "Initial notes", null, null);
        app1.setCompany(google);
        appRepo.save(app1);
    }

    @Test
    @DisplayName("Adding a valid application persists to repository with resolved company")
    void testCreateApplicationSuccess() throws ValidationException, SQLException {
        Application created = service.createApplication(
                USER_1, "Microsoft", "SDE Intern", JobType.INTERNSHIP,
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 30),
                new BigDecimal("50000"), "https://careers.microsoft.com", "Azure dev",
                ApplicationStatus.APPLIED, "Submitted via campus"
        );

        assertNotNull(created);
        assertTrue(created.getId() > 0);
        assertEquals("Microsoft", created.getCompanyName());
        assertEquals("SDE Intern", created.getJobRole());
        assertEquals(USER_1, created.getUserId());
    }

    @Test
    @DisplayName("Updating an application succeeds when owner matches")
    void testUpdateApplicationSuccess() throws ValidationException, SQLException {
        Application updated = service.updateApplication(
                1, USER_1, "Google", "Senior SDE", JobType.FULL_TIME,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 15),
                new BigDecimal("140000"), "https://careers.google.com", "Updated description",
                ApplicationStatus.INTERVIEW_SCHEDULED, "Round 1 scheduled"
        );

        assertNotNull(updated);
        assertEquals("Senior SDE", updated.getJobRole());
        assertEquals(ApplicationStatus.INTERVIEW_SCHEDULED, updated.getStatus());
        assertEquals(new BigDecimal("140000"), updated.getExpectedSalary());
    }

    @Test
    @DisplayName("Deleting an application succeeds when owner matches")
    void testDeleteApplicationSuccess() throws ValidationException, SQLException {
        boolean deleted = service.deleteApplication(1, USER_1);
        assertTrue(deleted);
        assertTrue(service.getApplicationById(1, USER_1).isEmpty());
    }

    @Test
    @DisplayName("Required-field validation fails when company name or job role is empty")
    void testRequiredFieldValidation() {
        // Blank company
        ValidationException ex1 = assertThrows(ValidationException.class, () ->
                service.createApplication(USER_1, "   ", "SDE", JobType.FULL_TIME,
                        LocalDate.now(), null, null, null, null, ApplicationStatus.APPLIED, null));
        assertTrue(ex1.getMessage().contains("Company name is required"));

        // Blank role
        ValidationException ex2 = assertThrows(ValidationException.class, () ->
                service.createApplication(USER_1, "Amazon", "", JobType.FULL_TIME,
                        LocalDate.now(), null, null, null, null, ApplicationStatus.APPLIED, null));
        assertTrue(ex2.getMessage().contains("Job role is required"));

        // Null application date
        ValidationException ex3 = assertThrows(ValidationException.class, () ->
                service.createApplication(USER_1, "Amazon", "SDE", JobType.FULL_TIME,
                        null, null, null, null, null, ApplicationStatus.APPLIED, null));
        assertTrue(ex3.getMessage().contains("Application date is required"));
    }

    @Test
    @DisplayName("Invalid salary validation fails when salary is negative")
    void testNegativeSalaryValidation() {
        ValidationException ex = assertThrows(ValidationException.class, () ->
                service.createApplication(USER_1, "Meta", "SWE", JobType.FULL_TIME,
                        LocalDate.now(), null, new BigDecimal("-500.00"), null, null, ApplicationStatus.APPLIED, null));
        assertTrue(ex.getMessage().contains("cannot be negative"));
    }

    @Test
    @DisplayName("Invalid date validation fails when deadline is before application date")
    void testInvalidDeadlineValidation() {
        LocalDate appDate = LocalDate.of(2026, 11, 10);
        LocalDate invalidDeadline = LocalDate.of(2026, 11, 5); // Earlier than application date

        ValidationException ex = assertThrows(ValidationException.class, () ->
                service.createApplication(USER_1, "Apple", "iOS Dev", JobType.FULL_TIME,
                        appDate, invalidDeadline, null, null, null, ApplicationStatus.APPLIED, null));
        assertTrue(ex.getMessage().contains("cannot be earlier than application date"));
    }

    @Test
    @DisplayName("Fetching applications for user with 0 records returns empty list")
    void testEmptyApplicationList() throws SQLException {
        List<Application> apps = service.getApplicationsForUser(999);
        assertNotNull(apps);
        assertTrue(apps.isEmpty());
    }

    @Test
    @DisplayName("Preventing one user from accessing another user's application (Ownership Isolation)")
    void testPreventAccessAcrossUsers() throws SQLException {
        // App 1 belongs to USER_1. USER_2 queries for it.
        Optional<Application> foreignApp = service.getApplicationById(1, USER_2);
        assertTrue(foreignApp.isEmpty(), "User 2 must not be able to retrieve User 1's application");
    }

    @Test
    @DisplayName("Preventing one user from updating another user's application (Update Isolation)")
    void testPreventUpdateAcrossUsers() {
        // USER_2 attempts to update App 1 (owned by USER_1)
        ValidationException ex = assertThrows(ValidationException.class, () ->
                service.updateApplication(1, USER_2, "Hacked Company", "Hacked Role", JobType.FULL_TIME,
                        LocalDate.now(), null, null, null, null, ApplicationStatus.REJECTED, null));
        assertTrue(ex.getMessage().contains("permission to modify"));
    }

    @Test
    @DisplayName("Preventing one user from deleting another user's application (Delete Isolation)")
    void testPreventDeleteAcrossUsers() {
        // USER_2 attempts to delete App 1 (owned by USER_1)
        ValidationException ex = assertThrows(ValidationException.class, () ->
                service.deleteApplication(1, USER_2));
        assertTrue(ex.getMessage().contains("permission to delete"));
    }

    // --- In-Memory Repository Stubs for Pure Unit Testing ---
    private static class InMemoryApplicationRepository implements ApplicationRepository {
        private final List<Application> storage = new ArrayList<>();
        private int seq = 1;

        @Override
        public Application save(Application app) {
            if (app.getId() <= 0) {
                app.setId(++seq);
                storage.add(app);
            } else {
                boolean updated = update(app);
                if (!updated) {
                    storage.add(app);
                }
            }
            return app;
        }

        @Override
        public Optional<Application> findById(int applicationId, int userId) {
            return storage.stream()
                    .filter(a -> a.getId() == applicationId && a.getUserId() == userId)
                    .findFirst();
        }

        @Override
        public List<Application> findByUserId(int userId) {
            return storage.stream()
                    .filter(a -> a.getUserId() == userId)
                    .toList();
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
            return storage.stream()
                    .filter(a -> a.getUserId() == userId && a.matches(keyword))
                    .toList();
        }

        @Override
        public List<Application> filterByStatus(int userId, ApplicationStatus status) {
            return storage.stream()
                    .filter(a -> a.getUserId() == userId && a.getStatus() == status)
                    .toList();
        }

        @Override
        public List<Application> findWithFilters(int userId, String searchKeyword, ApplicationStatus statusFilter,
                                                JobType jobTypeFilter, String sortBy, boolean ascending) {
            return storage.stream()
                    .filter(a -> a.getUserId() == userId)
                    .filter(a -> searchKeyword == null || a.matches(searchKeyword))
                    .filter(a -> statusFilter == null || a.getStatus() == statusFilter)
                    .filter(a -> jobTypeFilter == null || a.getJobType() == jobTypeFilter)
                    .toList();
        }
    }

    private static class InMemoryCompanyRepository implements CompanyRepository {
        private final List<Company> storage = new ArrayList<>();
        private int seq = 10;

        @Override
        public Optional<Company> findById(int companyId) {
            return storage.stream().filter(c -> c.getId() == companyId).findFirst();
        }

        @Override
        public Optional<Company> findByName(String companyName) {
            return storage.stream()
                    .filter(c -> c.getCompanyName().equalsIgnoreCase(companyName.trim()))
                    .findFirst();
        }

        @Override
        public Company findOrCreateByName(String companyName) {
            return findByName(companyName).orElseGet(() -> {
                Company c = new Company(companyName.trim());
                c.setId(++seq);
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
            if (company.getId() <= 0) {
                company.setId(++seq);
                storage.add(company);
            }
            return company;
        }
    }
}
