package com.applytrack.service;

import com.applytrack.model.dto.DashboardSummary;
import com.applytrack.model.dto.DeadlineSummaryItem;
import com.applytrack.model.dto.InterviewSummaryItem;
import com.applytrack.repository.DashboardRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 5: Dashboard Refresh & Dynamic Invalidation Tests")
class DashboardRefreshTest {

    @Test
    @DisplayName("Repeated dashboard refreshes reflect updated database metrics without accumulating stale data")
    void testDashboardRefreshBehavior() throws SQLException {
        MutableTestRepository repo = new MutableTestRepository();
        DashboardService service = new DashboardService(repo);

        // Initial snapshot
        repo.total = 5;
        repo.offers = 1;
        DashboardSummary initialSummary = service.getDashboardSummary(1);
        assertEquals(5, initialSummary.getTotalApplications());
        assertEquals(1, initialSummary.getOffersCount());
        assertEquals(20.0, initialSummary.getOfferRate(), 0.01);

        // Simulate database state change (e.g. user submitted a new application and received an offer)
        repo.total = 7;
        repo.offers = 2;
        repo.statusMap.put("Selected", 2);

        // Refresh snapshot
        DashboardSummary refreshedSummary = service.getDashboardSummary(1);
        assertEquals(7, refreshedSummary.getTotalApplications());
        assertEquals(2, refreshedSummary.getOffersCount());
        assertEquals(28.57, refreshedSummary.getOfferRate(), 0.01);

        // Verify independent instance creation (no shared mutation or stale accumulation)
        assertNotSame(initialSummary, refreshedSummary);
        assertEquals(5, initialSummary.getTotalApplications());
        assertEquals(7, refreshedSummary.getTotalApplications());
    }

    private static class MutableTestRepository implements DashboardRepository {
        int total = 0;
        int inProgress = 0;
        int interviews = 0;
        int offers = 0;
        int rejected = 0;
        final Map<String, Integer> statusMap = new LinkedHashMap<>();
        final Map<String, Integer> trendsMap = new LinkedHashMap<>();
        final List<DeadlineSummaryItem> deadlinesList = new ArrayList<>();
        final List<InterviewSummaryItem> interviewsList = new ArrayList<>();

        @Override public int countTotalApplications(int userId) { return total; }
        @Override public int countInProgressApplications(int userId) { return inProgress; }
        @Override public int countScheduledInterviews(int userId) { return interviews; }
        @Override public int countOffers(int userId) { return offers; }
        @Override public int countRejected(int userId) { return rejected; }
        @Override public Map<String, Integer> getStatusDistribution(int userId) { return new LinkedHashMap<>(statusMap); }
        @Override public Map<String, Integer> getMonthlyTrends(int userId) { return new LinkedHashMap<>(trendsMap); }
        @Override public List<DeadlineSummaryItem> getUpcomingDeadlines(int userId, int limit) { return new ArrayList<>(deadlinesList); }
        @Override public List<InterviewSummaryItem> getUpcomingInterviews(int userId, int limit) { return new ArrayList<>(interviewsList); }
    }
}

