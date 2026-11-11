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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for ExcelExportService using Apache POI 5.2.5.
 *
 * Demonstrates:
 * - Real Excel (.xlsx) workbook generation & validation (Unit 3)
 * - Multi-sheet structures, cell inspection, and header verification (Unit 3)
 * - Numeric and currency cell validation (Unit 3)
 * - Empty dataset handling and exception handling (Unit 3)
 */
public class ExcelExportServiceTest {

    private ExcelExportService excelExportService;

    @BeforeEach
    void setUp() {
        excelExportService = new ExcelExportService();
    }

    @Test
    @DisplayName("Excel Export: Job Application Report creates multi-sheet workbook with data rows")
    void testExportJobApplicationsToExcel(@TempDir Path tempDir) throws IOException, ValidationException {
        ReportPayload payload = new ReportPayload(ReportType.JOB_APPLICATIONS, "Alex Mercer", "alex@applytrack.io", new ReportFilter());
        List<ApplicationReportItem> apps = new ArrayList<>();
        apps.add(new ApplicationReportItem(101, "Google", "SWE", "Full-time",
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(15), "Applied", BigDecimal.valueOf(140000), "Sarah", "Referral"));
        apps.add(new ApplicationReportItem(102, "Amazon", "SDE II", "Full-time",
                LocalDate.now().minusDays(12), null, "Selected", BigDecimal.valueOf(160000), "Mark", "Job Board"));
        payload.setApplications(apps);
        payload.addSummaryKpi("Total Applications", "2");

        File targetExcel = tempDir.resolve("applications.xlsx").toFile();
        excelExportService.exportToExcel(payload, targetExcel);

        assertTrue(targetExcel.exists());
        assertTrue(targetExcel.length() > 0);

        try (FileInputStream fis = new FileInputStream(targetExcel);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            assertEquals(2, workbook.getNumberOfSheets());

            Sheet overview = workbook.getSheet("Executive Overview");
            assertNotNull(overview);

            Sheet dataSheet = workbook.getSheet("Applications");
            assertNotNull(dataSheet);

            Row headerRow = dataSheet.getRow(0);
            assertEquals("App ID", headerRow.getCell(0).getStringCellValue());
            assertEquals("Company Name", headerRow.getCell(1).getStringCellValue());
            assertEquals("Job Role", headerRow.getCell(2).getStringCellValue());

            Row row1 = dataSheet.getRow(1);
            assertEquals(101, (int) row1.getCell(0).getNumericCellValue());
            assertEquals("Google", row1.getCell(1).getStringCellValue());
            assertEquals(140000.0, row1.getCell(7).getNumericCellValue(), 0.01);
        }
    }

    @Test
    @DisplayName("Excel Export: Interview History Report creates interview sheet and headers")
    void testExportInterviewHistoryToExcel(@TempDir Path tempDir) throws IOException, ValidationException {
        ReportPayload payload = new ReportPayload(ReportType.INTERVIEW_HISTORY, "Alex Mercer", "alex@applytrack.io", new ReportFilter());
        List<InterviewReportItem> list = List.of(
                new InterviewReportItem(1, "Google", "SWE", "Round 1 - Coding",
                        LocalDateTime.now().minusDays(3), "Online / Video", "Room 201", "https://meet.google.com", "John Smith", "Great coding", "Passed")
        );
        payload.setInterviews(list);
        payload.addSummaryKpi("Total Rounds", "1");

        File targetExcel = tempDir.resolve("interviews.xlsx").toFile();
        excelExportService.exportToExcel(payload, targetExcel);

        try (FileInputStream fis = new FileInputStream(targetExcel);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet("Interviews");
            assertNotNull(sheet);

            Row header = sheet.getRow(0);
            assertEquals("Interview ID", header.getCell(0).getStringCellValue());
            assertEquals("Company", header.getCell(1).getStringCellValue());
            assertEquals("Outcome", header.getCell(6).getStringCellValue());

            Row row1 = sheet.getRow(1);
            assertEquals(1, (int) row1.getCell(0).getNumericCellValue());
            assertEquals("Google", row1.getCell(1).getStringCellValue());
            assertEquals("Passed", row1.getCell(6).getStringCellValue());
        }
    }

    @Test
    @DisplayName("Excel Export: Recruiter Directory Report creates recruiter sheet and headers")
    void testExportRecruiterDirectoryToExcel(@TempDir Path tempDir) throws IOException, ValidationException {
        ReportPayload payload = new ReportPayload(ReportType.RECRUITER_DIRECTORY, "Alex Mercer", "alex@applytrack.io", new ReportFilter());
        List<RecruiterReportItem> recruiters = List.of(
                new RecruiterReportItem(1, "Sarah Jenkins", "Google", "sarah@google.com", "555-0101",
                        "https://linkedin.com/in/sarah", 3, "Campus referral")
        );
        payload.setRecruiters(recruiters);
        payload.addSummaryKpi("Total Recruiters", "1");

        File targetExcel = tempDir.resolve("recruiters.xlsx").toFile();
        excelExportService.exportToExcel(payload, targetExcel);

        try (FileInputStream fis = new FileInputStream(targetExcel);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet("Recruiters");
            assertNotNull(sheet);

            Row header = sheet.getRow(0);
            assertEquals("Recruiter ID", header.getCell(0).getStringCellValue());
            assertEquals("Recruiter Name", header.getCell(1).getStringCellValue());
            assertEquals("Associated Apps", header.getCell(6).getStringCellValue());

            Row row1 = sheet.getRow(1);
            assertEquals(1, (int) row1.getCell(0).getNumericCellValue());
            assertEquals("Sarah Jenkins", row1.getCell(1).getStringCellValue());
            assertEquals(3, (int) row1.getCell(6).getNumericCellValue());
        }
    }

    @Test
    @DisplayName("Excel Export: Application Status Report creates status breakdown sheet")
    void testExportApplicationStatusToExcel(@TempDir Path tempDir) throws IOException, ValidationException {
        ReportPayload payload = new ReportPayload(ReportType.APPLICATION_STATUS, "Alex Mercer", "alex@applytrack.io", new ReportFilter());
        payload.setStatusDistributions(List.of(
                new StatusDistribution("Applied", 4, 66.7),
                new StatusDistribution("Selected", 2, 33.3)
        ));
        payload.addSummaryKpi("Total Applications", "6");

        File targetExcel = tempDir.resolve("status.xlsx").toFile();
        excelExportService.exportToExcel(payload, targetExcel);

        try (FileInputStream fis = new FileInputStream(targetExcel);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet("Status Breakdown");
            assertNotNull(sheet);

            Row row1 = sheet.getRow(1);
            assertEquals("Applied", row1.getCell(0).getStringCellValue());
            assertEquals(4, (int) row1.getCell(1).getNumericCellValue());
        }
    }

    @Test
    @DisplayName("Excel Export: Analytics Summary Report creates multi-tab intelligence workbook")
    void testExportAnalyticsSummaryToExcel(@TempDir Path tempDir) throws IOException, ValidationException {
        ReportPayload payload = new ReportPayload(ReportType.ANALYTICS_SUMMARY, "Alex Mercer", "alex@applytrack.io", new ReportFilter());
        AnalyticsSummary summary = new AnalyticsSummary();
        summary.setTotalApplications(10);
        summary.setActiveApplications(5);
        summary.setTotalInterviews(6);
        summary.setOffersReceived(2);
        summary.setInterviewConversionRate(40.0);
        summary.setOfferRate(20.0);

        payload.setAnalyticsSummary(summary);
        payload.setStatusDistributions(List.of(new StatusDistribution("Applied", 5, 50.0)));
        payload.setApplications(List.of(new ApplicationReportItem(1, "Google", "SWE", "Full-time",
                LocalDate.now(), null, "Applied", BigDecimal.valueOf(130000), null, null)));
        payload.addSummaryKpi("Total Applications", "10");

        File targetExcel = tempDir.resolve("analytics_summary.xlsx").toFile();
        excelExportService.exportToExcel(payload, targetExcel);

        try (FileInputStream fis = new FileInputStream(targetExcel);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            assertTrue(workbook.getNumberOfSheets() >= 2);
            assertNotNull(workbook.getSheet("Executive Overview"));
            assertNotNull(workbook.getSheet("Status Distribution"));
            assertNotNull(workbook.getSheet("Applications Log"));
        }
    }

    @Test
    @DisplayName("Excel Export: Date-wise Activity Report creates chronological activity sheet")
    void testExportDateWiseActivityToExcel(@TempDir Path tempDir) throws IOException, ValidationException {
        ReportPayload payload = new ReportPayload(ReportType.DATE_WISE_ACTIVITY, "Alex Mercer", "alex@applytrack.io", new ReportFilter());
        payload.setActivities(List.of(
                new DateWiseActivityItem(LocalDate.now(), "Application Submitted", "Google", "SWE Role", "Applied"),
                new DateWiseActivityItem(LocalDate.now().minusDays(1), "Interview Completed", "Amazon", "Round 1", "Passed")
        ));
        payload.addSummaryKpi("Total Events", "2");

        File targetExcel = tempDir.resolve("activity.xlsx").toFile();
        excelExportService.exportToExcel(payload, targetExcel);

        try (FileInputStream fis = new FileInputStream(targetExcel);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet("Activity Log");
            assertNotNull(sheet);

            Row header = sheet.getRow(0);
            assertEquals("Activity Date", header.getCell(0).getStringCellValue());
            assertEquals("Event Type", header.getCell(1).getStringCellValue());
            assertEquals("Company", header.getCell(2).getStringCellValue());
            assertEquals("Details", header.getCell(3).getStringCellValue());
            assertEquals("Status / Outcome", header.getCell(4).getStringCellValue());

            Row row1 = sheet.getRow(1);
            assertEquals("Application Submitted", row1.getCell(1).getStringCellValue());
        }
    }

    @Test
    @DisplayName("Excel Export: Empty dataset generates valid workbook without crash")
    void testExportEmptyDatasetExcel(@TempDir Path tempDir) throws IOException, ValidationException {
        ReportPayload payload = new ReportPayload(ReportType.JOB_APPLICATIONS, "Jane Doe", "jane@applytrack.io", new ReportFilter());
        payload.setApplications(List.of());
        payload.addSummaryKpi("Total Records", "0");

        File targetExcel = tempDir.resolve("empty.xlsx").toFile();
        excelExportService.exportToExcel(payload, targetExcel);

        assertTrue(targetExcel.exists());
        try (FileInputStream fis = new FileInputStream(targetExcel);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet("Applications");
            assertNotNull(sheet);
            assertNotNull(sheet.getRow(0));
        }
    }

    @Test
    @DisplayName("Excel Export: Null payload or null destination file throws IllegalArgumentException")
    void testExcelExportNullPayloadThrowsException(@TempDir Path tempDir) {
        assertThrows(IllegalArgumentException.class, () -> excelExportService.exportToExcel(null, tempDir.resolve("dummy.xlsx").toFile()));

        ReportPayload payload = new ReportPayload(ReportType.JOB_APPLICATIONS, "Alex", "alex@test.com", new ReportFilter());
        assertThrows(IllegalArgumentException.class, () -> excelExportService.exportToExcel(payload, null));
    }
}
