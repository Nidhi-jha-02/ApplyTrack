package com.applytrack.model.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Data Transfer Object (DTO) representing an upcoming application deadline.
 *
 * Demonstrates:
 * - OOP: Encapsulation & Separation of Concerns (Unit 3)
 * - Modern Java Date API: java.time.LocalDate and ChronoUnit (Unit 1 & 2)
 * - Interfaces: Comparable<DeadlineSummaryItem> for natural sorting (Unit 3)
 * - Enum-style urgency classification (Unit 1 & 3)
 */
public class DeadlineSummaryItem implements Comparable<DeadlineSummaryItem> {

    public enum Urgency {
        OVERDUE("Overdue", "-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c;"),
        URGENT_3_DAYS("Due in ≤ 3 Days", "-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c;"),
        SOON_7_DAYS("Approaching (< 7 Days)", "-fx-background-color: #fef3c7; -fx-text-fill: #b45309;"),
        UPCOMING("Upcoming", "-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8;");

        private final String label;
        private final String badgeStyle;

        Urgency(String label, String badgeStyle) {
            this.label = label;
            this.badgeStyle = badgeStyle;
        }

        public String getLabel() {
            return label;
        }

        public String getBadgeStyle() {
            return badgeStyle;
        }
    }

    private final int applicationId;
    private final String companyName;
    private final String jobRole;
    private final LocalDate deadline;
    private final long daysRemaining;
    private final String status;

    public DeadlineSummaryItem(int applicationId, String companyName, String jobRole, LocalDate deadline, String status) {
        this.applicationId = applicationId;
        this.companyName = companyName != null ? companyName : "Unknown Company";
        this.jobRole = jobRole != null ? jobRole : "Unknown Role";
        this.deadline = deadline;
        this.status = status != null ? status : "Applied";
        this.daysRemaining = deadline != null ? ChronoUnit.DAYS.between(LocalDate.now(), deadline) : 0;
    }

    public DeadlineSummaryItem(int applicationId, String companyName, String jobRole, LocalDate deadline, long daysRemaining, String status) {
        this.applicationId = applicationId;
        this.companyName = companyName != null ? companyName : "Unknown Company";
        this.jobRole = jobRole != null ? jobRole : "Unknown Role";
        this.deadline = deadline;
        this.daysRemaining = daysRemaining;
        this.status = status != null ? status : "Applied";
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

    public LocalDate getDeadline() {
        return deadline;
    }

    public long getDaysRemaining() {
        return daysRemaining;
    }

    public String getStatus() {
        return status;
    }

    /**
     * Categorizes deadline urgency based on days remaining.
     * Demonstrates conditional logic and branch evaluation (Unit 1).
     */
    public Urgency getUrgency() {
        if (daysRemaining < 0) {
            return Urgency.OVERDUE;
        } else if (daysRemaining <= 3) {
            return Urgency.URGENT_3_DAYS;
        } else if (daysRemaining <= 7) {
            return Urgency.SOON_7_DAYS;
        } else {
            return Urgency.UPCOMING;
        }
    }

    public String getFormattedRemaining() {
        if (daysRemaining < 0) {
            return Math.abs(daysRemaining) + " days overdue";
        } else if (daysRemaining == 0) {
            return "Due today!";
        } else if (daysRemaining == 1) {
            return "1 day remaining";
        } else {
            return daysRemaining + " days remaining";
        }
    }

    @Override
    public int compareTo(DeadlineSummaryItem other) {
        if (other == null || other.deadline == null) return -1;
        if (this.deadline == null) return 1;
        return this.deadline.compareTo(other.deadline);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeadlineSummaryItem that = (DeadlineSummaryItem) o;
        return applicationId == that.applicationId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationId);
    }

    @Override
    public String toString() {
        return "DeadlineSummaryItem{" +
                "companyName='" + companyName + '\'' +
                ", jobRole='" + jobRole + '\'' +
                ", deadline=" + deadline +
                ", daysRemaining=" + daysRemaining +
                ", status='" + status + '\'' +
                '}';
    }
}

