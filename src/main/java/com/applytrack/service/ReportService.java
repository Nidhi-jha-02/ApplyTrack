package com.applytrack.service;

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
import com.applytrack.repository.ReportRepository;
import com.applytrack.repository.impl.JdbcReportRepository;
import com.applytrack.service.export.ExcelExportService;
import com.applytrack.service.export.PdfExportService;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Service layer coordinating report preparation, KPI synthesis,
 * date-range filtering, and delegating to PDF and Excel export services.
 *
 * Demonstrates:
 * - Layered Architecture: UI -> Service -> Repository / Exporters (Unit 3)
 * - Constructor Overloading & Dependency Injection for Testing (Unit 2 & Unit 3)
 * - User-Specific Data Security (user_id enforcement) (Unit 3)
 * - Exception Handling across SQLException, IOException, and ValidationException (Unit 3)
 */
public class ReportService {

    private final ReportRepository reportRepository;
    private final AnalyticsService analyticsService;
    private final PdfExportService pdfExportService;
    private final ExcelExportService excelExportService;

    public ReportService() {
        this(new JdbcReportRepository(), new AnalyticsService(), new PdfExportService(), new ExcelExportService());
    }

    public ReportService(ReportRepository reportRepository, AnalyticsService analyticsService,
                         PdfExportService pdfExportService, ExcelExportService excelExportService) {
        if (reportRepository == null) throw new IllegalArgumentException("ReportRepository cannot be null.");
        if (analyticsService == null) throw new IllegalArgumentException("AnalyticsService cannot be null.");
        if (pdfExportService == null) throw new IllegalArgumentException("PdfExportService cannot be null.");
        if (excelExportService == null) throw new IllegalArgumentException("ExcelExportService cannot be null.");

        this.reportRepository = reportRepository;
        this.analyticsService = analyticsService;
        this.pdfExportService = pdfExportService;
        this.excelExportService = excelExportService;
    }

    /**
     * Prepares and packages all report data, summaries, and KPIs for the specified user and criteria.
     */
    public ReportPayload prepareReportPayload(int userId, String candidateName, String candidateEmail,
                                              ReportType type, ReportFilter filter)
            throws SQLException, ValidationException {

        if (type == null) {
            type = ReportType.JOB_APPLICATIONS;
        }
        if (filter == null) {
            filter = new ReportFilter();
        }
        filter.validate();

        ReportPayload payload = new ReportPayload(type, candidateName, candidateEmail, filter);
        LocalDate start = filter.getEffectiveStartDate();
        LocalDate end = filter.getEffectiveEndDate();

        switch (type) {
            case JOB_APPLICATIONS -> {
                List<ApplicationReportItem> apps = reportRepository.getApplicationReportData(userId, start, end);
                payload.setApplications(apps);

                int total = apps.size();
                long active = apps.stream().filter(a -> isStatusActive(a.getStatus())).count();
                long selected = apps.stream().filter(a -> "Selected".equalsIgnoreCase(a.getStatus())).count();
                long rejected = apps.stream().filter(a -> "Rejected".equalsIgnoreCase(a.getStatus())).count();

                payload.addSummaryKpi("Total Applications", String.valueOf(total));
                payload.addSummaryKpi("Active In-Progress", String.valueOf(active));
                payload.addSummaryKpi("Offers (Selected)", String.valueOf(selected));
                payload.addSummaryKpi("Rejected", String.valueOf(rejected));
            }

            case INTERVIEW_HISTORY -> {
                List<InterviewReportItem> interviews = reportRepository.getInterviewReportData(userId, start, end);
                payload.setInterviews(interviews);

                int total = interviews.size();
                long passed = interviews.stream().filter(i -> "Passed".equalsIgnoreCase(i.getOutcome())).count();
                long scheduled = interviews.stream().filter(i -> "Scheduled".equalsIgnoreCase(i.getOutcome())).count();
                long failed = interviews.stream().filter(i -> "Failed".equalsIgnoreCase(i.getOutcome())).count();

                payload.addSummaryKpi("Total Rounds", String.valueOf(total));
                payload.addSummaryKpi("Passed Rounds", String.valueOf(passed));
                payload.addSummaryKpi("Scheduled Upcoming", String.valueOf(scheduled));
                payload.addSummaryKpi("Failed / Other", String.valueOf(failed));
            }

            case RECRUITER_DIRECTORY -> {
                List<RecruiterReportItem> recruiters = reportRepository.getRecruiterReportData(userId);
                payload.setRecruiters(recruiters);

                long companies = recruiters.stream().map(RecruiterReportItem::getCompanyName).distinct().count();
                int totalApps = recruiters.stream().mapToInt(RecruiterReportItem::getAssociatedAppsCount).sum();

                payload.addSummaryKpi("Total Recruiters", String.valueOf(recruiters.size()));
                payload.addSummaryKpi("Companies Represented", String.valueOf(companies));
                payload.addSummaryKpi("Total Linked Apps", String.valueOf(totalApps));
            }

            case APPLICATION_STATUS -> {
                AnalyticsSummary summary = analyticsService.getAnalyticsSummary(userId, filter.getDateRangeFilter());
                List<StatusDistribution> distributions = summary.getStatusDistributions();
                payload.setStatusDistributions(distributions);

                payload.addSummaryKpi("Total Applications", String.valueOf(summary.getTotalApplications()));
                payload.addSummaryKpi("Active In-Progress", String.valueOf(summary.getActiveApplications()));
                payload.addSummaryKpi("Terminal Outcomes", String.valueOf(summary.getTotalApplications() - summary.getActiveApplications()));
            }

            case ANALYTICS_SUMMARY -> {
                AnalyticsSummary summary = analyticsService.getAnalyticsSummary(userId, filter.getDateRangeFilter());
                payload.setAnalyticsSummary(summary);
                payload.setStatusDistributions(summary.getStatusDistributions());
                payload.setApplications(reportRepository.getApplicationReportData(userId, start, end));

                payload.addSummaryKpi("Total Applications", String.valueOf(summary.getTotalApplications()));
                payload.addSummaryKpi("Active Pipeline", String.valueOf(summary.getActiveApplications()));
                payload.addSummaryKpi("Total Interviews", String.valueOf(summary.getTotalInterviews()));
                payload.addSummaryKpi("Offers Received", String.valueOf(summary.getOffersReceived()));
                payload.addSummaryKpi("Interview Conversion", summary.getFormattedConversionRate());
                payload.addSummaryKpi("Offer Success Rate", summary.getFormattedOfferRate());
            }

            case DATE_WISE_ACTIVITY -> {
                List<DateWiseActivityItem> activities = reportRepository.getDateWiseActivityData(userId, start, end);
                payload.setActivities(activities);

                long appCount = activities.stream().filter(a -> a.getActivityType().startsWith("Application")).count();
                long intCount = activities.stream().filter(a -> a.getActivityType().startsWith("Interview")).count();

                payload.addSummaryKpi("Total Events", String.valueOf(activities.size()));
                payload.addSummaryKpi("Applications Submitted", String.valueOf(appCount));
                payload.addSummaryKpi("Interview Rounds", String.valueOf(intCount));
            }
        }

        return payload;
    }

    /**
     * Exports the prepared payload into either PDF or Excel format based on the requested file extension.
     */
    public void exportReport(ReportPayload payload, File destinationFile) throws IOException, ValidationException {
        if (payload == null) {
            throw new ValidationException("Cannot export empty or uninitialized report payload.");
        }
        if (destinationFile == null) {
            throw new ValidationException("No destination file selected.");
        }

        String name = destinationFile.getName().toLowerCase();
        if (name.endsWith(".pdf")) {
            pdfExportService.exportToPdf(payload, destinationFile);
        } else if (name.endsWith(".xlsx")) {
            excelExportService.exportToExcel(payload, destinationFile);
        } else {
            // Default to PDF if unrecognized extension
            File correctedFile = new File(destinationFile.getParentFile(), destinationFile.getName() + ".pdf");
            pdfExportService.exportToPdf(payload, correctedFile);
        }
    }

    private boolean isStatusActive(String status) {
        if (status == null) return false;
        return switch (status.trim()) {
            case "Applied", "Under Review", "Shortlisted", "Interview Scheduled" -> true;
            default -> false;
        };
    }
}

