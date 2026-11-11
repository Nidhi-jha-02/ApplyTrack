package com.applytrack.service.export;

import com.applytrack.model.dto.ApplicationReportItem;
import com.applytrack.model.dto.DateWiseActivityItem;
import com.applytrack.model.dto.InterviewReportItem;
import com.applytrack.model.dto.RecruiterReportItem;
import com.applytrack.model.dto.ReportPayload;
import com.applytrack.model.dto.StatusDistribution;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Service generating high-quality, professional PDF reports using Apache PDFBox 3.0.
 *
 * Demonstrates:
 * - Java File I/O and PDF binary stream generation (Unit 3)
 * - Multi-page pagination and dynamic coordinates layout (Unit 1 & Unit 3)
 * - Typography, alignment, and table rendering engine (Unit 2 & Unit 3)
 */
public class PdfExportService {

    private static final float PAGE_MARGIN = 36f;
    private static final float HEADER_HEIGHT = 70f;
    private static final float ROW_HEIGHT = 20f;

    // Palette Colors using java.awt.Color for PDFBox 3.0 compatibility
    private static final Color COLOR_HEADER_BG = new Color(15, 23, 42);      // Deep Slate #0F172A
    private static final Color COLOR_KPI_BG = new Color(241, 245, 249);       // #F1F5F9
    private static final Color COLOR_BORDER = new Color(203, 213, 225);       // #CBD5E1
    private static final Color COLOR_MUTED = new Color(100, 116, 139);        // Slate Gray #64748B
    private static final Color COLOR_DARK = new Color(15, 23, 42);            // #0F172A
    private static final Color COLOR_TEXT = new Color(30, 41, 59);            // Body Slate #1E293B
    private static final Color COLOR_PRIMARY = new Color(79, 70, 229);        // Brand Indigo #4F46E5
    private static final Color COLOR_ROW_ALT = new Color(248, 250, 252);      // #F8FAFC
    private static final Color COLOR_ROW_BORDER = new Color(226, 232, 240);   // #E2E8F0
    private static final Color COLOR_FOOTER = new Color(148, 163, 184);       // #94A3B8

    private final PDType1Font fontRegular;
    private final PDType1Font fontBold;

    public PdfExportService() {
        this.fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        this.fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    }

    /**
     * Renders and exports the provided ReportPayload into a formatted PDF file.
     */
    public void exportToPdf(ReportPayload payload, File destinationFile) throws IOException {
        if (payload == null) {
            throw new IllegalArgumentException("ReportPayload cannot be null.");
        }
        if (destinationFile == null) {
            throw new IllegalArgumentException("Destination file cannot be null.");
        }

        try (PDDocument document = new PDDocument();
             PdfContext context = new PdfContext(document)) {
            context.newPage();

            // 1. Draw Brand Header
            drawHeader(context, payload);

            // 2. Draw KPI Summary Box (if KPIs present)
            if (payload.getSummaryKpis() != null && !payload.getSummaryKpis().isEmpty()) {
                drawSummaryKpis(context, payload.getSummaryKpis());
            }

            // 3. Draw Report-Specific Table
            drawReportTable(context, payload);

            // Close content stream before appending footers or saving
            context.close();

            // 4. Draw Footer & Page Numbers
            drawFooters(document);

            // Save Document
            document.save(destinationFile);
        }
    }

    private void drawHeader(PdfContext ctx, ReportPayload payload) throws IOException {
        float width = ctx.page.getMediaBox().getWidth() - (PAGE_MARGIN * 2);
        float startY = ctx.currentY;

        // Header Background Banner
        ctx.content.setNonStrokingColor(COLOR_HEADER_BG); // Deep Slate #0F172A
        ctx.content.addRect(PAGE_MARGIN, startY - HEADER_HEIGHT, width, HEADER_HEIGHT);
        ctx.content.fill();

        // Brand Title
        ctx.content.setNonStrokingColor(Color.WHITE);
        ctx.content.beginText();
        ctx.content.setFont(fontBold, 15);
        ctx.content.newLineAtOffset(PAGE_MARGIN + 14, startY - 24);
        ctx.content.showText("ApplyTrack — " + payload.getReportType().getDisplayName());
        ctx.content.endText();

        // Subtitle / Scope Details
        ctx.content.beginText();
        ctx.content.setFont(fontRegular, 9.5f);
        ctx.content.newLineAtOffset(PAGE_MARGIN + 14, startY - 40);
        ctx.content.showText("Candidate: " + payload.getCandidateName() + " (" + payload.getCandidateEmail() + ")");
        ctx.content.newLineAtOffset(0, -14);
        ctx.content.showText("Date Scope: " + payload.getFilter().getFormattedRange() +
                "  •  Generated: " + payload.getFormattedGeneratedAt());
        ctx.content.endText();

        ctx.currentY = startY - HEADER_HEIGHT - 16;
    }

    private void drawSummaryKpis(PdfContext ctx, Map<String, String> kpis) throws IOException {
        float width = ctx.page.getMediaBox().getWidth() - (PAGE_MARGIN * 2);
        float boxHeight = 36f;
        float startY = ctx.currentY;

        // Box background
        ctx.content.setNonStrokingColor(COLOR_KPI_BG); // #F1F5F9
        ctx.content.addRect(PAGE_MARGIN, startY - boxHeight, width, boxHeight);
        ctx.content.fill();

        // Border line
        ctx.content.setStrokingColor(COLOR_BORDER); // #CBD5E1
        ctx.content.setLineWidth(0.8f);
        ctx.content.addRect(PAGE_MARGIN, startY - boxHeight, width, boxHeight);
        ctx.content.stroke();

        // Print KPI labels and values
        int count = kpis.size();
        float colWidth = width / Math.max(1, count);
        float currentX = PAGE_MARGIN;

        for (Map.Entry<String, String> entry : kpis.entrySet()) {
            ctx.content.setNonStrokingColor(COLOR_MUTED); // Gray #64748B
            ctx.content.beginText();
            ctx.content.setFont(fontRegular, 8.5f);
            ctx.content.newLineAtOffset(currentX + 8, startY - 14);
            ctx.content.showText(truncate(entry.getKey(), 20));
            ctx.content.endText();

            ctx.content.setNonStrokingColor(COLOR_DARK); // Bold text
            ctx.content.beginText();
            ctx.content.setFont(fontBold, 11);
            ctx.content.newLineAtOffset(currentX + 8, startY - 28);
            ctx.content.showText(entry.getValue());
            ctx.content.endText();

            currentX += colWidth;
        }

        ctx.currentY = startY - boxHeight - 16;
    }

    private void drawReportTable(PdfContext ctx, ReportPayload payload) throws IOException {
        switch (payload.getReportType()) {
            case JOB_APPLICATIONS -> drawApplicationsTable(ctx, payload.getApplications());
            case INTERVIEW_HISTORY -> drawInterviewsTable(ctx, payload.getInterviews());
            case RECRUITER_DIRECTORY -> drawRecruitersTable(ctx, payload.getRecruiters());
            case APPLICATION_STATUS -> drawStatusTable(ctx, payload.getStatusDistributions());
            case DATE_WISE_ACTIVITY -> drawActivityTable(ctx, payload.getActivities());
            case ANALYTICS_SUMMARY -> drawAnalyticsSummaryTables(ctx, payload);
        }
    }

    private void drawApplicationsTable(PdfContext ctx, List<ApplicationReportItem> apps) throws IOException {
        String[] headers = {"Company", "Role", "Type", "Applied Date", "Deadline", "Status", "Salary", "Recruiter"};
        float[] widths = {85, 105, 55, 65, 65, 60, 50, 65};

        drawTableHeaders(ctx, headers, widths);

        if (apps == null || apps.isEmpty()) {
            drawEmptyRow(ctx, "No job applications found for the selected timeframe.", widths);
            return;
        }

        boolean alt = false;
        for (ApplicationReportItem item : apps) {
            checkPageOverflow(ctx, headers, widths);
            String[] row = {
                    item.getCompanyName(),
                    item.getJobRole(),
                    item.getJobType(),
                    item.getFormattedApplicationDate(),
                    item.getFormattedDeadline(),
                    item.getStatus(),
                    item.getFormattedSalary(),
                    item.getRecruiterName()
            };
            drawTableRow(ctx, row, widths, alt);
            alt = !alt;
        }
    }

    private void drawInterviewsTable(PdfContext ctx, List<InterviewReportItem> interviews) throws IOException {
        String[] headers = {"Company", "Job Role", "Round", "Date & Time", "Mode", "Outcome", "Interviewer"};
        float[] widths = {85, 95, 90, 95, 50, 60, 75};

        drawTableHeaders(ctx, headers, widths);

        if (interviews == null || interviews.isEmpty()) {
            drawEmptyRow(ctx, "No interview rounds found for the selected timeframe.", widths);
            return;
        }

        boolean alt = false;
        for (InterviewReportItem item : interviews) {
            checkPageOverflow(ctx, headers, widths);
            String[] row = {
                    item.getCompanyName(),
                    item.getJobRole(),
                    item.getRoundName(),
                    item.getFormattedDateTime(),
                    item.getInterviewMode(),
                    item.getOutcome(),
                    item.getInterviewerName()
            };
            drawTableRow(ctx, row, widths, alt);
            alt = !alt;
        }
    }

    private void drawRecruitersTable(PdfContext ctx, List<RecruiterReportItem> recruiters) throws IOException {
        String[] headers = {"Recruiter Name", "Company", "Email", "Phone", "LinkedIn", "Apps Count"};
        float[] widths = {100, 100, 120, 85, 95, 50};

        drawTableHeaders(ctx, headers, widths);

        if (recruiters == null || recruiters.isEmpty()) {
            drawEmptyRow(ctx, "No recruiter contacts found.", widths);
            return;
        }

        boolean alt = false;
        for (RecruiterReportItem item : recruiters) {
            checkPageOverflow(ctx, headers, widths);
            String[] row = {
                    item.getRecruiterName(),
                    item.getCompanyName(),
                    item.getEmail(),
                    item.getPhone(),
                    item.getLinkedinUrl(),
                    String.valueOf(item.getAssociatedAppsCount())
            };
            drawTableRow(ctx, row, widths, alt);
            alt = !alt;
        }
    }

    private void drawStatusTable(PdfContext ctx, List<StatusDistribution> statuses) throws IOException {
        String[] headers = {"Status Name", "Count", "Percentage of Total", "Lifecycle Category"};
        float[] widths = {150, 80, 120, 150};

        drawTableHeaders(ctx, headers, widths);

        if (statuses == null || statuses.isEmpty()) {
            drawEmptyRow(ctx, "No application status data available.", widths);
            return;
        }

        boolean alt = false;
        for (StatusDistribution item : statuses) {
            checkPageOverflow(ctx, headers, widths);
            String category = switch (item.getStatusName()) {
                case "Selected", "Rejected", "Withdrawn", "No Response" -> "Terminal Outcome";
                default -> "Active In-Progress";
            };
            String[] row = {
                    item.getStatusName(),
                    String.valueOf(item.getCount()),
                    item.getFormattedPercentage(),
                    category
            };
            drawTableRow(ctx, row, widths, alt);
            alt = !alt;
        }
    }

    private void drawActivityTable(PdfContext ctx, List<DateWiseActivityItem> activities) throws IOException {
        String[] headers = {"Activity Date", "Event Type", "Company", "Details (Role / Round)", "Status / Outcome"};
        float[] widths = {85, 120, 100, 140, 105};

        drawTableHeaders(ctx, headers, widths);

        if (activities == null || activities.isEmpty()) {
            drawEmptyRow(ctx, "No candidate activity recorded in this period.", widths);
            return;
        }

        boolean alt = false;
        for (DateWiseActivityItem item : activities) {
            checkPageOverflow(ctx, headers, widths);
            String[] row = {
                    item.getFormattedDate(),
                    item.getActivityType(),
                    item.getCompanyName(),
                    item.getDetails(),
                    item.getStatusOrOutcome()
            };
            drawTableRow(ctx, row, widths, alt);
            alt = !alt;
        }
    }

    private void drawAnalyticsSummaryTables(PdfContext ctx, ReportPayload payload) throws IOException {
        // Draw Status breakdown first
        ctx.content.setNonStrokingColor(COLOR_DARK);
        ctx.content.beginText();
        ctx.content.setFont(fontBold, 12);
        ctx.content.newLineAtOffset(PAGE_MARGIN, ctx.currentY);
        ctx.content.showText("1. Application Status Distribution");
        ctx.content.endText();
        ctx.currentY -= 14;

        drawStatusTable(ctx, payload.getStatusDistributions());

        ctx.currentY -= 16;
        checkPageOverflow(ctx, null, null);

        // Draw Recent Applications
        ctx.content.setNonStrokingColor(COLOR_DARK);
        ctx.content.beginText();
        ctx.content.setFont(fontBold, 12);
        ctx.content.newLineAtOffset(PAGE_MARGIN, ctx.currentY);
        ctx.content.showText("2. Recent Applications Log");
        ctx.content.endText();
        ctx.currentY -= 14;

        drawApplicationsTable(ctx, payload.getApplications());
    }

    private void drawTableHeaders(PdfContext ctx, String[] headers, float[] widths) throws IOException {
        float startX = PAGE_MARGIN;
        float startY = ctx.currentY;

        ctx.content.setNonStrokingColor(COLOR_PRIMARY); // Brand Indigo #4F46E5
        float totalWidth = 0;
        for (float w : widths) totalWidth += w;
        ctx.content.addRect(startX, startY - ROW_HEIGHT, totalWidth, ROW_HEIGHT);
        ctx.content.fill();

        ctx.content.setNonStrokingColor(Color.WHITE);
        for (int i = 0; i < headers.length; i++) {
            ctx.content.beginText();
            ctx.content.setFont(fontBold, 8.5f);
            ctx.content.newLineAtOffset(startX + 4, startY - 14);
            ctx.content.showText(headers[i]);
            ctx.content.endText();
            startX += widths[i];
        }
        ctx.currentY -= ROW_HEIGHT;
    }

    private void drawTableRow(PdfContext ctx, String[] data, float[] widths, boolean alternate) throws IOException {
        float startX = PAGE_MARGIN;
        float startY = ctx.currentY;
        float totalWidth = 0;
        for (float w : widths) totalWidth += w;

        // Background color
        if (alternate) {
            ctx.content.setNonStrokingColor(COLOR_ROW_ALT); // #F8FAFC
            ctx.content.addRect(startX, startY - ROW_HEIGHT, totalWidth, ROW_HEIGHT);
            ctx.content.fill();
        }

        // Row bottom border
        ctx.content.setStrokingColor(COLOR_ROW_BORDER); // #E2E8F0
        ctx.content.setLineWidth(0.5f);
        ctx.content.moveTo(startX, startY - ROW_HEIGHT);
        ctx.content.lineTo(startX + totalWidth, startY - ROW_HEIGHT);
        ctx.content.stroke();

        ctx.content.setNonStrokingColor(COLOR_TEXT); // Slate #1E293B
        for (int i = 0; i < data.length && i < widths.length; i++) {
            ctx.content.beginText();
            ctx.content.setFont(fontRegular, 8f);
            ctx.content.newLineAtOffset(startX + 4, startY - 13);
            ctx.content.showText(truncate(data[i], (int) (widths[i] / 5.2f)));
            ctx.content.endText();
            startX += widths[i];
        }
        ctx.currentY -= ROW_HEIGHT;
    }

    private void drawEmptyRow(PdfContext ctx, String message, float[] widths) throws IOException {
        float totalWidth = 0;
        for (float w : widths) totalWidth += w;

        ctx.content.setNonStrokingColor(COLOR_MUTED);
        ctx.content.beginText();
        ctx.content.setFont(fontRegular, 9f);
        ctx.content.newLineAtOffset(PAGE_MARGIN + 10, ctx.currentY - 14);
        ctx.content.showText(message);
        ctx.content.endText();
        ctx.currentY -= ROW_HEIGHT;
    }

    private void checkPageOverflow(PdfContext ctx, String[] headers, float[] widths) throws IOException {
        if (ctx.currentY <= 65) {
            ctx.newPage();
            if (headers != null && widths != null) {
                drawTableHeaders(ctx, headers, widths);
            }
        }
    }

    private void drawFooters(PDDocument document) throws IOException {
        int totalPages = document.getNumberOfPages();
        for (int i = 0; i < totalPages; i++) {
            PDPage page = document.getPage(i);
            try (PDPageContentStream cs = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true)) {
                cs.setNonStrokingColor(COLOR_FOOTER); // #94A3B8
                cs.beginText();
                cs.setFont(fontRegular, 8f);
                cs.newLineAtOffset(PAGE_MARGIN, 25);
                cs.showText("ApplyTrack Intelligence System  •  Confidential Career Management Report");
                cs.endText();

                String pageNum = "Page " + (i + 1) + " of " + totalPages;
                cs.beginText();
                cs.setFont(fontRegular, 8f);
                cs.newLineAtOffset(page.getMediaBox().getWidth() - PAGE_MARGIN - 60, 25);
                cs.showText(pageNum);
                cs.endText();
            }
        }
    }

    private String truncate(String text, int maxChars) {
        if (text == null) return "—";
        text = text.replace("\n", " ").replace("\r", " ").trim();
        if (text.length() <= maxChars) return text;
        return text.substring(0, Math.max(1, maxChars - 2)) + "…";
    }

    private static class PdfContext implements AutoCloseable {
        final PDDocument doc;
        PDPage page;
        PDPageContentStream content;
        float currentY;

        PdfContext(PDDocument doc) {
            this.doc = doc;
        }

        void newPage() throws IOException {
            if (content != null) {
                content.close();
                content = null;
            }
            page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            content = new PDPageContentStream(doc, page);
            currentY = page.getMediaBox().getHeight() - PAGE_MARGIN;
        }

        @Override
        public void close() throws IOException {
            if (content != null) {
                content.close();
                content = null;
            }
        }
    }
}

