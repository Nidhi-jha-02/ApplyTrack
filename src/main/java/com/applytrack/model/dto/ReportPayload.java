package com.applytrack.model.dto;

import com.applytrack.model.enums.ReportType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Master payload container encapsulating prepared report data, summary KPIs,
 * and user metadata for PDF and Excel export services.
 *
 * Demonstrates:
 * - DTO pattern & Polymorphic report structure (Unit 2 & Unit 3)
 * - Encapsulation and defensive list initialization (Unit 2)
 */
public class ReportPayload {

    private static final DateTimeFormatter GENERATED_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a", Locale.US);

    private ReportType reportType;
    private String candidateName;
    private String candidateEmail;
    private LocalDateTime generatedAt;
    private ReportFilter filter;

    private Map<String, String> summaryKpis = new LinkedHashMap<>();

    private List<ApplicationReportItem> applications = new ArrayList<>();
    private List<InterviewReportItem> interviews = new ArrayList<>();
    private List<RecruiterReportItem> recruiters = new ArrayList<>();
    private List<StatusDistribution> statusDistributions = new ArrayList<>();
    private AnalyticsSummary analyticsSummary;
    private List<DateWiseActivityItem> activities = new ArrayList<>();

    public ReportPayload() {
        this.generatedAt = LocalDateTime.now();
    }

    public ReportPayload(ReportType reportType, String candidateName, String candidateEmail, ReportFilter filter) {
        this.reportType = reportType;
        this.candidateName = candidateName;
        this.candidateEmail = candidateEmail;
        this.filter = filter;
        this.generatedAt = LocalDateTime.now();
    }

    public ReportType getReportType() {
        return reportType;
    }

    public void setReportType(ReportType reportType) {
        this.reportType = reportType;
    }

    public String getCandidateName() {
        return candidateName != null ? candidateName : "Candidate";
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getCandidateEmail() {
        return candidateEmail != null ? candidateEmail : "";
    }

    public void setCandidateEmail(String candidateEmail) {
        this.candidateEmail = candidateEmail;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getFormattedGeneratedAt() {
        return generatedAt != null ? generatedAt.format(GENERATED_FORMATTER) : "—";
    }

    public ReportFilter getFilter() {
        return filter != null ? filter : new ReportFilter();
    }

    public void setFilter(ReportFilter filter) {
        this.filter = filter;
    }

    public Map<String, String> getSummaryKpis() {
        return summaryKpis;
    }

    public void setSummaryKpis(Map<String, String> summaryKpis) {
        this.summaryKpis = summaryKpis != null ? summaryKpis : new LinkedHashMap<>();
    }

    public void addSummaryKpi(String label, String value) {
        this.summaryKpis.put(label, value);
    }

    public List<ApplicationReportItem> getApplications() {
        return applications;
    }

    public void setApplications(List<ApplicationReportItem> applications) {
        this.applications = applications != null ? applications : new ArrayList<>();
    }

    public List<InterviewReportItem> getInterviews() {
        return interviews;
    }

    public void setInterviews(List<InterviewReportItem> interviews) {
        this.interviews = interviews != null ? interviews : new ArrayList<>();
    }

    public List<RecruiterReportItem> getRecruiters() {
        return recruiters;
    }

    public void setRecruiters(List<RecruiterReportItem> recruiters) {
        this.recruiters = recruiters != null ? recruiters : new ArrayList<>();
    }

    public List<StatusDistribution> getStatusDistributions() {
        return statusDistributions;
    }

    public void setStatusDistributions(List<StatusDistribution> statusDistributions) {
        this.statusDistributions = statusDistributions != null ? statusDistributions : new ArrayList<>();
    }

    public AnalyticsSummary getAnalyticsSummary() {
        return analyticsSummary;
    }

    public void setAnalyticsSummary(AnalyticsSummary analyticsSummary) {
        this.analyticsSummary = analyticsSummary;
    }

    public List<DateWiseActivityItem> getActivities() {
        return activities;
    }

    public void setActivities(List<DateWiseActivityItem> activities) {
        this.activities = activities != null ? activities : new ArrayList<>();
    }

    public int getRecordCount() {
        return switch (reportType) {
            case JOB_APPLICATIONS -> applications.size();
            case INTERVIEW_HISTORY -> interviews.size();
            case RECRUITER_DIRECTORY -> recruiters.size();
            case APPLICATION_STATUS -> statusDistributions.size();
            case DATE_WISE_ACTIVITY -> activities.size();
            case ANALYTICS_SUMMARY -> (analyticsSummary != null ? analyticsSummary.getTotalApplications() : 0);
        };
    }
}

