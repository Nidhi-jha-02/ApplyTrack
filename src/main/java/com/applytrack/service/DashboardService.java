package com.applytrack.service;

import com.applytrack.model.dto.DashboardSummary;
import com.applytrack.model.dto.DeadlineSummaryItem;
import com.applytrack.model.dto.InterviewSummaryItem;
import com.applytrack.repository.DashboardRepository;
import com.applytrack.repository.impl.JdbcDashboardRepository;

import java.sql.SQLException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Service orchestrating dashboard analytics, data aggregation, and view formatting.
 *
 * Demonstrates:
 * - OOP: Layered Architecture & Separation of Concerns (Unit 3)
 * - Dependency Injection: Constructor injection for testability with mocks (Unit 3)
 * - Collections Framework: LinkedHashMap for ordered aggregation (Unit 3)
 * - Exception Handling: Checked SQLException propagation (Unit 3)
 * - Modern Java Date/Time: YearMonth formatting (Unit 1 & 2)
 */
public class DashboardService {

    private static final DateTimeFormatter MONTH_DISPLAY_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US);
    private static final int DEFAULT_DEADLINES_LIMIT = 5;
    private static final int DEFAULT_INTERVIEWS_LIMIT = 5;

    private final DashboardRepository dashboardRepository;

    public DashboardService() {
        this(new JdbcDashboardRepository());
    }

    public DashboardService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = Objects.requireNonNull(dashboardRepository, "DashboardRepository must not be null");
    }

    /**
     * Retrieves aggregated dashboard data strictly for the specified authenticated user.
     *
     * @param userId Authenticated user primary key
     * @return DashboardSummary containing counters, charts data, and upcoming activities
     * @throws SQLException If database retrieval encounters an error
     */
    public DashboardSummary getDashboardSummary(int userId) throws SQLException {
        if (userId <= 0) {
            return DashboardSummary.empty();
        }

        int totalApps = dashboardRepository.countTotalApplications(userId);
        int inProgress = dashboardRepository.countInProgressApplications(userId);
        int interviews = dashboardRepository.countScheduledInterviews(userId);
        int offers = dashboardRepository.countOffers(userId);
        int rejected = dashboardRepository.countRejected(userId);

        Map<String, Integer> rawStatus = dashboardRepository.getStatusDistribution(userId);
        Map<String, Integer> rawTrends = dashboardRepository.getMonthlyTrends(userId);

        // Format month keys into readable format: "2026-10" -> "Oct 2026"
        Map<String, Integer> formattedTrends = new LinkedHashMap<>();
        if (rawTrends != null) {
            for (Map.Entry<String, Integer> entry : rawTrends.entrySet()) {
                formattedTrends.put(formatMonthKey(entry.getKey()), entry.getValue());
            }
        }

        List<DeadlineSummaryItem> deadlines = dashboardRepository.getUpcomingDeadlines(userId, DEFAULT_DEADLINES_LIMIT);
        List<InterviewSummaryItem> upcomingInterviews = dashboardRepository.getUpcomingInterviews(userId, DEFAULT_INTERVIEWS_LIMIT);

        return new DashboardSummary(
                totalApps,
                inProgress,
                interviews,
                offers,
                rejected,
                rawStatus != null ? rawStatus : Collections.emptyMap(),
                formattedTrends,
                deadlines != null ? deadlines : Collections.emptyList(),
                upcomingInterviews != null ? upcomingInterviews : Collections.emptyList()
        );
    }

    /**
     * Converts a database month key (e.g. "2026-10") into a human-friendly label (e.g. "Oct 2026").
     * Demonstrates string manipulation and exception-safe parsing (Unit 2 & 3).
     */
    public static String formatMonthKey(String rawMonthKey) {
        if (rawMonthKey == null || rawMonthKey.isBlank()) {
            return "N/A";
        }
        try {
            YearMonth ym = YearMonth.parse(rawMonthKey);
            return ym.format(MONTH_DISPLAY_FORMAT);
        } catch (Exception e) {
            return rawMonthKey;
        }
    }
}
