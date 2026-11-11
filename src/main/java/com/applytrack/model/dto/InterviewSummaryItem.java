package com.applytrack.model.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Data Transfer Object (DTO) representing a scheduled or upcoming interview round.
 *
 * Demonstrates:
 * - OOP: Encapsulation & Separation of Concerns (Unit 3)
 * - Modern Date/Time API (java.time.LocalDateTime & DateTimeFormatter) - Unit 1 & 2
 * - Interfaces: Comparable<InterviewSummaryItem> for chronological sorting (Unit 3)
 * - Defensive copying and null-safe methods (Unit 2)
 */
public class InterviewSummaryItem implements Comparable<InterviewSummaryItem> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.US);
    private static final DateTimeFormatter FULL_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm", Locale.US);

    private final int interviewId;
    private final int applicationId;
    private final String companyName;
    private final String jobRole;
    private final String roundName;
    private final LocalDateTime interviewDate;
    private final String interviewMode;
    private final String meetingLink;
    private final String interviewerName;
    private final String outcome;

    public InterviewSummaryItem(int interviewId, int applicationId, String companyName, String jobRole,
                                String roundName, LocalDateTime interviewDate, String interviewMode,
                                String meetingLink, String interviewerName, String outcome) {
        this.interviewId = interviewId;
        this.applicationId = applicationId;
        this.companyName = companyName != null ? companyName : "Unknown Company";
        this.jobRole = jobRole != null ? jobRole : "Unknown Role";
        this.roundName = roundName != null ? roundName : "Interview Round";
        this.interviewDate = interviewDate;
        this.interviewMode = interviewMode != null ? interviewMode : "Online";
        this.meetingLink = meetingLink;
        this.interviewerName = interviewerName;
        this.outcome = outcome != null ? outcome : "Scheduled";
    }

    public int getInterviewId() {
        return interviewId;
    }

    public int getApplicationId() {
        return applicationId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getJobRole() {
        return jobRole;
    }

    public String getRoundName() {
        return roundName;
    }

    public LocalDateTime getInterviewDate() {
        return interviewDate;
    }

    public String getInterviewMode() {
        return interviewMode;
    }

    public String getMeetingLink() {
        return meetingLink;
    }

    public String getInterviewerName() {
        return interviewerName;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getFormattedDate() {
        return interviewDate != null ? interviewDate.format(DATE_FORMATTER) : "TBD";
    }

    public String getFormattedTime() {
        return interviewDate != null ? interviewDate.format(TIME_FORMATTER) + " IST" : "";
    }

    public String getFormattedDateTime() {
        return interviewDate != null ? interviewDate.format(FULL_FORMATTER) + " IST" : "TBD";
    }

    @Override
    public int compareTo(InterviewSummaryItem other) {
        if (other == null || other.interviewDate == null) return -1;
        if (this.interviewDate == null) return 1;
        return this.interviewDate.compareTo(other.interviewDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InterviewSummaryItem that = (InterviewSummaryItem) o;
        return interviewId == that.interviewId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(interviewId);
    }

    @Override
    public String toString() {
        return "InterviewSummaryItem{" +
                "companyName='" + companyName + '\'' +
                ", roundName='" + roundName + '\'' +
                ", interviewDate=" + interviewDate +
                ", interviewMode='" + interviewMode + '\'' +
                ", outcome='" + outcome + '\'' +
                '}';
    }
}
