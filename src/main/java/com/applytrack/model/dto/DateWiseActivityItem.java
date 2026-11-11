package com.applytrack.model.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Data Transfer Object representing an action in the Date-Wise Activity Report.
 *
 * Demonstrates:
 * - DTO pattern & Encapsulation (Unit 2 & Unit 3)
 * - Comparable interface for timeline ordering (Unit 2)
 */
public class DateWiseActivityItem implements Comparable<DateWiseActivityItem> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private LocalDate activityDate;
    private String activityType; // "Application Submitted", "Interview Scheduled", "Interview Completed"
    private String companyName;
    private String details; // Job role or interview round
    private String statusOrOutcome;

    public DateWiseActivityItem() {
    }

    public DateWiseActivityItem(LocalDate activityDate, String activityType, String companyName,
                                String details, String statusOrOutcome) {
        this.activityDate = activityDate;
        this.activityType = activityType != null ? activityType : "Activity";
        this.companyName = companyName != null ? companyName : "Unknown";
        this.details = details != null ? details : "";
        this.statusOrOutcome = statusOrOutcome != null ? statusOrOutcome : "";
    }

    public LocalDate getActivityDate() {
        return activityDate;
    }

    public void setActivityDate(LocalDate activityDate) {
        this.activityDate = activityDate;
    }

    public String getFormattedDate() {
        return activityDate != null ? activityDate.format(DATE_FORMATTER) : "—";
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getStatusOrOutcome() {
        return statusOrOutcome;
    }

    public void setStatusOrOutcome(String statusOrOutcome) {
        this.statusOrOutcome = statusOrOutcome;
    }

    @Override
    public int compareTo(DateWiseActivityItem other) {
        if (other == null || other.activityDate == null) {
            return -1;
        }
        if (this.activityDate == null) {
            return 1;
        }
        // Descending by date (most recent first)
        int cmp = other.activityDate.compareTo(this.activityDate);
        if (cmp != 0) {
            return cmp;
        }
        return this.companyName.compareToIgnoreCase(other.companyName);
    }
}

