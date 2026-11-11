package com.applytrack.service;

import com.applytrack.model.dto.AnalyticsSummary;
import com.applytrack.model.dto.CompanyApplicationData;
import com.applytrack.model.dto.InterviewAnalytics;
import com.applytrack.model.dto.KeyInsights;
import com.applytrack.model.dto.MonthlyApplicationData;
import com.applytrack.model.dto.StatusDistribution;
import com.applytrack.model.enums.ApplicationStatus;
import com.applytrack.model.enums.DateRangeFilter;
import com.applytrack.repository.AnalyticsRepository;
import com.applytrack.repository.impl.JdbcAnalyticsRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Service layer coordinating career analytics calculations, conversion funnels,
 * date range filtering, and metric packaging for JavaFX charts.
 *
 * Demonstrates:
 * - Layered Architecture: UI -> Service -> Repository (Unit 3)
 * - Constructor Overloading & Dependency Injection for Unit Testing (Unit 2 & Unit 3)
 * - Safe numeric operations: division-by-zero protection and rounding (Unit 1)
 * - Stream API & Collections transformations (Unit 3)
 */
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    /**
     * Default constructor utilizing concrete JDBC MariaDB repository.
     */
    public AnalyticsService() {
        this(new JdbcAnalyticsRepository());
    }

    /**
     * Overloaded constructor supporting repository stub injection for unit tests.
     */
    public AnalyticsService(AnalyticsRepository analyticsRepository) {
        if (analyticsRepository == null) {
            throw new IllegalArgumentException("AnalyticsRepository cannot be null.");
        }
        this.analyticsRepository = analyticsRepository;
    }

    /**
     * Generates a complete AnalyticsSummary master DTO for the user under the specified date filter.
     */
    public AnalyticsSummary getAnalyticsSummary(int userId, DateRangeFilter filter) throws SQLException {
        if (filter == null) {
            filter = DateRangeFilter.ALL_TIME;
        }

        LocalDate startDate = filter.getStartDate();
        AnalyticsSummary summary = new AnalyticsSummary();
        summary.setDateRangeFilter(filter);

        // Core aggregate counts
        int totalApps = analyticsRepository.countTotalApplications(userId, startDate);
        int activeApps = analyticsRepository.countActiveApplications(userId, startDate);
        int offers = analyticsRepository.countOffers(userId, startDate);
        int rejected = analyticsRepository.countRejected(userId, startDate);
        int totalInterviews = analyticsRepository.countTotalInterviews(userId, startDate);
        int distinctInterviewed = analyticsRepository.countDistinctApplicationsInterviewed(userId, startDate);
        int upcomingInterviews = analyticsRepository.countUpcomingInterviews(userId);
        int completedInterviews = analyticsRepository.countCompletedInterviews(userId, startDate);

        summary.setTotalApplications(totalApps);
        summary.setActiveApplications(activeApps);
        summary.setOffersReceived(offers);
        summary.setRejectedApplications(rejected);
        summary.setTotalInterviews(totalInterviews);
        summary.setDistinctApplicationsInterviewed(distinctInterviewed);

        // Rates
        double conversionRate = calculateConversionRate(distinctInterviewed, totalApps);
        double offerRate = calculateOfferRate(offers, totalApps);
        summary.setInterviewConversionRate(conversionRate);
        summary.setOfferRate(offerRate);

        // Status Distribution
        Map<String, Integer> rawStatusCounts = analyticsRepository.getStatusCounts(userId, startDate);
        List<StatusDistribution> statusDistributions = processStatusDistributions(rawStatusCounts, totalApps);
        summary.setStatusDistributions(statusDistributions);

        // Monthly Trends
        List<MonthlyApplicationData> monthlyAppTrends = analyticsRepository.getMonthlyApplicationCounts(userId, startDate);
        Collections.sort(monthlyAppTrends);
        summary.setMonthlyApplicationTrends(monthlyAppTrends);

        // Company Application Ranking (Top 10)
        List<CompanyApplicationData> companyData = analyticsRepository.getCompanyApplicationCounts(userId, startDate, 10);
        Collections.sort(companyData);
        summary.setCompanyApplicationData(companyData);

        // Interview Analytics DTO
        InterviewAnalytics interviewAnalytics = new InterviewAnalytics();
        interviewAnalytics.setTotalInterviews(totalInterviews);
        interviewAnalytics.setDistinctApplicationsInterviewed(distinctInterviewed);
        interviewAnalytics.setUpcomingInterviewsCount(upcomingInterviews);
        interviewAnalytics.setCompletedInterviewsCount(completedInterviews);
        interviewAnalytics.setConversionRate(conversionRate);
        interviewAnalytics.setOutcomeDistribution(analyticsRepository.getInterviewOutcomeCounts(userId, startDate));
        List<MonthlyApplicationData> monthlyInterviewTrends = analyticsRepository.getMonthlyInterviewCounts(userId, startDate);
        Collections.sort(monthlyInterviewTrends);
        interviewAnalytics.setMonthlyInterviewTrends(monthlyInterviewTrends);
        summary.setInterviewAnalytics(interviewAnalytics);

        // Key Insights
        KeyInsights keyInsights = generateKeyInsights(totalApps, activeApps, offers, totalInterviews,
                completedInterviews, companyData, monthlyAppTrends, conversionRate, offerRate);
        summary.setKeyInsights(keyInsights);

        return summary;
    }

    /**
     * Calculates the interview conversion rate as a percentage rounded to 1 decimal place.
     * Formula: (Distinct Applications Interviewed / Total Applications) * 100
     */
    public double calculateConversionRate(int distinctInterviewed, int totalApplications) {
        if (totalApplications <= 0 || distinctInterviewed <= 0) {
            return 0.0;
        }
        double rate = ((double) distinctInterviewed / totalApplications) * 100.0;
        return Math.round(rate * 10.0) / 10.0;
    }

    /**
     * Calculates the offer conversion rate as a percentage rounded to 1 decimal place.
     * Formula: (Offers Received / Total Applications) * 100
     */
    public double calculateOfferRate(int offersReceived, int totalApplications) {
        if (totalApplications <= 0 || offersReceived <= 0) {
            return 0.0;
        }
        double rate = ((double) offersReceived / totalApplications) * 100.0;
        return Math.round(rate * 10.0) / 10.0;
    }

    /**
     * Transforms raw database status counts into enriched StatusDistribution objects with percentages.
     */
    public List<StatusDistribution> processStatusDistributions(Map<String, Integer> rawStatusCounts, int totalApplications) {
        List<StatusDistribution> result = new ArrayList<>();
        if (rawStatusCounts == null || rawStatusCounts.isEmpty()) {
            return result;
        }

        for (Map.Entry<String, Integer> entry : rawStatusCounts.entrySet()) {
            String status = entry.getKey();
            int count = entry.getValue() != null ? entry.getValue() : 0;
            double percentage = totalApplications > 0 ? ((double) count / totalApplications) * 100.0 : 0.0;
            String badgeColor = ApplicationStatus.fromString(status).getBadgeColor();
            result.add(new StatusDistribution(status, count, percentage, badgeColor));
        }

        Collections.sort(result);
        return result;
    }

    /**
     * Computes high-level takeaways and actionable observations based on real metrics.
     */
    public KeyInsights generateKeyInsights(int totalApps, int activeApps, int offers, int totalInterviews,
                                           int completedInterviews, List<CompanyApplicationData> companyData,
                                           List<MonthlyApplicationData> monthlyTrends, double conversionRate, double offerRate) {
        KeyInsights insights = new KeyInsights();

        if (totalApps == 0) {
            insights.setHasData(false);
            insights.setSummaryText("No application data available for the selected timeframe. Submit new applications to generate career intelligence.");
            return insights;
        }

        insights.setHasData(true);
        insights.setPendingDecisions(activeApps);
        insights.setCompletedInterviews(completedInterviews);
        insights.setOfferRate(offerRate);

        // Top company
        if (companyData != null && !companyData.isEmpty()) {
            CompanyApplicationData top = companyData.get(0);
            insights.setTopCompany(top.getCompanyName());
            insights.setTopCompanyCount(top.getApplicationCount());
        }

        // Peak month
        if (monthlyTrends != null && !monthlyTrends.isEmpty()) {
            MonthlyApplicationData peak = monthlyTrends.get(0);
            for (MonthlyApplicationData data : monthlyTrends) {
                if (data.getCount() > peak.getCount()) {
                    peak = data;
                }
            }
            insights.setPeakMonth(peak.getDisplayLabel());
            insights.setPeakMonthCount(peak.getCount());
        }

        // Dynamic summary narrative
        StringBuilder sb = new StringBuilder();
        if (offers > 0) {
            sb.append(String.format("Congratulations! You have secured %d job offer%s (%s offer rate). ",
                    offers, offers > 1 ? "s" : "", String.format("%.1f%%", offerRate)));
        } else if (conversionRate > 0) {
            sb.append(String.format("Healthy pipeline: %.1f%% of your applications have converted into interview rounds. ", conversionRate));
        } else {
            sb.append("Applications submitted. Follow up with recruiters to accelerate interview conversions. ");
        }

        if (activeApps > 0) {
            sb.append(String.format("%d application%s currently in progress.", activeApps, activeApps > 1 ? "s are" : " is"));
        } else {
            sb.append("All applications in this period have reached terminal outcomes.");
        }

        insights.setSummaryText(sb.toString());
        return insights;
    }
}

