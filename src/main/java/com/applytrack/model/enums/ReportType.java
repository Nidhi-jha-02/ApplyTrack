package com.applytrack.model.enums;

/**
 * Enumeration of supported report types in ApplyTrack.
 *
 * Demonstrates:
 * - Java Enums with custom fields and metadata (Unit 2)
 * - Enhanced switch pattern matching for report configurations (Unit 1 & Unit 2)
 */
public enum ReportType {
    JOB_APPLICATIONS(
            "Job Application Report",
            "Comprehensive log of all submitted applications, roles, dates, deadlines, salaries, and current statuses.",
            "ApplyTrack_Job_Applications"
    ),
    INTERVIEW_HISTORY(
            "Interview History Report",
            "Multi-round interview records, dates, modes, evaluation outcomes, interviewers, and feedback.",
            "ApplyTrack_Interview_History"
    ),
    RECRUITER_DIRECTORY(
            "Recruiter Directory Report",
            "Directory of talent acquisition contacts, company associations, emails, phone numbers, and notes.",
            "ApplyTrack_Recruiter_Directory"
    ),
    APPLICATION_STATUS(
            "Application Status Report",
            "Categorical breakdown of application statuses, active vs terminal counts, and percentage distributions.",
            "ApplyTrack_Status_Report"
    ),
    ANALYTICS_SUMMARY(
            "Analytics Summary Report",
            "Executive career intelligence summary including conversion funnels, offer rates, and monthly velocity.",
            "ApplyTrack_Analytics_Summary"
    ),
    DATE_WISE_ACTIVITY(
            "Date-wise Activity Report",
            "Chronological audit trail of application submissions, scheduled rounds, and interview milestones.",
            "ApplyTrack_Activity_Log"
    );

    private final String displayName;
    private final String description;
    private final String defaultFilenamePrefix;

    ReportType(String displayName, String description, String defaultFilenamePrefix) {
        this.displayName = displayName;
        this.description = description;
        this.defaultFilenamePrefix = defaultFilenamePrefix;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultFilenamePrefix() {
        return defaultFilenamePrefix;
    }

    public static ReportType fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return JOB_APPLICATIONS;
        }
        for (ReportType type : values()) {
            if (type.name().equalsIgnoreCase(text.trim()) ||
                type.displayName.equalsIgnoreCase(text.trim())) {
                return type;
            }
        }
        return JOB_APPLICATIONS;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

