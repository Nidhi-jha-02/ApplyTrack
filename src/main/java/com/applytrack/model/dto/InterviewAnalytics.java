package com.applytrack.model.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object encapsulating interview pipeline analytics, outcome distributions,
 * and monthly progression trends.
 *
 * Demonstrates:
 * - OOP: Data Transfer Object (DTO) pattern (Unit 3)
 * - Collections: Map and List structures for chart bindings (Unit 3)
 */
public class InterviewAnalytics {

    private int totalInterviews;
    private int distinctApplicationsInterviewed;
    private int upcomingInterviewsCount;
    private int completedInterviewsCount;
    private double conversionRate; // (Distinct Apps with interview / Total Apps) * 100
    private Map<String, Integer> outcomeDistribution = new LinkedHashMap<>();
    private List<MonthlyApplicationData> monthlyInterviewTrends = new ArrayList<>();

    public InterviewAnalytics() {
    }

    public int getTotalInterviews() {
        return totalInterviews;
    }

    public void setTotalInterviews(int totalInterviews) {
        this.totalInterviews = totalInterviews;
    }

    public int getDistinctApplicationsInterviewed() {
        return distinctApplicationsInterviewed;
    }

    public void setDistinctApplicationsInterviewed(int distinctApplicationsInterviewed) {
        this.distinctApplicationsInterviewed = distinctApplicationsInterviewed;
    }

    public int getUpcomingInterviewsCount() {
        return upcomingInterviewsCount;
    }

    public void setUpcomingInterviewsCount(int upcomingInterviewsCount) {
        this.upcomingInterviewsCount = upcomingInterviewsCount;
    }

    public int getCompletedInterviewsCount() {
        return completedInterviewsCount;
    }

    public void setCompletedInterviewsCount(int completedInterviewsCount) {
        this.completedInterviewsCount = completedInterviewsCount;
    }

    public double getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(double conversionRate) {
        this.conversionRate = Math.round(conversionRate * 10.0) / 10.0;
    }

    public String getFormattedConversionRate() {
        return String.format("%.1f%%", conversionRate);
    }

    public Map<String, Integer> getOutcomeDistribution() {
        return outcomeDistribution;
    }

    public void setOutcomeDistribution(Map<String, Integer> outcomeDistribution) {
        this.outcomeDistribution = outcomeDistribution != null ? outcomeDistribution : new LinkedHashMap<>();
    }

    public List<MonthlyApplicationData> getMonthlyInterviewTrends() {
        return monthlyInterviewTrends;
    }

    public void setMonthlyInterviewTrends(List<MonthlyApplicationData> monthlyInterviewTrends) {
        this.monthlyInterviewTrends = monthlyInterviewTrends != null ? monthlyInterviewTrends : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "InterviewAnalytics{" +
                "totalInterviews=" + totalInterviews +
                ", distinctApplicationsInterviewed=" + distinctApplicationsInterviewed +
                ", upcomingInterviewsCount=" + upcomingInterviewsCount +
                ", conversionRate=" + conversionRate +
                '}';
    }
}

