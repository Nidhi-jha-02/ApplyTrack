package com.applytrack.model.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Data Transfer Object representing an interview round in the Interview History Report.
 *
 * Demonstrates:
 * - DTO pattern & Encapsulation (Unit 2 & Unit 3)
 * - Modern Java Date/Time formatting with LocalDateTime (Unit 1)
 */
public class InterviewReportItem {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);
    private static final DateTimeFormatter FULL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a", Locale.US);

    private int interviewId;
    private String companyName;
    private String jobRole;
    private String roundName;
    private LocalDateTime interviewDate;
    private String interviewMode;
    private String interviewLocation;
    private String meetingLink;
    private String interviewerName;
    private String feedback;
    private String outcome;

    public InterviewReportItem() {
    }

    public InterviewReportItem(int interviewId, String companyName, String jobRole, String roundName,
                               LocalDateTime interviewDate, String interviewMode, String interviewLocation,
                               String meetingLink, String interviewerName, String feedback, String outcome) {
        this.interviewId = interviewId;
        this.companyName = companyName != null ? companyName : "Unknown";
        this.jobRole = jobRole != null ? jobRole : "";
        this.roundName = roundName != null ? roundName : "Round";
        this.interviewDate = interviewDate;
        this.interviewMode = interviewMode != null ? interviewMode : "Online";
        this.interviewLocation = interviewLocation != null ? interviewLocation : "";
        this.meetingLink = meetingLink != null ? meetingLink : "";
        this.interviewerName = interviewerName != null && !interviewerName.isBlank() ? interviewerName : "—";
        this.feedback = feedback != null ? feedback : "";
        this.outcome = outcome != null ? outcome : "Scheduled";
    }

    public int getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(int interviewId) {
        this.interviewId = interviewId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getJobRole() {
        return jobRole;
    }

    public void setJobRole(String jobRole) {
        this.jobRole = jobRole;
    }

    public String getRoundName() {
        return roundName;
    }

    public void setRoundName(String roundName) {
        this.roundName = roundName;
    }

    public LocalDateTime getInterviewDate() {
        return interviewDate;
    }

    public void setInterviewDate(LocalDateTime interviewDate) {
        this.interviewDate = interviewDate;
    }

    public String getFormattedDate() {
        return interviewDate != null ? interviewDate.format(DATE_FORMATTER) : "—";
    }

    public String getFormattedTime() {
        return interviewDate != null ? interviewDate.format(TIME_FORMATTER) : "—";
    }

    public String getFormattedDateTime() {
        return interviewDate != null ? interviewDate.format(FULL_FORMATTER) : "—";
    }

    public String getInterviewMode() {
        return interviewMode;
    }

    public void setInterviewMode(String interviewMode) {
        this.interviewMode = interviewMode;
    }

    public String getInterviewLocation() {
        return interviewLocation;
    }

    public void setInterviewLocation(String interviewLocation) {
        this.interviewLocation = interviewLocation;
    }

    public String getMeetingLink() {
        return meetingLink;
    }

    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }

    public String getInterviewerName() {
        return interviewerName;
    }

    public void setInterviewerName(String interviewerName) {
        this.interviewerName = interviewerName;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }
}

