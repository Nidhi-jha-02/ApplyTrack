package com.applytrack.service.export;

import com.applytrack.model.dto.ApplicationReportItem;
import com.applytrack.model.dto.CompanyApplicationData;
import com.applytrack.model.dto.DateWiseActivityItem;
import com.applytrack.model.dto.InterviewReportItem;
import com.applytrack.model.dto.MonthlyApplicationData;
import com.applytrack.model.dto.RecruiterReportItem;
import com.applytrack.model.dto.ReportPayload;
import com.applytrack.model.dto.StatusDistribution;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Service generating formatted Microsoft Excel (.xlsx) workbooks using Apache POI.
 *
 * Demonstrates:
 * - Apache POI Spreadsheet Model (XSSFWorkbook, Sheet, Row, Cell) (Unit 3)
 * - Java File I/O with FileOutputStream and try-with-resources (Unit 3)
 * - Custom CellStyles, DataFormats, and Auto-Sizing (Unit 3)
 */
public class ExcelExportService {

    /**
     * Exports the provided ReportPayload into a structured Excel workbook (.xlsx).
     */
    public void exportToExcel(ReportPayload payload, File destinationFile) throws IOException {
        if (payload == null) {
            throw new IllegalArgumentException("ReportPayload cannot be null.");
        }
        if (destinationFile == null) {
            throw new IllegalArgumentException("Destination file cannot be null.");
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Styles styles = new Styles(workbook);

            switch (payload.getReportType()) {
                case JOB_APPLICATIONS -> exportApplicationsWorkbook(workbook, payload, styles);
                case INTERVIEW_HISTORY -> exportInterviewsWorkbook(workbook, payload, styles);
                case RECRUITER_DIRECTORY -> exportRecruitersWorkbook(workbook, payload, styles);
                case APPLICATION_STATUS -> exportStatusWorkbook(workbook, payload, styles);
                case DATE_WISE_ACTIVITY -> exportActivityWorkbook(workbook, payload, styles);
                case ANALYTICS_SUMMARY -> exportAnalyticsSummaryWorkbook(workbook, payload, styles);
            }

            try (FileOutputStream out = new FileOutputStream(destinationFile)) {
                workbook.write(out);
            }
        }
    }

    private void exportApplicationsWorkbook(XSSFWorkbook wb, ReportPayload payload, Styles styles) {
        Sheet sheet = wb.createSheet("Applications");
        sheet.createFreezePane(0, 1);

        String[] headers = {"App ID", "Company Name", "Job Role", "Job Type", "Applied Date", "Deadline", "Status", "Expected Salary", "Recruiter", "Notes"};
        createHeaderRow(sheet, headers, styles.headerStyle);

        int rowIdx = 1;
        for (ApplicationReportItem item : payload.getApplications()) {
            Row row = sheet.createRow(rowIdx++);
            CellStyle rowStyle = (rowIdx % 2 == 0) ? styles.altStyle : styles.regularStyle;

            createCell(row, 0, item.getApplicationId(), styles.intStyle);
            createCell(row, 1, item.getCompanyName(), rowStyle);
            createCell(row, 2, item.getJobRole(), rowStyle);
            createCell(row, 3, item.getJobType(), rowStyle);
            createCell(row, 4, item.getFormattedApplicationDate(), rowStyle);
            createCell(row, 5, item.getFormattedDeadline(), rowStyle);
            createCell(row, 6, item.getStatus(), rowStyle);

            Cell salaryCell = row.createCell(7);
            if (item.getSalary() != null) {
                salaryCell.setCellValue(item.getSalary().doubleValue());
                salaryCell.setCellStyle(styles.currencyStyle);
            } else {
                salaryCell.setCellValue("—");
                salaryCell.setCellStyle(rowStyle);
            }

            createCell(row, 8, item.getRecruiterName(), rowStyle);
            createCell(row, 9, item.getNotes(), rowStyle);
        }

        autoSizeColumns(sheet, headers.length);
        addSummarySheetIfPresent(wb, payload, styles);
    }

    private void exportInterviewsWorkbook(XSSFWorkbook wb, ReportPayload payload, Styles styles) {
        Sheet sheet = wb.createSheet("Interviews");
        sheet.createFreezePane(0, 1);

        String[] headers = {"Interview ID", "Company", "Job Role", "Round", "Date & Time", "Mode", "Outcome", "Interviewer", "Meeting / Location", "Feedback"};
        createHeaderRow(sheet, headers, styles.headerStyle);

        int rowIdx = 1;
        for (InterviewReportItem item : payload.getInterviews()) {
            Row row = sheet.createRow(rowIdx++);
            CellStyle rowStyle = (rowIdx % 2 == 0) ? styles.altStyle : styles.regularStyle;

            createCell(row, 0, item.getInterviewId(), styles.intStyle);
            createCell(row, 1, item.getCompanyName(), rowStyle);
            createCell(row, 2, item.getJobRole(), rowStyle);
            createCell(row, 3, item.getRoundName(), rowStyle);
            createCell(row, 4, item.getFormattedDateTime(), rowStyle);
            createCell(row, 5, item.getInterviewMode(), rowStyle);
            createCell(row, 6, item.getOutcome(), rowStyle);
            createCell(row, 7, item.getInterviewerName(), rowStyle);
            createCell(row, 8, !item.getMeetingLink().isBlank() ? item.getMeetingLink() : item.getInterviewLocation(), rowStyle);
            createCell(row, 9, item.getFeedback(), rowStyle);
        }

        autoSizeColumns(sheet, headers.length);
        addSummarySheetIfPresent(wb, payload, styles);
    }

    private void exportRecruitersWorkbook(XSSFWorkbook wb, ReportPayload payload, Styles styles) {
        Sheet sheet = wb.createSheet("Recruiters");
        sheet.createFreezePane(0, 1);

        String[] headers = {"Recruiter ID", "Recruiter Name", "Company", "Email", "Phone", "LinkedIn Profile", "Associated Apps", "Notes"};
        createHeaderRow(sheet, headers, styles.headerStyle);

        int rowIdx = 1;
        for (RecruiterReportItem item : payload.getRecruiters()) {
            Row row = sheet.createRow(rowIdx++);
            CellStyle rowStyle = (rowIdx % 2 == 0) ? styles.altStyle : styles.regularStyle;

            createCell(row, 0, item.getRecruiterId(), styles.intStyle);
            createCell(row, 1, item.getRecruiterName(), rowStyle);
            createCell(row, 2, item.getCompanyName(), rowStyle);
            createCell(row, 3, item.getEmail(), rowStyle);
            createCell(row, 4, item.getPhone(), rowStyle);
            createCell(row, 5, item.getLinkedinUrl(), rowStyle);
            createCell(row, 6, item.getAssociatedAppsCount(), styles.intStyle);
            createCell(row, 7, item.getNotes(), rowStyle);
        }

        autoSizeColumns(sheet, headers.length);
        addSummarySheetIfPresent(wb, payload, styles);
    }

    private void exportStatusWorkbook(XSSFWorkbook wb, ReportPayload payload, Styles styles) {
        Sheet sheet = wb.createSheet("Status Breakdown");
        sheet.createFreezePane(0, 1);

        String[] headers = {"Application Status", "Count", "Percentage of Total", "Pipeline Category"};
        createHeaderRow(sheet, headers, styles.headerStyle);

        int rowIdx = 1;
        for (StatusDistribution item : payload.getStatusDistributions()) {
            Row row = sheet.createRow(rowIdx++);
            CellStyle rowStyle = (rowIdx % 2 == 0) ? styles.altStyle : styles.regularStyle;

            createCell(row, 0, item.getStatusName(), rowStyle);
            createCell(row, 1, item.getCount(), styles.intStyle);

            Cell pctCell = row.createCell(2);
            pctCell.setCellValue(item.getPercentage() / 100.0);
            pctCell.setCellStyle(styles.percentStyle);

            String category = switch (item.getStatusName()) {
                case "Selected", "Rejected", "Withdrawn", "No Response" -> "Terminal Outcome";
                default -> "Active In-Progress";
            };
            createCell(row, 3, category, rowStyle);
        }

        autoSizeColumns(sheet, headers.length);
        addSummarySheetIfPresent(wb, payload, styles);
    }

    private void exportActivityWorkbook(XSSFWorkbook wb, ReportPayload payload, Styles styles) {
        Sheet sheet = wb.createSheet("Activity Log");
        sheet.createFreezePane(0, 1);

        String[] headers = {"Activity Date", "Event Type", "Company", "Details", "Status / Outcome"};
        createHeaderRow(sheet, headers, styles.headerStyle);

        int rowIdx = 1;
        for (DateWiseActivityItem item : payload.getActivities()) {
            Row row = sheet.createRow(rowIdx++);
            CellStyle rowStyle = (rowIdx % 2 == 0) ? styles.altStyle : styles.regularStyle;

            createCell(row, 0, item.getFormattedDate(), rowStyle);
            createCell(row, 1, item.getActivityType(), rowStyle);
            createCell(row, 2, item.getCompanyName(), rowStyle);
            createCell(row, 3, item.getDetails(), rowStyle);
            createCell(row, 4, item.getStatusOrOutcome(), rowStyle);
        }

        autoSizeColumns(sheet, headers.length);
        addSummarySheetIfPresent(wb, payload, styles);
    }

    private void exportAnalyticsSummaryWorkbook(XSSFWorkbook wb, ReportPayload payload, Styles styles) {
        // Multi-Sheet Workbook: Summary, Status Distribution, Top Companies, Monthly Trends
        addSummarySheetIfPresent(wb, payload, styles);

        // Sheet 2: Status Breakdown
        Sheet statusSheet = wb.createSheet("Status Distribution");
        statusSheet.createFreezePane(0, 1);
        String[] sHeaders = {"Status", "Applications Count", "Percentage"};
        createHeaderRow(statusSheet, sHeaders, styles.headerStyle);
        int r = 1;
        for (StatusDistribution item : payload.getStatusDistributions()) {
            Row row = statusSheet.createRow(r++);
            createCell(row, 0, item.getStatusName(), styles.regularStyle);
            createCell(row, 1, item.getCount(), styles.intStyle);
            Cell c = row.createCell(2);
            c.setCellValue(item.getPercentage() / 100.0);
            c.setCellStyle(styles.percentStyle);
        }
        autoSizeColumns(statusSheet, sHeaders.length);

        // Sheet 3: Applications Log
        if (!payload.getApplications().isEmpty()) {
            Sheet appSheet = wb.createSheet("Applications Log");
            appSheet.createFreezePane(0, 1);
            String[] aHeaders = {"Company", "Role", "Date", "Status", "Salary"};
            createHeaderRow(appSheet, aHeaders, styles.headerStyle);
            int ar = 1;
            for (ApplicationReportItem item : payload.getApplications()) {
                Row row = appSheet.createRow(ar++);
                createCell(row, 0, item.getCompanyName(), styles.regularStyle);
                createCell(row, 1, item.getJobRole(), styles.regularStyle);
                createCell(row, 2, item.getFormattedApplicationDate(), styles.regularStyle);
                createCell(row, 3, item.getStatus(), styles.regularStyle);
                Cell sc = row.createCell(4);
                if (item.getSalary() != null) {
                    sc.setCellValue(item.getSalary().doubleValue());
                    sc.setCellStyle(styles.currencyStyle);
                } else {
                    sc.setCellValue("—");
                    sc.setCellStyle(styles.regularStyle);
                }
            }
            autoSizeColumns(appSheet, aHeaders.length);
        }
    }

    private void addSummarySheetIfPresent(XSSFWorkbook wb, ReportPayload payload, Styles styles) {
        if (payload.getSummaryKpis() == null || payload.getSummaryKpis().isEmpty()) {
            return;
        }

        Sheet sumSheet = wb.createSheet("Executive Overview");
        wb.setSheetOrder("Executive Overview", 0); // Put as first tab
        sumSheet.createFreezePane(0, 1);

        String[] headers = {"Metric / KPI", "Value"};
        createHeaderRow(sumSheet, headers, styles.headerStyle);

        int r = 1;
        // Metadata
        Row rMeta1 = sumSheet.createRow(r++);
        createCell(rMeta1, 0, "Candidate Name", styles.boldStyle);
        createCell(rMeta1, 1, payload.getCandidateName(), styles.regularStyle);

        Row rMeta2 = sumSheet.createRow(r++);
        createCell(rMeta2, 0, "Report Scope", styles.boldStyle);
        createCell(rMeta2, 1, payload.getFilter().getFormattedRange(), styles.regularStyle);

        Row rMeta3 = sumSheet.createRow(r++);
        createCell(rMeta3, 0, "Generated At", styles.boldStyle);
        createCell(rMeta3, 1, payload.getFormattedGeneratedAt(), styles.regularStyle);

        for (Map.Entry<String, String> entry : payload.getSummaryKpis().entrySet()) {
            Row row = sumSheet.createRow(r++);
            createCell(row, 0, entry.getKey(), styles.regularStyle);
            createCell(row, 1, entry.getValue(), styles.boldStyle);
        }

        autoSizeColumns(sumSheet, 2);
    }

    private void createHeaderRow(Sheet sheet, String[] headers, CellStyle style) {
        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(24);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "—");
        cell.setCellStyle(style);
    }

    private void createCell(Row row, int col, int value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void autoSizeColumns(Sheet sheet, int colCount) {
        for (int i = 0; i < colCount; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            // Minimum width = 12 characters (3072), Maximum width = 45 characters (11520)
            if (width < 3200) {
                sheet.setColumnWidth(i, 3200);
            } else if (width > 12000) {
                sheet.setColumnWidth(i, 12000);
            }
        }
    }

    private static class Styles {
        final CellStyle headerStyle;
        final CellStyle regularStyle;
        final CellStyle altStyle;
        final CellStyle boldStyle;
        final CellStyle currencyStyle;
        final CellStyle percentStyle;
        final CellStyle intStyle;

        Styles(XSSFWorkbook wb) {
            DataFormat df = wb.createDataFormat();

            // Fonts
            XSSFFont headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 10);
            headerFont.setFontName("Segoe UI");

            XSSFFont regularFont = wb.createFont();
            regularFont.setFontHeightInPoints((short) 9.5);
            regularFont.setFontName("Segoe UI");

            XSSFFont boldFont = wb.createFont();
            boldFont.setBold(true);
            boldFont.setFontHeightInPoints((short) 9.5);
            boldFont.setFontName("Segoe UI");

            // Header Style: Deep Slate #0F172A
            headerStyle = wb.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(15, 23, 42), null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.LEFT);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(headerStyle);

            // Regular Style
            regularStyle = wb.createCellStyle();
            regularStyle.setFont(regularFont);
            regularStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(regularStyle);

            // Alternating Row Style: #F8FAFC
            altStyle = wb.createCellStyle();
            altStyle.setFont(regularFont);
            altStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(248, 250, 252), null));
            altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            altStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(altStyle);

            // Bold Style
            boldStyle = wb.createCellStyle();
            boldStyle.setFont(boldFont);
            boldStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(boldStyle);

            // Currency Style
            currencyStyle = wb.createCellStyle();
            currencyStyle.setFont(regularFont);
            currencyStyle.setDataFormat(df.getFormat("$#,##0.00"));
            currencyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(currencyStyle);

            // Percentage Style
            percentStyle = wb.createCellStyle();
            percentStyle.setFont(regularFont);
            percentStyle.setDataFormat(df.getFormat("0.0%"));
            percentStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(percentStyle);

            // Integer Style
            intStyle = wb.createCellStyle();
            intStyle.setFont(regularFont);
            intStyle.setDataFormat(df.getFormat("#,##0"));
            intStyle.setAlignment(HorizontalAlignment.RIGHT);
            intStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(intStyle);
        }

        private void applyBorders(CellStyle style) {
            style.setBorderBottom(BorderStyle.THIN);
            style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setBorderTop(BorderStyle.THIN);
            style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setBorderLeft(BorderStyle.THIN);
            style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setBorderRight(BorderStyle.THIN);
            style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        }
    }
}

