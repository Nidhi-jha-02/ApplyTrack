package com.applytrack.model.dto;

/**
 * Data Transfer Object representing company-wise application aggregation.
 *
 * Demonstrates:
 * - DTO pattern & Encapsulation (Unit 2 & Unit 3)
 * - Comparable interface implementation for ranking analysis (Unit 2)
 */
public class CompanyApplicationData implements Comparable<CompanyApplicationData> {

    private String companyName;
    private int applicationCount;
    private int interviewCount;
    private int offerCount;

    public CompanyApplicationData() {
    }

    public CompanyApplicationData(String companyName, int applicationCount) {
        this(companyName, applicationCount, 0, 0);
    }

    public CompanyApplicationData(String companyName, int applicationCount, int interviewCount, int offerCount) {
        this.companyName = companyName != null ? companyName : "Other / Unknown";
        this.applicationCount = applicationCount;
        this.interviewCount = interviewCount;
        this.offerCount = offerCount;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName != null ? companyName : "Other / Unknown";
    }

    public int getApplicationCount() {
        return applicationCount;
    }

    public void setApplicationCount(int applicationCount) {
        this.applicationCount = applicationCount;
    }

    public int getInterviewCount() {
        return interviewCount;
    }

    public void setInterviewCount(int interviewCount) {
        this.interviewCount = interviewCount;
    }

    public int getOfferCount() {
        return offerCount;
    }

    public void setOfferCount(int offerCount) {
        this.offerCount = offerCount;
    }

    @Override
    public int compareTo(CompanyApplicationData other) {
        if (other == null) {
            return -1;
        }
        // Descending by applicationCount, then ascending by companyName
        int countComparison = Integer.compare(other.applicationCount, this.applicationCount);
        if (countComparison != 0) {
            return countComparison;
        }
        return this.companyName.compareToIgnoreCase(other.companyName);
    }

    @Override
    public String toString() {
        return "CompanyApplicationData{" +
                "companyName='" + companyName + '\'' +
                ", applicationCount=" + applicationCount +
                ", interviewCount=" + interviewCount +
                ", offerCount=" + offerCount +
                '}';
    }
}

