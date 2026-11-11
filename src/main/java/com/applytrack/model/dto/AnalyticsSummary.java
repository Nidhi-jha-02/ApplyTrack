package com.applytrack.model.dto;

import com.applytrack.model.enums.DateRangeFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Master Data Transfer Object aggregating all career analytics metrics,
 * chart datasets, and insights for the logged-in user under a specific date filter.
 *
 * Demonstrates:
 * - DTO Design Pattern aggregating complex domain metrics (Unit 3)
 * - Safe numeric rounding and percentage computation (Unit 1)
 * - Collections Framework (List, Map) for UI binding (Unit 3)
 */
public class AnalyticsSummary {

    private DateRangeFilter dateRangeFilter = DateRangeFilter.ALL_TIME;

    // Core Summary KPI Metrics
    private int totalApplications;
    private int activeApplications;
    private int totalInterviews;
    private int offersReceived;
    private int rejectedApplications;
    private int distinctApplicationsInterviewed;
    private double interviewConversionRate; // (Distinct Apps Interviewed / Total Apps) * 100
    private double offerRate; // (Offers / Total Apps) * 100

    // Detailed Visualization Collections
    private List<StatusDistribution> statusDistributions = new ArrayList<>();
    private List<MonthlyApplicationData> monthlyApplicationTrends = new ArrayList<>();
    private List<CompanyApplicationData> companyApplicationData = new ArrayList<>();
    private InterviewAnalytics interviewAnalytics = new InterviewAnalytics();
    private KeyInsights keyInsights = new KeyInsights();

    public AnalyticsSummary() {
    }

    public DateRangeFilter getDateRangeFilter() {
        return dateRangeFilter;
    }

    public void setDateRangeFilter(DateRangeFilter dateRangeFilter) {
        this.dateRangeFilter = dateRangeFilter != null ? dateRangeFilter : DateRangeFilter.ALL_TIME;
    }

    public int getTotalApplications() {
        return totalApplications;
    }

    public void setTotalApplications(int totalApplications) {
        this.totalApplications = totalApplications;
    }

    public int getActiveApplications() {
        return activeApplications;
    }

    public void setActiveApplications(int activeApplications) {
        this.activeApplications = activeApplications;
    }

    public int getTotalInterviews() {
        return totalInterviews;
    }

    public void setTotalInterviews(int totalInterviews) {
        this.totalInterviews = totalInterviews;
    }

    public int getOffersReceived() {
        return offersReceived;
    }

    public void setOffersReceived(int offersReceived) {
        this.offersReceived = offersReceived;
    }

    public int getRejectedApplications() {
        return rejectedApplications;
    }

    public void setRejectedApplications(int rejectedApplications) {
        this.rejectedApplications = rejectedApplications;
    }

    public int getDistinctApplicationsInterviewed() {
        return distinctApplicationsInterviewed;
    }

    public void setDistinctApplicationsInterviewed(int distinctApplicationsInterviewed) {
        this.distinctApplicationsInterviewed = distinctApplicationsInterviewed;
    }

    public double getInterviewConversionRate() {
        return interviewConversionRate;
    }

    public void setInterviewConversionRate(double interviewConversionRate) {
        this.interviewConversionRate = Math.round(interviewConversionRate * 10.0) / 10.0;
    }

    public String getFormattedConversionRate() {
        return String.format("%.1f%%", interviewConversionRate);
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

    public List<StatusDistribution> getStatusDistributions() {
        return statusDistributions;
    }

    public void setStatusDistributions(List<StatusDistribution> statusDistributions) {
        this.statusDistributions = statusDistributions != null ? statusDistributions : new ArrayList<>();
    }

    public List<MonthlyApplicationData> getMonthlyApplicationTrends() {
        return monthlyApplicationTrends;
    }

    public void setMonthlyApplicationTrends(List<MonthlyApplicationData> monthlyApplicationTrends) {
        this.monthlyApplicationTrends = monthlyApplicationTrends != null ? monthlyApplicationTrends : new ArrayList<>();
    }

    public List<CompanyApplicationData> getCompanyApplicationData() {
        return companyApplicationData;
    }

    public void setCompanyApplicationData(List<CompanyApplicationData> companyApplicationData) {
        this.companyApplicationData = companyApplicationData != null ? companyApplicationData : new ArrayList<>();
    }

    public InterviewAnalytics getInterviewAnalytics() {
        return interviewAnalytics;
    }

    public void setInterviewAnalytics(InterviewAnalytics interviewAnalytics) {
        this.interviewAnalytics = interviewAnalytics != null ? interviewAnalytics : new InterviewAnalytics();
    }

    public KeyInsights getKeyInsights() {
        return keyInsights;
    }

    public void setKeyInsights(KeyInsights keyInsights) {
        this.keyInsights = keyInsights != null ? keyInsights : new KeyInsights();
    }

    @Override
    public String toString() {
        return "AnalyticsSummary{" +
                "dateRange=" + dateRangeFilter +
                ", totalApplications=" + totalApplications +
                ", activeApplications=" + activeApplications +
                ", totalInterviews=" + totalInterviews +
                ", offersReceived=" + offersReceived +
                ", rejectedApplications=" + rejectedApplications +
                ", conversionRate=" + interviewConversionRate +
                '}';
    }
}

