package com.applytrack.model;

import com.applytrack.model.base.BaseEntity;
import com.applytrack.model.enums.InterviewMode;
import com.applytrack.model.enums.InterviewOutcome;
import com.applytrack.model.interfaces.Searchable;
import com.applytrack.util.ValidationUtil;

import java.time.LocalDateTime;

/**
 * Represents a single evaluation round (Online Assessment, Technical Interview, HR round).
 *
 * Demonstrates:
 * - Inheritance: extends BaseEntity (Unit 3)
 * - Interfaces: implements Searchable (Unit 3)
 * - Modern Java Date/Time: LocalDateTime (Unit 1 & 2)
 * - Java Enums: InterviewMode, InterviewOutcome (Unit 2)
 * - Encapsulation and Data Validation (Unit 3)
 * - Many-to-One relationship back to parent Application without recursion (Unit 2 & 3)
 */
public class Interview extends BaseEntity implements Searchable {

    private int applicationId;
    private Application application; // Optional domain reference

    private String roundName;
    private LocalDateTime interviewDate;
    private InterviewMode interviewMode;
    private String interviewLocation;
    private String meetingLink;
    private String interviewerName;
    private String feedback;
    private InterviewOutcome outcome;

    public Interview() {
        super();
        this.interviewMode = InterviewMode.ONLINE;
        this.outcome = InterviewOutcome.SCHEDULED;
    }

    public Interview(int applicationId, String roundName, LocalDateTime interviewDate) {
        this(0, applicationId, roundName, interviewDate, InterviewMode.ONLINE, null, null, null, null,
                InterviewOutcome.SCHEDULED, LocalDateTime.now(), LocalDateTime.now());
    }

    public Interview(int applicationId, String roundName, LocalDateTime interviewDate,
                     InterviewMode interviewMode, String meetingLink, String interviewerName,
                     InterviewOutcome outcome) {
        this(0, applicationId, roundName, interviewDate, interviewMode, null, meetingLink, interviewerName,
                null, outcome, LocalDateTime.now(), LocalDateTime.now());
    }

    public Interview(int interviewId, int applicationId, String roundName, LocalDateTime interviewDate,
                     InterviewMode interviewMode, String interviewLocation, String meetingLink,
                     String interviewerName, String feedback, InterviewOutcome outcome,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(interviewId, createdAt, updatedAt);
        this.applicationId = applicationId;
        setRoundName(roundName);
        setInterviewDate(interviewDate);
        this.interviewMode = (interviewMode != null) ? interviewMode : InterviewMode.ONLINE;
        this.interviewLocation = interviewLocation;
        setMeetingLink(meetingLink);
        this.interviewerName = interviewerName;
        this.feedback = feedback;
        this.outcome = (outcome != null) ? outcome : InterviewOutcome.SCHEDULED;
    }

    // Convenience alias for entity ID
    public int getInterviewId() {
        return getId();
    }

    public void setInterviewId(int interviewId) {
        setId(interviewId);
    }

    public int getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
        if (application != null) {
            this.applicationId = application.getId();
        }
    }

    public String getRoundName() {
        return roundName;
    }

    /**
     * Convenience getter for UI TableView bindings and presentation.
     */
    public String getCompanyName() {
        return (application != null && application.getCompanyName() != null)
                ? application.getCompanyName()
                : "Unknown Company";
    }

    /**
     * Convenience getter for UI TableView bindings and presentation.
     */
    public String getJobRole() {
        return (application != null && application.getJobRole() != null)
                ? application.getJobRole()
                : "Unknown Role";
    }

    /**
     * Formats interview date for UI display (e.g. "Nov 15, 2026").
     */
    public String getFormattedDate() {
        if (interviewDate == null) {
            return "—";
        }
        return interviewDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy", java.util.Locale.US));
    }

    /**
     * Formats interview time for UI display (e.g. "02:30 PM").
     */
    public String getFormattedTime() {
        if (interviewDate == null) {
            return "—";
        }
        return interviewDate.format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.US));
    }

    /**
     * Formats both interview date and time for UI display (e.g. "Nov 15, 2026 • 02:30 PM").
     */
    public String getFormattedDateTime() {
        if (interviewDate == null) {
            return "—";
        }
        return interviewDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a", java.util.Locale.US));
    }

    /**
     * Checks if this interview is scheduled in the future.
     */
    public boolean isUpcoming() {
        return interviewDate != null && interviewDate.isAfter(LocalDateTime.now()) && outcome == InterviewOutcome.SCHEDULED;
    }

    public void setRoundName(String roundName) {
        if (!ValidationUtil.isNotEmpty(roundName)) {
            throw new IllegalArgumentException("Interview round name cannot be null or empty.");
        }
        this.roundName = roundName.trim();
    }

    public LocalDateTime getInterviewDate() {
        return interviewDate;
    }

    public void setInterviewDate(LocalDateTime interviewDate) {
        if (interviewDate == null) {
            throw new IllegalArgumentException("Interview date and time cannot be null.");
        }
        this.interviewDate = interviewDate;
    }

    public InterviewMode getInterviewMode() {
        return interviewMode;
    }

    public void setInterviewMode(InterviewMode interviewMode) {
        this.interviewMode = (interviewMode != null) ? interviewMode : InterviewMode.ONLINE;
    }

    public String getInterviewLocation() {
        return interviewLocation;
    }

    public void setInterviewLocation(String interviewLocation) {
        this.interviewLocation = (interviewLocation != null) ? interviewLocation.trim() : null;
    }

    public String getMeetingLink() {
        return meetingLink;
    }

    public void setMeetingLink(String meetingLink) {
        if (meetingLink != null && !meetingLink.trim().isEmpty() && !ValidationUtil.isValidUrl(meetingLink)) {
            throw new IllegalArgumentException("Invalid meeting URL: " + meetingLink);
        }
        this.meetingLink = (meetingLink != null) ? meetingLink.trim() : null;
    }

    public String getInterviewerName() {
        return interviewerName;
    }

    public void setInterviewerName(String interviewerName) {
        this.interviewerName = (interviewerName != null) ? interviewerName.trim() : null;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = (feedback != null) ? feedback.trim() : null;
    }

    public InterviewOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(InterviewOutcome outcome) {
        this.outcome = (outcome != null) ? outcome : InterviewOutcome.SCHEDULED;
    }

    @Override
    public boolean matches(String query) {
        if (!ValidationUtil.isNotEmpty(query)) {
            return true;
        }
        String q = query.trim().toLowerCase();
        return (roundName != null && roundName.toLowerCase().contains(q)) ||
               (interviewerName != null && interviewerName.toLowerCase().contains(q)) ||
               (feedback != null && feedback.toLowerCase().contains(q)) ||
               (outcome != null && outcome.getDisplayName().toLowerCase().contains(q)) ||
               (interviewMode != null && interviewMode.getDisplayName().toLowerCase().contains(q)) ||
               (application != null && application.getCompanyName() != null && application.getCompanyName().toLowerCase().contains(q)) ||
               (application != null && application.getJobRole() != null && application.getJobRole().toLowerCase().contains(q));
    }

    @Override
    public String getDisplayTitle() {
        return roundName + " [" + interviewMode.getDisplayName() + "] - " + outcome.getDisplayName();
    }

    @Override
    public String toString() {
        return "Interview{" +
                "interviewId=" + getId() +
                ", applicationId=" + applicationId +
                ", roundName='" + roundName + '\'' +
                ", interviewDate=" + interviewDate +
                ", interviewMode=" + interviewMode +
                ", outcome=" + outcome +
                '}';
    }
}

