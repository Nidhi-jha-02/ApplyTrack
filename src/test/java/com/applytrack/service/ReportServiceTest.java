package com.applytrack.service;

import com.applytrack.exception.ValidationException;
import com.applytrack.model.dto.ApplicationReportItem;
import com.applytrack.model.dto.CompanyApplicationData;
import com.applytrack.model.dto.DateWiseActivityItem;
import com.applytrack.model.dto.InterviewReportItem;
import com.applytrack.model.dto.MonthlyApplicationData;
import com.applytrack.model.dto.RecruiterReportItem;
import com.applytrack.model.dto.ReportFilter;
import com.applytrack.model.dto.ReportPayload;
import com.applytrack.model.dto.StatusDistribution;
import com.applytrack.model.enums.DateRangeFilter;
import com.applytrack.model.enums.ReportType;
import com.applytrack.repository.AnalyticsRepository;
import com.applytrack.repository.ReportRepository;
import com.applytrack.service.export.ExcelExportService;
import com.applytrack.service.export.PdfExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Unit Tests for ReportService, ReportFilter, and ReportType.
 *
 * Demonstrates:
 * - Mock/Stub Service & Repository testing (Unit 3)
 * - Custom domain validation testing with ValidationException (Unit 3)
 * - Java Enums with custom fields and metadata (Unit 2)
 * - Modern Java Date/Time boundaries (Unit 1)
 */
public class ReportServiceTest {

    private StubReportRepository stubRepo;
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        stubRepo = new StubReportRepository();
        AnalyticsService stubAnalytics = new AnalyticsService(new StubAnalyticsRepository());
        reportService = new ReportService(stubRepo, stubAnalytics, new PdfExportService(), new ExcelExportService());
    }

    @Test
    @DisplayName("ReportType enum verifies 6 distinct report options with display names and prefixes")
    void testReportTypeEnumMetadata() {
        ReportType[] types = ReportType.values();
        assertEquals(6, types.length);

        for (ReportType type : types) {
            assertNotNull(type.getDisplayName());
            assertFalse(type.getDisplayName().isBlank());
            assertNotNull(type.getDescription());
            assertFalse(type.getDescription().isBlank());
            assertNotNull(type.getDefaultFilenamePrefix());
            assertTrue(type.getDefaultFilenamePrefix().startsWith("ApplyTrack_"));
        }
    }

    @Test
    @DisplayName("ReportFilter predefined ranges compute correct effective bounds")
    void testReportFilterPredefined() {
        ReportFilter filterAll = ReportFilter.predefined(DateRangeFilter.ALL_TIME);
        assertFalse(filterAll.isCustomRange());
        assertNull(filterAll.getEffectiveStartDate());
        assertEquals(LocalDate.now(), filterAll.getEffectiveEndDate());
        assertEquals("All Time", filterAll.getFormattedRange());

        ReportFilter filter30 = ReportFilter.predefined(DateRangeFilter.LAST_30_DAYS);
        assertNotNull(filter30.getEffectiveStartDate());
        assertEquals(LocalDate.now().minusDays(30), filter30.getEffectiveStartDate());
        assertEquals("Last 30 Days", filter30.getFormattedRange());
    }

    @Test
    @DisplayName("ReportFilter custom date range validates boundaries properly")
    void testReportFilterCustomValid() throws ValidationException {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);
        ReportFilter filter = ReportFilter.custom(start, end);

        assertTrue(filter.isCustomRange());
        assertEquals(start, filter.getEffectiveStartDate());
        assertEquals(end, filter.getEffectiveEndDate());
        assertTrue(filter.getFormattedRange().contains("2026"));
    }

    @Test
    @DisplayName("ReportFilter throws ValidationException when start date is after end date")
    void testReportFilterCustomInvalidDatesThrowValidationException() {
        LocalDate start = LocalDate.of(2026, 7, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);

        ValidationException ex = assertThrows(ValidationException.class, () -> ReportFilter.custom(start, end));
        assertTrue(ex.getMessage().contains("cannot be after"));
    }

    @Test
    @DisplayName("prepareReportPayload for JOB_APPLICATIONS computes correct counts and KPIs")
    void testPrepareReportPayloadJobApplications() throws SQLException, ValidationException {
        stubRepo.apps.add(new ApplicationReportItem(1, "Google", "SWE", "Full-time",
                LocalDate.now().minusDays(5), null, "Applied", BigDecimal.valueOf(135000), "Sarah Jenkins", null));
        stubRepo.apps.add(new ApplicationReportItem(2, "Amazon", "SDE II", "Full-time",
                LocalDate.now().minusDays(10), null, "Interview Scheduled", BigDecimal.valueOf(150000), "Mark Davis", null));
        stubRepo.apps.add(new ApplicationReportItem(3, "Meta", "Software Engineer", "Full-time",
                LocalDate.now().minusDays(20), null, "Selected", BigDecimal.valueOf(160000), null, null));
        stubRepo.apps.add(new ApplicationReportItem(4, "Netflix", "Data Engineer", "Full-time",
                LocalDate.now().minusDays(25), null, "Rejected", BigDecimal.valueOf(140000), null, null));

        ReportPayload payload = reportService.prepareReportPayload(1, "Alex Mercer", "alex@example.com",
                ReportType.JOB_APPLICATIONS, ReportFilter.predefined(DateRangeFilter.ALL_TIME));

        assertEquals(ReportType.JOB_APPLICATIONS, payload.getReportType());
        assertEquals("Alex Mercer", payload.getCandidateName());
        assertEquals(4, payload.getRecordCount());

        Map<String, String> kpis = payload.getSummaryKpis();
        assertEquals("4", kpis.get("Total Applications"));
        assertEquals("2", kpis.get("Active In-Progress"));
        assertEquals("1", kpis.get("Offers (Selected)"));
        assertEquals("1", kpis.get("Rejected"));
    }

    @Test
    @DisplayName("prepareReportPayload for INTERVIEW_HISTORY computes rounds and outcome KPIs")
    void testPrepareReportPayloadInterviewHistory() throws SQLException, ValidationException {
        stubRepo.interviews.add(new InterviewReportItem(1, "Google", "SWE", "Technical Round 1",
                LocalDateTime.now().minusDays(3), "Online / Video", "Room 402", "https://meet.google.com", "John Smith", "Great coding", "Passed"));
        stubRepo.interviews.add(new InterviewReportItem(2, "Google", "SWE", "System Design",
                LocalDateTime.now().plusDays(2), "Online / Video", "Virtual", "https://meet.google.com", "Emily Blunt", "Pending", "Scheduled"));
        stubRepo.interviews.add(new InterviewReportItem(3, "Stripe", "Backend Eng", "Coding Challenge",
                LocalDateTime.now().minusDays(15), "Take-home", "", "", "Rob Miller", "Needs improvement", "Failed"));

        ReportPayload payload = reportService.prepareReportPayload(1, "Alex Mercer", "alex@example.com",
                ReportType.INTERVIEW_HISTORY, ReportFilter.predefined(DateRangeFilter.ALL_TIME));

        assertEquals(ReportType.INTERVIEW_HISTORY, payload.getReportType());
        assertEquals(3, payload.getRecordCount());

        Map<String, String> kpis = payload.getSummaryKpis();
        assertEquals("3", kpis.get("Total Rounds"));
        assertEquals("1", kpis.get("Passed Rounds"));
        assertEquals("1", kpis.get("Scheduled Upcoming"));
        assertEquals("1", kpis.get("Failed / Other"));
    }

    @Test
    @DisplayName("prepareReportPayload for RECRUITER_DIRECTORY calculates recruiter metrics")
    void testPrepareReportPayloadRecruiterDirectory() throws SQLException, ValidationException {
        stubRepo.recruiters.add(new RecruiterReportItem(1, "Sarah Jenkins", "Google",
                "sarah@google.com", "555-0101", "https://linkedin.com/in/sarah", 2, "Met at campus fair"));
        stubRepo.recruiters.add(new RecruiterReportItem(2, "Mark Davis", "Amazon",
                "mark@amazon.com", "555-0102", "https://linkedin.com/in/mark", 1, "Referred by alum"));

        ReportPayload payload = reportService.prepareReportPayload(1, "Alex Mercer", "alex@example.com",
                ReportType.RECRUITER_DIRECTORY, ReportFilter.predefined(DateRangeFilter.ALL_TIME));

        assertEquals(ReportType.RECRUITER_DIRECTORY, payload.getReportType());
        assertEquals(2, payload.getRecordCount());

        Map<String, String> kpis = payload.getSummaryKpis();
        assertEquals("2", kpis.get("Total Recruiters"));
        assertEquals("2", kpis.get("Companies Represented"));
        assertEquals("3", kpis.get("Total Linked Apps"));
    }

    @Test
    @DisplayName("prepareReportPayload for DATE_WISE_ACTIVITY aggregates events")
    void testPrepareReportPayloadDateWiseActivity() throws SQLException, ValidationException {
        stubRepo.activities.add(new DateWiseActivityItem(LocalDate.now().minusDays(1), "Application Submitted",
                "Google", "Software Engineer", "Applied"));
        stubRepo.activities.add(new DateWiseActivityItem(LocalDate.now().minusDays(2), "Interview Completed",
                "Amazon", "Round 1 - Coding", "Passed"));

        ReportPayload payload = reportService.prepareReportPayload(1, "Alex Mercer", "alex@example.com",
                ReportType.DATE_WISE_ACTIVITY, ReportFilter.predefined(DateRangeFilter.ALL_TIME));

        assertEquals(ReportType.DATE_WISE_ACTIVITY, payload.getReportType());
        assertEquals(2, payload.getRecordCount());

        Map<String, String> kpis = payload.getSummaryKpis();
        assertEquals("2", kpis.get("Total Events"));
        assertEquals("1", kpis.get("Applications Submitted"));
        assertEquals("1", kpis.get("Interview Rounds"));
    }

    @Test
    @DisplayName("prepareReportPayload for APPLICATION_STATUS aggregates distribution")
    void testPrepareReportPayloadApplicationStatus() throws SQLException, ValidationException {
        ReportPayload payload = reportService.prepareReportPayload(1, "Alex Mercer", "alex@example.com",
                ReportType.APPLICATION_STATUS, ReportFilter.predefined(DateRangeFilter.ALL_TIME));

        assertEquals(ReportType.APPLICATION_STATUS, payload.getReportType());
        assertNotNull(payload.getStatusDistributions());
        Map<String, String> kpis = payload.getSummaryKpis();
        assertNotNull(kpis.get("Total Applications"));
    }

    @Test
    @DisplayName("prepareReportPayload for ANALYTICS_SUMMARY compiles executive overview")
    void testPrepareReportPayloadAnalyticsSummary() throws SQLException, ValidationException {
        ReportPayload payload = reportService.prepareReportPayload(1, "Alex Mercer", "alex@example.com",
                ReportType.ANALYTICS_SUMMARY, ReportFilter.predefined(DateRangeFilter.ALL_TIME));

        assertEquals(ReportType.ANALYTICS_SUMMARY, payload.getReportType());
        assertNotNull(payload.getAnalyticsSummary());
        Map<String, String> kpis = payload.getSummaryKpis();
        assertTrue(kpis.containsKey("Interview Conversion"));
        assertTrue(kpis.containsKey("Offer Success Rate"));
    }

    @Test
    @DisplayName("exportReport validates null payload and null destination file")
    void testExportReportValidationErrors(@TempDir Path tempDir) {
        assertThrows(ValidationException.class, () -> reportService.exportReport(null, tempDir.resolve("test.pdf").toFile()));

        ReportPayload payload = new ReportPayload(ReportType.JOB_APPLICATIONS, "Alex", "alex@test.com", new ReportFilter());
        assertThrows(ValidationException.class, () -> reportService.exportReport(payload, null));
    }

    @Test
    @DisplayName("exportReport defaults to PDF when file has unknown extension")
    void testExportReportDefaultsToPdfOnUnknownExtension(@TempDir Path tempDir) throws IOException, ValidationException {
        ReportPayload payload = new ReportPayload(ReportType.JOB_APPLICATIONS, "Alex Mercer", "alex@test.com", new ReportFilter());
        File unknownFile = tempDir.resolve("myreport.custom").toFile();

        reportService.exportReport(payload, unknownFile);

        File expectedPdf = tempDir.resolve("myreport.custom.pdf").toFile();
        assertTrue(expectedPdf.exists(), "Should have generated PDF with corrected extension");
        assertTrue(expectedPdf.length() > 0);
    }

    // --- Stub Implementations for Unit Testing ---

    private static class StubReportRepository implements ReportRepository {
        final List<ApplicationReportItem> apps = new ArrayList<>();
        final List<InterviewReportItem> interviews = new ArrayList<>();
        final List<RecruiterReportItem> recruiters = new ArrayList<>();
        final List<DateWiseActivityItem> activities = new ArrayList<>();

        @Override
        public List<ApplicationReportItem> getApplicationReportData(int userId, LocalDate startDate, LocalDate endDate) {
            return apps;
        }

        @Override
        public List<InterviewReportItem> getInterviewReportData(int userId, LocalDate startDate, LocalDate endDate) {
            return interviews;
        }

        @Override
        public List<RecruiterReportItem> getRecruiterReportData(int userId) {
            return recruiters;
        }

        @Override
        public List<DateWiseActivityItem> getDateWiseActivityData(int userId, LocalDate startDate, LocalDate endDate) {
            return activities;
        }
    }

    private static class StubAnalyticsRepository implements AnalyticsRepository {
        @Override public int countTotalApplications(int userId, LocalDate startDate) { return 5; }
        @Override public int countActiveApplications(int userId, LocalDate startDate) { return 3; }
        @Override public int countOffers(int userId, LocalDate startDate) { return 1; }
        @Override public int countRejected(int userId, LocalDate startDate) { return 1; }
        @Override public int countTotalInterviews(int userId, LocalDate startDate) { return 4; }
        @Override public int countDistinctApplicationsInterviewed(int userId, LocalDate startDate) { return 2; }
        @Override public int countUpcomingInterviews(int userId) { return 1; }
        @Override public int countCompletedInterviews(int userId, LocalDate startDate) { return 3; }

        @Override
        public Map<String, Integer> getStatusCounts(int userId, LocalDate startDate) {
            Map<String, Integer> map = new LinkedHashMap<>();
            map.put("Applied", 2);
            map.put("Under Review", 1);
            map.put("Selected", 1);
            map.put("Rejected", 1);
            return map;
        }

        @Override
        public List<MonthlyApplicationData> getMonthlyApplicationCounts(int userId, LocalDate startDate) {
            return new ArrayList<>();
        }

        @Override
        public List<CompanyApplicationData> getCompanyApplicationCounts(int userId, LocalDate startDate, int limit) {
            return new ArrayList<>();
        }

        @Override
        public Map<String, Integer> getInterviewOutcomeCounts(int userId, LocalDate startDate) {
            return Map.of("Passed", 1, "Scheduled", 1, "Failed", 1);
        }

        @Override
        public List<MonthlyApplicationData> getMonthlyInterviewCounts(int userId, LocalDate startDate) {
            return new ArrayList<>();
        }
    }
}
