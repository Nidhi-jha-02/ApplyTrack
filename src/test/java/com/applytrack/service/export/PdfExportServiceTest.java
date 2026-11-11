package com.applytrack.service.export;

import com.applytrack.exception.ValidationException;
import com.applytrack.model.dto.AnalyticsSummary;
import com.applytrack.model.dto.ApplicationReportItem;
import com.applytrack.model.dto.DateWiseActivityItem;
import com.applytrack.model.dto.InterviewReportItem;
import com.applytrack.model.dto.RecruiterReportItem;
import com.applytrack.model.dto.ReportFilter;
import com.applytrack.model.dto.ReportPayload;
import com.applytrack.model.dto.StatusDistribution;
import com.applytrack.model.enums.ReportType;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for PdfExportService using Apache PDFBox 3.0.
 *
 * Demonstrates:
 * - PDF Document generation and structure validation (Unit 3)
 * - Multi-page pagination and running header verification (Unit 3)
 * - Empty dataset handling without NullPointerExceptions (Unit 3)
 * - File I/O operations and resource cleanups (Unit 3)
 */
public class PdfExportServiceTest {

    private PdfExportService pdfExportService;

    @BeforeEach
    void setUp() {
        pdfExportService = new PdfExportService();
    }

    @Test
    @DisplayName("PDF Export: Job Application Report produces valid multi-record document")
    void testExportJobApplicationsReportToPdf(@TempDir Path tempDir) throws IOException, ValidationException {
        ReportPayload payload = new ReportPayload(ReportType.JOB_APPLICATIONS, "Alex Mercer", "alex@applytrack.io", new ReportFilter());
        List<ApplicationReportItem> apps = new ArrayList<>();
        apps.add(new ApplicationReportItem(1, "Google", "Software Engineer", "Full-time",
                LocalDate.now().minusDays(3), LocalDate.now().plusDays(20), "Applied", BigDecimal.valueOf(140000), "Sarah Jenkins", "Referral"));
        apps.add(new ApplicationReportItem(2, "Amazon", "SDE II", "Full-time",
                LocalDate.now().minusDays(10), null, "Interview Scheduled", BigDecimal.valueOf(155000), "Mark Davis", "Online"));
        payload.setApplications(apps);
        payload.addSummaryKpi("Total Applications", "2");
        payload.addSummaryKpi("Active In-Progress", "2");

        File targetPdf = tempDir.resolve("test_applications.pdf").toFile();
        pdfExportService.exportToPdf(payload, targetPdf);

        assertTrue(targetPdf.exists());
        assertTrue(targetPdf.length() > 0, "PDF file should have non-zero size");

        // Verify PDF integrity with PDFBox Loader
        try (PDDocument doc = Loader.loadPDF(targetPdf)) {
            assertNotNull(doc);
            assertEquals(1, doc.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("PDF Export: Interview History Report produces valid document")
    void testExportInterviewHistoryReportToPdf(@TempDir Path tempDir) throws IOException, ValidationException {
        ReportPayload payload = new ReportPayload(ReportType.INTERVIEW_HISTORY, "Alex Mercer", "alex@applytrack.io", new ReportFilter());
        List<InterviewReportItem> interviews = new ArrayList<>();
        interviews.add(new InterviewReportItem(1, "Google", "SWE", "Technical Screening",
                LocalDateTime.now().minusDays(2), "Online", "Room 101", "https://meet.google.com", "John Doe", "Passed technical round", "Passed"));
        payload.setInterviews(interviews);
        payload.addSummaryKpi("Total Rounds", "1");
        payload.addSummaryKpi("Passed Rounds", "1");

        File targetPdf = tempDir.resolve("test_interviews.pdf").toFile();
        pdfExportService.exportToPdf(payload, targetPdf);

        assertTrue(targetPdf.exists());
        try (PDDocument doc = Loader.loadPDF(targetPdf)) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("PDF Export: Recruiter Directory Report produces valid document")
    void testExportRecruiterDirectoryReportToPdf(@TempDir Path tempDir) throws IOException, ValidationException {
        ReportPayload payload = new ReportPayload(ReportType.RECRUITER_DIRECTORY, "Alex Mercer", "alex@applytrack.io", new ReportFilter());
        List<RecruiterReportItem> recruiters = new ArrayList<>();
        recruiters.add(new RecruiterReportItem(1, "Sarah Jenkins", "Google",
                "sarah@google.com", "555-0199", "https://linkedin.com/in/sarah", 2, "Campus outreach"));
        payload.setRecruiters(recruiters);
        payload.addSummaryKpi("Total Recruiters", "1");

        File targetPdf = tempDir.resolve("test_recruiters.pdf").toFile();
        pdfExportService.exportToPdf(payload, targetPdf);

        assertTrue(targetPdf.exists());
        try (PDDocument doc = Loader.loadPDF(targetPdf)) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("PDF Export: Application Status Report produces valid document")
    void testExportApplicationStatusReportToPdf(@TempDir Path tempDir) throws IOException, ValidationException {
        ReportPayload payload = new ReportPayload(ReportType.APPLICATION_STATUS, "Alex Mercer", "alex@applytrack.io", new ReportFilter());
        List<StatusDistribution> dist = List.of(
                new StatusDistribution("Applied", 3, 50.0),
                new StatusDistribution("Interview Scheduled", 2, 33.3),
                new StatusDistribution("Selected", 1, 16.7)
        );
        payload.setStatusDistributions(dist);
        payload.addSummaryKpi("Total Applications", "6");

        File targetPdf = tempDir.resolve("test_status.pdf").toFile();
        pdfExportService.exportToPdf(payload, targetPdf);

        assertTrue(targetPdf.exists());
        try (PDDocument doc = Loader.loadPDF(targetPdf)) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("PDF Export: Analytics Summary Report produces valid document")
    void testExportAnalyticsSummaryReportToPdf(@TempDir Path tempDir) throws IOException, ValidationException {
        ReportPayload payload = new ReportPayload(ReportType.ANALYTICS_SUMMARY, "Alex Mercer", "alex@applytrack.io", new ReportFilter());
        AnalyticsSummary summary = new AnalyticsSummary();
        summary.setTotalApplications(10);
        summary.setActiveApplications(6);
        summary.setTotalInterviews(7);
        summary.setOffersReceived(2);
        summary.setInterviewConversionRate(40.0);
        summary.setOfferRate(20.0);
        payload.setAnalyticsSummary(summary);
        payload.setStatusDistributions(List.of(new StatusDistribution("Applied", 4, 40.0)));
        payload.addSummaryKpi("Total Applications", "10");
        payload.addSummaryKpi("Conversion Rate", "40.0%");

        File targetPdf = tempDir.resolve("test_analytics.pdf").toFile();
        pdfExportService.exportToPdf(payload, targetPdf);

        assertTrue(targetPdf.exists());
        try (PDDocument doc = Loader.loadPDF(targetPdf)) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("PDF Export: Date-wise Activity Report produces valid document")
    void testExportDateWiseActivityReportToPdf(@TempDir Path tempDir) throws IOException, ValidationException {
        ReportPayload payload = new ReportPayload(ReportType.DATE_WISE_ACTIVITY, "Alex Mercer", "alex@applytrack.io", new ReportFilter());
        List<DateWiseActivityItem> activities = List.of(
                new DateWiseActivityItem(LocalDate.now(), "Application Submitted", "Google", "SWE Role", "Applied"),
                new DateWiseActivityItem(LocalDate.now().minusDays(1), "Interview Scheduled", "Amazon", "Round 1", "Scheduled")
        );
        payload.setActivities(activities);
        payload.addSummaryKpi("Total Events", "2");

        File targetPdf = tempDir.resolve("test_activity.pdf").toFile();
        pdfExportService.exportToPdf(payload, targetPdf);

        assertTrue(targetPdf.exists());
        try (PDDocument doc = Loader.loadPDF(targetPdf)) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("PDF Export: Empty dataset generates document gracefully without errors")
    void testExportEmptyDatasetPdf(@TempDir Path tempDir) throws IOException, ValidationException {
        ReportPayload payload = new ReportPayload(ReportType.JOB_APPLICATIONS, "Jane Doe", "jane@applytrack.io", new ReportFilter());
        payload.setApplications(List.of());
        payload.addSummaryKpi("Total Records", "0");

        File targetPdf = tempDir.resolve("test_empty.pdf").toFile();
        pdfExportService.exportToPdf(payload, targetPdf);

        assertTrue(targetPdf.exists());
        try (PDDocument doc = Loader.loadPDF(targetPdf)) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("PDF Export: Multi-page dataset triggers automatic pagination with page count > 1")
    void testPdfPaginationMultiPage(@TempDir Path tempDir) throws IOException, ValidationException {
        ReportPayload payload = new ReportPayload(ReportType.JOB_APPLICATIONS, "Alex Mercer", "alex@applytrack.io", new ReportFilter());
        List<ApplicationReportItem> largeList = new ArrayList<>();
        // 60 items will exceed a single letter page (~25 rows per page)
        for (int i = 1; i <= 60; i++) {
            largeList.add(new ApplicationReportItem(i, "Company " + i, "Role " + i, "Full-time",
                    LocalDate.now().minusDays(i), null, "Applied", BigDecimal.valueOf(100000 + (i * 1000)), null, null));
        }
        payload.setApplications(largeList);
        payload.addSummaryKpi("Total Applications", "60");

        File targetPdf = tempDir.resolve("test_multipage.pdf").toFile();
        pdfExportService.exportToPdf(payload, targetPdf);

        assertTrue(targetPdf.exists());
        try (PDDocument doc = Loader.loadPDF(targetPdf)) {
            assertTrue(doc.getNumberOfPages() > 1, "60 rows should create multiple pages with pagination");
        }
    }

    @Test
    @DisplayName("PDF Export: Null payload or null destination file throws IllegalArgumentException")
    void testPdfExportNullPayloadThrowsException(@TempDir Path tempDir) {
        assertThrows(IllegalArgumentException.class, () -> pdfExportService.exportToPdf(null, tempDir.resolve("dummy.pdf").toFile()));

        ReportPayload payload = new ReportPayload(ReportType.JOB_APPLICATIONS, "Alex", "alex@test.com", new ReportFilter());
        assertThrows(IllegalArgumentException.class, () -> pdfExportService.exportToPdf(payload, null));
    }
}
