package com.applytrack.model.dto;

/**
 * Data Transfer Object encapsulating key analytical takeaways and executive observations
 * derived from candidate history.
 *
 * Demonstrates:
 * - DTO pattern & Encapsulation (Unit 2 & Unit 3)
 * - Safe string formatting and conditional messaging (Unit 1)
 */
public class KeyInsights {

    private String topCompany;
    private int topCompanyCount;
    private String peakMonth;
    private int peakMonthCount;
    private int completedInterviews;
    private int pendingDecisions;
    private double offerRate;
    private boolean hasData;
    private String summaryText;

    public KeyInsights() {
        this.hasData = false;
        this.summaryText = "No application activity recorded for this period.";
    }

    public String getTopCompany() {
        return topCompany != null ? topCompany : "None";
    }

    public void setTopCompany(String topCompany) {
        this.topCompany = topCompany;
    }

    public int getTopCompanyCount() {
        return topCompanyCount;
    }

    public void setTopCompanyCount(int topCompanyCount) {
        this.topCompanyCount = topCompanyCount;
    }

    public String getPeakMonth() {
        return peakMonth != null ? peakMonth : "None";
    }

    public void setPeakMonth(String peakMonth) {
        this.peakMonth = peakMonth;
    }

    public int getPeakMonthCount() {
        return peakMonthCount;
    }

    public void setPeakMonthCount(int peakMonthCount) {
        this.peakMonthCount = peakMonthCount;
    }

    public int getCompletedInterviews() {
        return completedInterviews;
    }

    public void setCompletedInterviews(int completedInterviews) {
        this.completedInterviews = completedInterviews;
    }

    public int getPendingDecisions() {
        return pendingDecisions;
    }

    public void setPendingDecisions(int pendingDecisions) {
        this.pendingDecisions = pendingDecisions;
    }

    public double getOfferRate() {
        return offerRate;
    }

    public void setOfferRate(double offerRate) {
        this.offerRate = Math.round(offerRate * 10.0) / 10.0;
    }

    public String getFormattedOfferRate() {
        return String.format("%.1f%%", offerRate);
    }

    public boolean isHasData() {
        return hasData;
    }

    public void setHasData(boolean hasData) {
        this.hasData = hasData;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    @Override
    public String toString() {
        return "KeyInsights{" +
                "topCompany='" + topCompany + '\'' +
                ", peakMonth='" + peakMonth + '\'' +
                ", pendingDecisions=" + pendingDecisions +
                ", offerRate=" + offerRate +
                ", hasData=" + hasData +
                '}';
    }
}

