package com.applytrack.model.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object (DTO) aggregating all metrics, charts data, deadlines,
 * and upcoming interviews for the active user dashboard.
 *
 * Demonstrates:
 * - OOP: Encapsulation & Separation of Concerns (Unit 3)
 * - Collections Framework: Map (LinkedHashMap), List (ArrayList) - Unit 3
 * - Generics: Parameterized collections <String, Integer>, <DeadlineSummaryItem> (Unit 3)
 * - Immutability via unmodifiable collections and defensive copying (Unit 2 & 3)
 */
public class DashboardSummary {

    private final int totalApplications;
    private final int inProgressApplications;
    private final int scheduledInterviews;
    private final int offersCount;
    private final int rejectedCount;

    private final Map<String, Integer> statusDistribution;
    private final Map<String, Integer> monthlyTrends;
    private final List<DeadlineSummaryItem> upcomingDeadlines;
    private final List<InterviewSummaryItem> upcomingInterviews;
    private final LocalDateTime generatedAt;

    public DashboardSummary(int totalApplications,
                            int inProgressApplications,
                            int scheduledInterviews,
                            int offersCount,
                            int rejectedCount,
                            Map<String, Integer> statusDistribution,
                            Map<String, Integer> monthlyTrends,
                            List<DeadlineSummaryItem> upcomingDeadlines,
                            List<InterviewSummaryItem> upcomingInterviews) {
        this.totalApplications = totalApplications;
        this.inProgressApplications = inProgressApplications;
        this.scheduledInterviews = scheduledInterviews;
        this.offersCount = offersCount;
        this.rejectedCount = rejectedCount;
        this.statusDistribution = statusDistribution != null ? new LinkedHashMap<>(statusDistribution) : new LinkedHashMap<>();
        this.monthlyTrends = monthlyTrends != null ? new LinkedHashMap<>(monthlyTrends) : new LinkedHashMap<>();
        this.upcomingDeadlines = upcomingDeadlines != null ? new ArrayList<>(upcomingDeadlines) : new ArrayList<>();
        this.upcomingInterviews = upcomingInterviews != null ? new ArrayList<>(upcomingInterviews) : new ArrayList<>();
        this.generatedAt = LocalDateTime.now();
    }

    public static DashboardSummary empty() {
        return new DashboardSummary(0, 0, 0, 0, 0,
                Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptyList(), Collections.emptyList());
    }

    public int getTotalApplications() {
        return totalApplications;
    }

    public int getInProgressApplications() {
        return inProgressApplications;
    }

    public int getScheduledInterviews() {
        return scheduledInterviews;
    }

    public int getOffersCount() {
        return offersCount;
    }

    public int getRejectedCount() {
        return rejectedCount;
    }

    public Map<String, Integer> getStatusDistribution() {
        return Collections.unmodifiableMap(statusDistribution);
    }

    public Map<String, Integer> getMonthlyTrends() {
        return Collections.unmodifiableMap(monthlyTrends);
    }

    public List<DeadlineSummaryItem> getUpcomingDeadlines() {
        return Collections.unmodifiableList(upcomingDeadlines);
    }

    public List<InterviewSummaryItem> getUpcomingInterviews() {
        return Collections.unmodifiableList(upcomingInterviews);
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public boolean hasApplications() {
        return totalApplications > 0;
    }

    public boolean hasUpcomingDeadlines() {
        return !upcomingDeadlines.isEmpty();
    }

    public boolean hasUpcomingInterviews() {
        return !upcomingInterviews.isEmpty();
    }

    /**
     * Calculates the offer conversion rate as a percentage.
     * Demonstrates floating point arithmetic and rounding (Unit 1 & 2).
     */
    public double getOfferRate() {
        if (totalApplications == 0) return 0.0;
        return ((double) offersCount / totalApplications) * 100.0;
    }

    @Override
    public String toString() {
        return "DashboardSummary{" +
                "totalApplications=" + totalApplications +
                ", inProgressApplications=" + inProgressApplications +
                ", scheduledInterviews=" + scheduledInterviews +
                ", offersCount=" + offersCount +
                ", rejectedCount=" + rejectedCount +
                ", statusDistributionSize=" + statusDistribution.size() +
                ", upcomingDeadlinesSize=" + upcomingDeadlines.size() +
                ", upcomingInterviewsSize=" + upcomingInterviews.size() +
                '}';
    }
}

