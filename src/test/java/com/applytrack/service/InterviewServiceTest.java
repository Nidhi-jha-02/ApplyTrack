package com.applytrack.service;

import com.applytrack.exception.ValidationException;
import com.applytrack.model.Application;
import com.applytrack.model.Company;
import com.applytrack.model.Interview;
import com.applytrack.model.enums.ApplicationStatus;
import com.applytrack.model.enums.InterviewMode;
import com.applytrack.model.enums.InterviewOutcome;
import com.applytrack.model.enums.JobType;
import com.applytrack.repository.ApplicationRepository;
import com.applytrack.repository.InterviewRepository;
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

@DisplayName("Phase 7: InterviewService Unit Tests")
class InterviewServiceTest {

    private InMemoryInterviewRepository interviewRepo;
    private InMemoryApplicationRepository appRepo;
    private InterviewService service;

    private static final int USER_1 = 1;
    private static final int USER_2 = 2;

    private Application appUser1;
    private Application appUser2;

    @BeforeEach
    void setUp() throws SQLException {
        interviewRepo = new InMemoryInterviewRepository();
        appRepo = new InMemoryApplicationRepository();
        service = new InterviewService(interviewRepo, appRepo);

        // Preload Application 10 for User 1
        Company google = new Company(1, "Google", "https://google.com", "Tech", "CA", null, null);
        appUser1 = new Application(10, USER_1, 1, null, "Software Engineer", JobType.FULL_TIME,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1), null, null,
                new BigDecimal("130000"), ApplicationStatus.APPLIED, "Notes", null, null);
        appUser1.setCompany(google);
        appRepo.save(appUser1);

        // Preload Application 20 for User 2
        Company meta = new Company(2, "Meta", "https://meta.com", "Tech", "Menlo Park", null, null);
        appUser2 = new Application(20, USER_2, 2, null, "Frontend Engineer", JobType.FULL_TIME,
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 11, 15), null, null,
                new BigDecimal("125000"), ApplicationStatus.APPLIED, "Notes", null, null);
        appUser2.setCompany(meta);
        appRepo.save(appUser2);

        // Preload Interview 100 for User 1
        Interview interview1 = new Interview(100, 10, "Technical Round 1",
                LocalDateTime.now().plusDays(2), InterviewMode.ONLINE, "Remote",
                "https://meet.google.com/abc-xyz", "Sarah Lee", "Focus on graphs",
                InterviewOutcome.SCHEDULED, LocalDateTime.now(), LocalDateTime.now());
        interview1.setApplication(appUser1);
        interviewRepo.save(interview1);
    }

    @Test
    @DisplayName("1. Scheduling an interview with valid inputs persists successfully")
    void testScheduleInterviewSuccess() throws ValidationException, SQLException {
        LocalDateTime date = LocalDateTime.now().plusDays(5);
        Interview interview = service.scheduleInterview(
                10, USER_1, "System Design Round", date,
                InterviewMode.ONLINE, "Virtual", "https://meet.google.com/test-room",
                "Alex Rivera", "Prepare on scalable architecture", InterviewOutcome.SCHEDULED
        );

        assertNotNull(interview);
        assertTrue(interview.getId() > 0);
        assertEquals("System Design Round", interview.getRoundName());
        assertEquals("Google", interview.getCompanyName());
        assertEquals("Software Engineer", interview.getJobRole());
        assertEquals(InterviewMode.ONLINE, interview.getInterviewMode());
        assertEquals(InterviewOutcome.SCHEDULED, interview.getOutcome());
    }

    @Test
    @DisplayName("2. Scheduling with blank or null round name throws ValidationException")
    void testScheduleInterviewEmptyRoundName() {
        LocalDateTime date = LocalDateTime.now().plusDays(2);
        assertThrows(ValidationException.class, () ->
                service.scheduleInterview(10, USER_1, "", date, InterviewMode.ONLINE,
                        null, null, null, null, InterviewOutcome.SCHEDULED)
        );
        assertThrows(ValidationException.class, () ->
                service.scheduleInterview(10, USER_1, "   ", date, InterviewMode.ONLINE,
                        null, null, null, null, InterviewOutcome.SCHEDULED)
        );
        assertThrows(ValidationException.class, () ->
                service.scheduleInterview(10, USER_1, null, date, InterviewMode.ONLINE,
                        null, null, null, null, InterviewOutcome.SCHEDULED)
        );
    }

    @Test
    @DisplayName("3. Scheduling with null interview date throws ValidationException")
    void testScheduleInterviewNullDate() {
        assertThrows(ValidationException.class, () ->
                service.scheduleInterview(10, USER_1, "Technical Round", null,
                        InterviewMode.ONLINE, null, null, null, null, InterviewOutcome.SCHEDULED)
        );
    }

    @Test
    @DisplayName("4. Scheduling for a non-existent application throws ValidationException")
    void testScheduleInterviewNonExistentApp() {
        LocalDateTime date = LocalDateTime.now().plusDays(3);
        assertThrows(ValidationException.class, () ->
                service.scheduleInterview(999, USER_1, "Round 1", date,
                        InterviewMode.ONLINE, null, null, null, null, InterviewOutcome.SCHEDULED)
        );
    }

    @Test
    @DisplayName("5. Cross-tenant check: User 2 cannot schedule an interview for User 1's application")
    void testScheduleInterviewCrossTenantSecurity() {
        LocalDateTime date = LocalDateTime.now().plusDays(3);
        ValidationException ex = assertThrows(ValidationException.class, () ->
                service.scheduleInterview(10, USER_2, "Hacking Round", date,
                        InterviewMode.ONLINE, null, null, null, null, InterviewOutcome.SCHEDULED)
        );
        assertTrue(ex.getMessage().contains("does not belong to the authenticated user"));
    }

    @Test
    @DisplayName("6. Scheduling with an invalid meeting URL throws ValidationException")
    void testScheduleInterviewInvalidMeetingUrl() {
        LocalDateTime date = LocalDateTime.now().plusDays(3);
        assertThrows(ValidationException.class, () ->
                service.scheduleInterview(10, USER_1, "Round 1", date,
                        InterviewMode.ONLINE, null, "invalid-url-string", null, null, InterviewOutcome.SCHEDULED)
        );
    }

    @Test
    @DisplayName("7. Scheduling with a valid meeting URL succeeds")
    void testScheduleInterviewValidMeetingUrl() throws ValidationException, SQLException {
        LocalDateTime date = LocalDateTime.now().plusDays(3);
        Interview interview = service.scheduleInterview(
                10, USER_1, "Coding Assessment", date,
                InterviewMode.ONLINE, null, "https://zoom.us/j/123456789", null, null, InterviewOutcome.SCHEDULED
        );
        assertNotNull(interview);
        assertEquals("https://zoom.us/j/123456789", interview.getMeetingLink());
    }

    @Test
    @DisplayName("8. Updating an interview with valid data succeeds")
    void testUpdateInterviewSuccess() throws ValidationException, SQLException {
        LocalDateTime newDate = LocalDateTime.now().plusDays(7);
        Interview updated = service.updateInterview(
                100, USER_1, "Technical Round 1 (Rescheduled)", newDate,
                InterviewMode.ONLINE, "Remote", "https://meet.google.com/new-room",
                "Senior Architect", "Passed coding phase", InterviewOutcome.PASSED
        );

        assertNotNull(updated);
        assertEquals("Technical Round 1 (Rescheduled)", updated.getRoundName());
        assertEquals(InterviewOutcome.PASSED, updated.getOutcome());
        assertEquals("Passed coding phase", updated.getFeedback());
    }

    @Test
    @DisplayName("9. Updating a non-existent interview throws ValidationException")
    void testUpdateInterviewNonExistent() {
        LocalDateTime date = LocalDateTime.now().plusDays(2);
        assertThrows(ValidationException.class, () ->
                service.updateInterview(9999, USER_1, "Fake Round", date,
                        InterviewMode.ONLINE, null, null, null, null, InterviewOutcome.SCHEDULED)
        );
    }

    @Test
    @DisplayName("10. Cross-tenant check: User 2 cannot update User 1's interview")
    void testUpdateInterviewCrossTenantSecurity() {
        LocalDateTime date = LocalDateTime.now().plusDays(2);
        ValidationException ex = assertThrows(ValidationException.class, () ->
                service.updateInterview(100, USER_2, "Malicious Update", date,
                        InterviewMode.ONLINE, null, null, null, null, InterviewOutcome.SCHEDULED)
        );
        assertTrue(ex.getMessage().contains("does not belong to this user"));
    }

    @Test
    @DisplayName("11. Deleting an interview owned by the user succeeds")
    void testDeleteInterviewSuccess() throws ValidationException, SQLException {
        boolean deleted = service.deleteInterview(100, USER_1);
        assertTrue(deleted);

        Optional<Interview> check = service.getInterviewById(100, USER_1);
        assertTrue(check.isEmpty());
    }

    @Test
    @DisplayName("12. Cross-tenant check: User 2 cannot delete User 1's interview")
    void testDeleteInterviewCrossTenantSecurity() {
        ValidationException ex = assertThrows(ValidationException.class, () ->
                service.deleteInterview(100, USER_2)
        );
        assertTrue(ex.getMessage().contains("does not belong to this user"));
    }

    @Test
    @DisplayName("13. Query upcoming interviews returns only future scheduled rounds")
    void testGetUpcomingInterviews() throws ValidationException, SQLException {
        // Add past interview
        Interview past = new Interview(101, 10, "Past OA",
                LocalDateTime.now().minusDays(3), InterviewMode.ONLINE, null, null,
                null, "Done", InterviewOutcome.PASSED, LocalDateTime.now(), LocalDateTime.now());
        past.setApplication(appUser1);
        interviewRepo.save(past);

        List<Interview> upcoming = service.getUpcomingInterviews(USER_1);
        assertEquals(1, upcoming.size());
        assertEquals(100, upcoming.get(0).getId());
    }

    @Test
    @DisplayName("14. Query completed interviews returns past or non-scheduled rounds")
    void testGetCompletedInterviews() throws SQLException {
        Interview completed = new Interview(102, 10, "HR Round",
                LocalDateTime.now().minusDays(5), InterviewMode.ONLINE, null, null,
                null, "Cleared", InterviewOutcome.PASSED, LocalDateTime.now(), LocalDateTime.now());
        completed.setApplication(appUser1);
        interviewRepo.save(completed);

        List<Interview> done = service.getCompletedInterviews(USER_1);
        assertEquals(1, done.size());
        assertEquals(102, done.get(0).getId());
    }

    @Test
    @DisplayName("15. Search interviews matches across round, interviewer, feedback, company, and role")
    void testSearchInterviewsKeyword() {
        List<Interview> resRound = service.searchInterviews(USER_1, "Technical");
        assertEquals(1, resRound.size());

        List<Interview> resInterviewer = service.searchInterviews(USER_1, "Sarah");
        assertEquals(1, resInterviewer.size());

        List<Interview> resCompany = service.searchInterviews(USER_1, "Google");
        assertEquals(1, resCompany.size());

        List<Interview> resRole = service.searchInterviews(USER_1, "Software");
        assertEquals(1, resRole.size());

        List<Interview> resNone = service.searchInterviews(USER_1, "NonExistentTerm");
        assertTrue(resNone.isEmpty());
    }

    @Test
    @DisplayName("16. Filtered interviews supports multi-parameter filtering")
    void testFilterInterviewsWithFilters() {
        List<Interview> results = service.getFilteredInterviews(
                USER_1, "Google", "Scheduled", "Upcoming", "date", true
        );
        assertEquals(1, results.size());

        List<Interview> emptyRes = service.getFilteredInterviews(
                USER_1, "Google", "Failed", "Upcoming", "date", true
        );
        assertTrue(emptyRes.isEmpty());
    }

    @Test
    @DisplayName("17. Accurate interview counts for dashboard and pipeline badges")
    void testCountInterviewsAndUpcoming() {
        assertEquals(1, service.getInterviewCount(USER_1));
        assertEquals(1, service.getUpcomingInterviewCount(USER_1));

        assertEquals(0, service.getInterviewCount(USER_2));
        assertEquals(0, service.getUpcomingInterviewCount(USER_2));
    }

    // -------------------------------------------------------------
    // In-Memory Stubs for Unit Testing
    // -------------------------------------------------------------

    private static class InMemoryInterviewRepository implements InterviewRepository {
        private final List<Interview> storage = new ArrayList<>();
        private int seq = 200;

        @Override
        public Interview save(Interview interview) {
            if (interview.getId() <= 0) {
                interview.setId(++seq);
            }
            // Remove existing if any
            storage.removeIf(i -> i.getId() == interview.getId());
            storage.add(interview);
            return interview;
        }

        @Override
        public Optional<Interview> findById(int interviewId, int userId) {
            return storage.stream()
                    .filter(i -> i.getId() == interviewId && i.getApplication() != null && i.getApplication().getUserId() == userId)
                    .findFirst();
        }

        @Override
        public List<Interview> findByUserId(int userId) {
            return storage.stream()
                    .filter(i -> i.getApplication() != null && i.getApplication().getUserId() == userId)
                    .sorted((a, b) -> b.getInterviewDate().compareTo(a.getInterviewDate()))
                    .toList();
        }

        @Override
        public List<Interview> findByApplicationId(int applicationId, int userId) {
            return storage.stream()
                    .filter(i -> i.getApplicationId() == applicationId && i.getApplication() != null && i.getApplication().getUserId() == userId)
                    .sorted((a, b) -> a.getInterviewDate().compareTo(b.getInterviewDate()))
                    .toList();
        }

        @Override
        public boolean update(Interview interview, int userId) {
            for (int i = 0; i < storage.size(); i++) {
                Interview existing = storage.get(i);
                if (existing.getId() == interview.getId() && existing.getApplication() != null && existing.getApplication().getUserId() == userId) {
                    storage.set(i, interview);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean delete(int interviewId, int userId) {
            return storage.removeIf(i -> i.getId() == interviewId && i.getApplication() != null && i.getApplication().getUserId() == userId);
        }

        @Override
        public List<Interview> searchByKeyword(int userId, String keyword) {
            return storage.stream()
                    .filter(i -> i.getApplication() != null && i.getApplication().getUserId() == userId)
                    .filter(i -> i.matches(keyword))
                    .toList();
        }

        @Override
        public List<Interview> filterByOutcome(int userId, InterviewOutcome outcome) {
            return storage.stream()
                    .filter(i -> i.getApplication() != null && i.getApplication().getUserId() == userId)
                    .filter(i -> i.getOutcome() == outcome)
                    .toList();
        }

        @Override
        public List<Interview> findUpcoming(int userId) {
            LocalDateTime now = LocalDateTime.now();
            return storage.stream()
                    .filter(i -> i.getApplication() != null && i.getApplication().getUserId() == userId)
                    .filter(i -> i.getInterviewDate().isAfter(now) && i.getOutcome() == InterviewOutcome.SCHEDULED)
                    .sorted((a, b) -> a.getInterviewDate().compareTo(b.getInterviewDate()))
                    .toList();
        }

        @Override
        public List<Interview> findCompleted(int userId) {
            LocalDateTime now = LocalDateTime.now();
            return storage.stream()
                    .filter(i -> i.getApplication() != null && i.getApplication().getUserId() == userId)
                    .filter(i -> i.getInterviewDate().isBefore(now) || i.getOutcome() != InterviewOutcome.SCHEDULED)
                    .sorted((a, b) -> b.getInterviewDate().compareTo(a.getInterviewDate()))
                    .toList();
        }

        @Override
        public List<Interview> findWithFilters(int userId, String searchKeyword, String outcomeFilter,
                                              String timelineFilter, String sortBy, boolean ascending) {
            LocalDateTime now = LocalDateTime.now();
            return storage.stream()
                    .filter(i -> i.getApplication() != null && i.getApplication().getUserId() == userId)
                    .filter(i -> searchKeyword == null || searchKeyword.isBlank() || i.matches(searchKeyword))
                    .filter(i -> {
                        if (outcomeFilter == null || outcomeFilter.equalsIgnoreCase("All Outcomes") || outcomeFilter.isBlank()) return true;
                        return i.getOutcome().getDisplayName().equalsIgnoreCase(outcomeFilter);
                    })
                    .filter(i -> {
                        if (timelineFilter == null || timelineFilter.equalsIgnoreCase("All Interviews") || timelineFilter.isBlank()) return true;
                        if ("Upcoming Interviews".equalsIgnoreCase(timelineFilter) || "Upcoming".equalsIgnoreCase(timelineFilter)) {
                            return i.getInterviewDate().isAfter(now) && i.getOutcome() == InterviewOutcome.SCHEDULED;
                        }
                        if ("Completed / Past".equalsIgnoreCase(timelineFilter) || "Completed".equalsIgnoreCase(timelineFilter)) {
                            return i.getInterviewDate().isBefore(now) || i.getOutcome() != InterviewOutcome.SCHEDULED;
                        }
                        return true;
                    })
                    .toList();
        }

        @Override
        public int countInterviewsByUserId(int userId) {
            return (int) storage.stream()
                    .filter(i -> i.getApplication() != null && i.getApplication().getUserId() == userId)
                    .count();
        }

        @Override
        public int countUpcomingByUserId(int userId) {
            LocalDateTime now = LocalDateTime.now();
            return (int) storage.stream()
                    .filter(i -> i.getApplication() != null && i.getApplication().getUserId() == userId)
                    .filter(i -> i.getInterviewDate().isAfter(now) && i.getOutcome() == InterviewOutcome.SCHEDULED)
                    .count();
        }
    }

    private static class InMemoryApplicationRepository implements ApplicationRepository {
        private final List<Application> storage = new ArrayList<>();
        private int seq = 100;

        @Override
        public Application save(Application application) {
            if (application.getId() <= 0) {
                application.setId(++seq);
            }
            storage.removeIf(a -> a.getId() == application.getId());
            storage.add(application);
            return application;
        }

        @Override
        public Optional<Application> findById(int applicationId, int userId) {
            return storage.stream()
                    .filter(a -> a.getId() == applicationId && a.getUserId() == userId)
                    .findFirst();
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
        public List<Application> findWithFilters(int userId, String searchKeyword, ApplicationStatus statusFilter,
                                                JobType jobTypeFilter, String sortBy, boolean ascending) {
            return Collections.emptyList();
        }
    }
}

