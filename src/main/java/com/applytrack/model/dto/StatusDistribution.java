package com.applytrack.model.dto;

import com.applytrack.model.enums.ApplicationStatus;

/**
 * Data Transfer Object representing status distribution metrics for charts and lists.
 *
 * Demonstrates:
 * - DTO pattern & Encapsulation (Unit 2 & Unit 3)
 * - Safe numeric rounding and percentage computation (Unit 1)
 */
public class StatusDistribution implements Comparable<StatusDistribution> {

    private String statusName;
    private int count;
    private double percentage;
    private String badgeColor;

    public StatusDistribution() {
    }

    public StatusDistribution(String statusName, int count, double percentage) {
        this.statusName = statusName;
        this.count = count;
        this.percentage = Math.round(percentage * 10.0) / 10.0;
        this.badgeColor = ApplicationStatus.fromString(statusName).getBadgeColor();
    }

    public StatusDistribution(String statusName, int count, double percentage, String badgeColor) {
        this.statusName = statusName;
        this.count = count;
        this.percentage = Math.round(percentage * 10.0) / 10.0;
        this.badgeColor = badgeColor != null ? badgeColor : "#6366f1";
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
        this.badgeColor = ApplicationStatus.fromString(statusName).getBadgeColor();
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = Math.round(percentage * 10.0) / 10.0;
    }

    public String getFormattedPercentage() {
        return String.format("%.1f%%", percentage);
    }

    public String getBadgeColor() {
        return badgeColor != null ? badgeColor : "#6366f1";
    }

    public void setBadgeColor(String badgeColor) {
        this.badgeColor = badgeColor;
    }

    @Override
    public int compareTo(StatusDistribution other) {
        if (other == null) {
            return -1;
        }
        return Integer.compare(other.count, this.count); // Descending by count
    }

    @Override
    public String toString() {
        return "StatusDistribution{" +
                "statusName='" + statusName + '\'' +
                ", count=" + count +
                ", percentage=" + percentage +
                '}';
    }
}

