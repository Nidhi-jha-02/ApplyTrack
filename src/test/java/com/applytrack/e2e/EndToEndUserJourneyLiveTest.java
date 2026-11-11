package com.applytrack.e2e;

import com.applytrack.database.DatabaseConnection;
import com.applytrack.exception.ValidationException;
import com.applytrack.model.Application;
import com.applytrack.model.Company;
import com.applytrack.model.Interview;
import com.applytrack.model.Recruiter;
import com.applytrack.model.User;
import com.applytrack.model.UserPreferences;
import com.applytrack.model.dto.AnalyticsSummary;
import com.applytrack.model.dto.DashboardSummary;
import com.applytrack.model.dto.ReportFilter;
import com.applytrack.model.dto.ReportPayload;
import com.applytrack.model.enums.AppTheme;
import com.applytrack.model.enums.ApplicationStatus;
import com.applytrack.model.enums.DateRangeFilter;
import com.applytrack.model.enums.InterviewMode;
import com.applytrack.model.enums.InterviewOutcome;
import com.applytrack.model.enums.JobType;
import com.applytrack.model.enums.ReportType;
import com.applytrack.repository.CompanyRepository;
import com.applytrack.repository.impl.JdbcCompanyRepository;
import com.applytrack.service.AnalyticsService;
import com.applytrack.service.ApplicationService;
import com.applytrack.service.AuthService;
import com.applytrack.service.DashboardService;
import com.applytrack.service.InterviewService;
import com.applytrack.service.RecruiterService;
import com.applytrack.service.ReportService;
import com.applytrack.service.SettingsService;
import com.applytrack.service.export.ExcelExportService;
import com.applytrack.service.export.PdfExportService;
import com.applytrack.session.UserSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * End-to-End User Journey and Multi-Tenant Isolation Integration Test (Phase 12).
 *
 * Executes the complete 20-step lifecycle of a new candidate:
 * 1. Register candidate User A
 * 2. Log in with credentials
 * 3. Verify empty initial dashboard
 * 4. Create target company & recruiter
 * 5. Create job application
 * 6. Update/Edit application status
 * 7. Schedule multiple interview rounds
 * 8. Verify synchronized Dashboard metrics
 * 9. Verify Career Analytics and funnel conversion rates
 * 10. Generate PDF export document via PDFBox
 * 11. Generate Excel (.xlsx) workbook via POI
 * 12. Update candidate profile & preferences
 * 13. Register and switch to candidate User B
 * 14. Verify strict multi-tenant data isolation
 * 15. Attempt cross-tenant mutation & assert security rejection
 * 16. Log back in as User A & verify full data fidelity
 * 17. Clean up test records
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EndToEndUserJourneyLiveTest {

    private static final String USER_A_EMAIL = "alice.e2e.2026@applytrack.test";
    private static final String USER_A_PASSWORD = "PasswordA123!";
    private static final String USER_B_EMAIL = "bob.e2e.2026@applytrack.test";
    private static final String USER_B_PASSWORD = "PasswordB123!";

    private static AuthService authService;
    private static ApplicationService applicationService;
    private static InterviewService interviewService;
    private static RecruiterService recruiterService;
    private static DashboardService dashboardService;
    private static AnalyticsService analyticsService;
    private static ReportService reportService;
    private static SettingsService settingsService;
    private static CompanyRepository companyRepository;

    private static User userA;
    private static User userB;
    private static Company testCompany;
    private static Recruiter testRecruiter;
    private static Application testApplication;
    private static Interview testInterviewRound1;
    private static Interview testInterviewRound2;

    @BeforeAll
    static void checkDatabaseAndInitializeServices() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            assumeTrue(conn != null && !conn.isClosed(), "MariaDB instance must be running for live E2E test.");

            authService = new AuthService();
            applicationService = new ApplicationService();
            interviewService = new InterviewService();
            recruiterService = new RecruiterService();
            dashboardService = new DashboardService();
            analyticsService = new AnalyticsService();
            reportService = new ReportService();
            settingsService = new SettingsService();
            companyRepository = new JdbcCompanyRepository();

            // Pre-clean any leftover test records from previous aborted runs
            cleanTestRecords();

        } catch (SQLException e) {
            assumeTrue(false, "Skipping E2E test: MariaDB connection unavailable (" + e.getMessage() + ")");
        }
    }

    private static void cleanTestRecords() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM users WHERE email IN (?, ?)")) {
                stmt.setString(1, USER_A_EMAIL);
                stmt.setString(2, USER_B_EMAIL);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM companies WHERE company_name = 'Acme Global E2E'")) {
                stmt.executeUpdate();
            }
            conn.commit();
        } catch (SQLException ignored) {
        }
    }

    @AfterAll
    static void tearDown() {
        cleanTestRecords();
        UserSession.getInstance().clearSession();
    }

    @Test
    @Order(1)
    @DisplayName("Step 1 & 2: Register User A and authenticate via AuthService")
    void testRegisterAndLoginUserA() throws Exception {
        userA = authService.register("Alice Mercer", USER_A_EMAIL, USER_A_PASSWORD, USER_A_PASSWORD);
        assertNotNull(userA, "Registered User A must not be null");
        assertTrue(userA.getId() > 0, "Registered User A must have an auto-generated positive ID");

        // Verify login
        User loggedIn = authService.login(USER_A_EMAIL, USER_A_PASSWORD);
        assertNotNull(loggedIn, "Login with valid credentials must succeed");
        assertEquals(userA.getId(), loggedIn.getId(), "User IDs must match");

        // Establish session
        UserSession.getInstance().setCurrentUser(loggedIn);
        assertTrue(UserSession.getInstance().isAuthenticated(), "UserSession must be authenticated");
        assertEquals(userA.getId(), UserSession.getInstance().getUserId(), "Session user ID must match User A");
    }

    @Test
    @Order(2)
    @DisplayName("Step 3 & 4: Initial Dashboard is clean (0 counts) and create Company + Recruiter")
    void testInitialDashboardAndCreateCompanyRecruiter() throws Exception {
        DashboardSummary initialSummary = dashboardService.getDashboardSummary(userA.getId());
        assertNotNull(initialSummary);
        assertEquals(0, initialSummary.getTotalApplications(), "New candidate must start with 0 applications");
        assertEquals(0, initialSummary.getScheduledInterviews(), "New candidate must start with 0 interviews");

        // Register Company
        testCompany = companyRepository.save(new Company("Acme Global E2E", "https://acme-e2e.test", "Software", "San Francisco, CA"));
        assertNotNull(testCompany);
        assertTrue(testCompany.getId() > 0);

        // Pre-create an application so the company is accessible in pipeline for recruiter creation
        testApplication = applicationService.createApplication(
                userA.getId(),
                "Acme Global E2E",
                "Senior Cloud Architect",
                JobType.FULL_TIME,
                LocalDate.now(),
                LocalDate.now().plusDays(14),
                new BigDecimal("160000.00"),
                "https://acme-e2e.test/jobs/101",
                "Lead distributed systems development.",
                ApplicationStatus.APPLIED,
                "E2E test application notes.",
                null
        );
        assertNotNull(testApplication);
        assertTrue(testApplication.getId() > 0);

        // Register Recruiter associated with Acme Global
        testRecruiter = recruiterService.createRecruiter(
                userA.getId(),
                "Sarah Connor E2E",
                testCompany.getId(),
                "sarah.c@acme-e2e.test",
                "+1-555-0199",
                "https://linkedin.com/in/sarah-e2e",
                "Senior Tech Talent Acquisition."
        );
        assertNotNull(testRecruiter);
        assertTrue(testRecruiter.getId() > 0);
    }

    @Test
    @Order(3)
    @DisplayName("Step 5 & 6: Edit application status to 'Interview Scheduled' and link recruiter")
    void testEditApplicationAndLinkRecruiter() throws Exception {
        Application updated = applicationService.updateApplication(
                testApplication.getId(),
                userA.getId(),
                "Acme Global E2E",
                "Senior Cloud Architect",
                JobType.FULL_TIME,
                testApplication.getApplicationDate(),
                testApplication.getDeadline(),
                new BigDecimal("165000.00"),
                "https://acme-e2e.test/jobs/101",
                "Updated description.",
                ApplicationStatus.INTERVIEW_SCHEDULED,
                "Shortlisted for technical rounds.",
                testRecruiter.getId()
        );

        assertNotNull(updated);
        assertEquals(ApplicationStatus.INTERVIEW_SCHEDULED, updated.getStatus());
        assertEquals(testRecruiter.getId(), updated.getRecruiterId());
        assertEquals(new BigDecimal("165000.00"), updated.getExpectedSalary());
    }

    @Test
    @Order(4)
    @DisplayName("Step 7: Schedule multiple interview rounds for the application")
    void testScheduleMultipleInterviewRounds() throws Exception {
        LocalDateTime round1Time = LocalDateTime.now().plusDays(2);
        testInterviewRound1 = interviewService.scheduleInterview(
                testApplication.getId(),
                userA.getId(),
                "Round 1: System Design",
                round1Time,
                InterviewMode.ONLINE,
                "Virtual",
                "https://meet.google.com/e2e-design",
                "Dave Miller (VP Eng)",
                "Focus on scalability and DB partitioning.",
                InterviewOutcome.PASSED
        );
        assertNotNull(testInterviewRound1);
        assertTrue(testInterviewRound1.getId() > 0);

        LocalDateTime round2Time = LocalDateTime.now().plusDays(5);
        testInterviewRound2 = interviewService.scheduleInterview(
                testApplication.getId(),
                userA.getId(),
                "Round 2: Executive Bar Raiser",
                round2Time,
                InterviewMode.ONLINE,
                "Virtual",
                "https://meet.google.com/e2e-exec",
                "Elena Rostova (CTO)",
                "Leadership principles & team leadership.",
                InterviewOutcome.SCHEDULED
        );
        assertNotNull(testInterviewRound2);
        assertTrue(testInterviewRound2.getId() > 0);

        List<Interview> rounds = interviewService.getInterviewsForApplication(testApplication.getId(), userA.getId());
        assertEquals(2, rounds.size(), "Application should have 2 distinct interview rounds scheduled");
    }

    @Test
    @Order(5)
    @DisplayName("Step 8 & 9: Verify updated Dashboard & Career Analytics metrics")
    void testVerifyUpdatedDashboardAndAnalytics() throws Exception {
        DashboardSummary summary = dashboardService.getDashboardSummary(userA.getId());
        assertEquals(1, summary.getTotalApplications(), "Total applications must be 1");
        assertEquals(1, summary.getInProgressApplications(), "In-progress applications must be 1");
        assertEquals(1, summary.getScheduledInterviews(), "Upcoming scheduled interviews must be 1 (Round 2)");

        AnalyticsSummary analytics = analyticsService.getAnalyticsSummary(userA.getId(), DateRangeFilter.ALL_TIME);
        assertEquals(1, analytics.getTotalApplications());
        assertEquals(1, analytics.getDistinctApplicationsInterviewed());
        assertEquals(100.0, analytics.getInterviewConversionRate(), 0.01, "Conversion rate should be 100%");
        assertEquals(2, analytics.getTotalInterviews(), "Total interview rounds count must be 2");
    }

    @Test
    @Order(6)
    @DisplayName("Step 10 & 11: Generate PDF and Excel reports for User A")
    void testGenerateReports() throws Exception {
        ReportFilter filter = ReportFilter.predefined(DateRangeFilter.ALL_TIME);
        ReportPayload payload = reportService.prepareReportPayload(userA.getId(), userA.getFullName(), userA.getEmail(),
                ReportType.JOB_APPLICATIONS, filter);

        assertNotNull(payload);
        assertEquals(1, payload.getApplications().size());

        // Test PDF generation
        File tempPdf = File.createTempFile("ApplyTrack_E2E_Test_", ".pdf");
        tempPdf.deleteOnExit();
        PdfExportService pdfService = new PdfExportService();
        pdfService.exportToPdf(payload, tempPdf);

        assertTrue(tempPdf.exists(), "PDF file must be generated");
        assertTrue(tempPdf.length() > 500, "PDF file must contain valid PDF bytes (> 500 bytes)");

        // Test Excel generation
        File tempExcel = File.createTempFile("ApplyTrack_E2E_Test_", ".xlsx");
        tempExcel.deleteOnExit();
        ExcelExportService excelService = new ExcelExportService();
        excelService.exportToExcel(payload, tempExcel);

        assertTrue(tempExcel.exists(), "Excel file must be generated");
        assertTrue(tempExcel.length() > 500, "Excel file must contain valid XLSX bytes (> 500 bytes)");
    }

    @Test
    @Order(7)
    @DisplayName("Step 12: Update Profile and Preferences for User A")
    void testUpdateProfileAndPreferences() throws Exception {
        // Update Profile
        settingsService.updateProfile(userA.getId(), "Alice Mercer Updated", USER_A_EMAIL);
        assertEquals("Alice Mercer Updated", UserSession.getInstance().getUserFullName());

        // Update Preferences
        UserPreferences prefs = new UserPreferences(userA.getId());
        prefs.setTheme(AppTheme.DARK);
        prefs.setDefaultLandingPage("APPLICATIONS");
        prefs.setPreferredAnalyticsRange(DateRangeFilter.LAST_30_DAYS);
        prefs.setReminderUpcomingInterviews(true);
        prefs.setReminderApplicationDeadlines(true);
        settingsService.savePreferences(prefs);

        UserPreferences readBack = settingsService.getPreferences(userA.getId());
        assertEquals(AppTheme.DARK, readBack.getTheme(), "Saved theme must be DARK");
        assertEquals("APPLICATIONS", readBack.getDefaultLandingPage(), "Landing page must be APPLICATIONS");
    }

    @Test
    @Order(8)
    @DisplayName("Step 13, 14 & 15: Register User B and verify strict Multi-Tenant Data Isolation")
    void testMultiTenantIsolationUserB() throws Exception {
        // Clear session (Sign out User A)
        UserSession.getInstance().clearSession();
        assertFalse(UserSession.getInstance().isAuthenticated());

        // Register and login User B
        userB = authService.register("Bob Builder", USER_B_EMAIL, USER_B_PASSWORD, USER_B_PASSWORD);
        assertNotNull(userB);
        UserSession.getInstance().setCurrentUser(userB);

        // 1. Verify User B has 0 applications
        List<Application> bApps = applicationService.getApplicationsForUser(userB.getId());
        assertTrue(bApps.isEmpty(), "User B must have 0 applications");

        // 2. Verify User B has 0 interviews
        List<Interview> bInterviews = interviewService.getInterviewsForUser(userB.getId());
        assertTrue(bInterviews.isEmpty(), "User B must have 0 interviews");

        // 3. Verify User B dashboard is completely isolated
        DashboardSummary bSummary = dashboardService.getDashboardSummary(userB.getId());
        assertEquals(0, bSummary.getTotalApplications());
        assertEquals(0, bSummary.getScheduledInterviews());

        // 4. Verify User B cannot delete User A's application
        assertThrows(ValidationException.class, () ->
                applicationService.deleteApplication(testApplication.getId(), userB.getId()),
                "User B must not be permitted to delete User A's application"
        );

        // 5. Verify User B cannot delete User A's interview
        assertThrows(ValidationException.class, () ->
                interviewService.deleteInterview(testInterviewRound1.getId(), userB.getId()),
                "User B must not be permitted to delete User A's interview"
        );

        // 6. Verify User B has default LIGHT theme preferences
        UserPreferences bPrefs = settingsService.getPreferences(userB.getId());
        assertEquals(AppTheme.LIGHT, bPrefs.getTheme(), "User B must have default LIGHT theme, not User A's DARK theme");
    }

    @Test
    @Order(9)
    @DisplayName("Step 16: Log back in as User A & verify complete data fidelity and persistence")
    void testReLoginUserA() throws Exception {
        UserSession.getInstance().clearSession();

        User reLoggedIn = authService.login(USER_A_EMAIL, USER_A_PASSWORD);
        assertNotNull(reLoggedIn);
        UserSession.getInstance().setCurrentUser(reLoggedIn);

        // Verify updated full name
        assertEquals("Alice Mercer Updated", reLoggedIn.getFullName());

        // Verify applications count
        List<Application> aApps = applicationService.getApplicationsForUser(userA.getId());
        assertEquals(1, aApps.size());
        assertEquals("Acme Global E2E", aApps.get(0).getCompanyName());

        // Verify interview rounds count
        List<Interview> aInterviews = interviewService.getInterviewsForUser(userA.getId());
        assertEquals(2, aInterviews.size());

        // Verify preferences preserved
        UserPreferences aPrefs = settingsService.getPreferences(userA.getId());
        assertEquals(AppTheme.DARK, aPrefs.getTheme());
        assertEquals("APPLICATIONS", aPrefs.getDefaultLandingPage());
    }
}
