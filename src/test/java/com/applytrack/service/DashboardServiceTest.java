package com.applytrack.service;

import com.applytrack.model.dto.DashboardSummary;
import com.applytrack.model.dto.DeadlineSummaryItem;
import com.applytrack.model.dto.InterviewSummaryItem;
import com.applytrack.repository.DashboardRepository;
import org.junit.jupiter.api.BeforeEach;
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

@DisplayName("Phase 5: DashboardService Unit Tests")
class DashboardServiceTest {

    private StubDashboardRepository stubRepo;
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        stubRepo = new StubDashboardRepository();
        dashboardService = new DashboardService(stubRepo);
    }

    @Test
    @DisplayName("getDashboardSummary aggregates counts and formats month labels correctly")
    void testGetDashboardSummarySuccessful() throws SQLException {
        stubRepo.total = 6;
        stubRepo.inProgress = 4;
        stubRepo.interviews = 1;
        stubRepo.offers = 1;
        stubRepo.rejected = 1;

        stubRepo.statusDistribution.put("Applied", 1);
        stubRepo.statusDistribution.put("Under Review", 1);
        stubRepo.statusDistribution.put("Shortlisted", 1);
        stubRepo.statusDistribution.put("Interview Scheduled", 1);
        stubRepo.statusDistribution.put("Selected", 1);
        stubRepo.statusDistribution.put("Rejected", 1);

        stubRepo.monthlyTrends.put("2026-09", 1);
        stubRepo.monthlyTrends.put("2026-10", 3);
        stubRepo.monthlyTrends.put("2026-11", 2);

        LocalDate now = LocalDate.now();
        stubRepo.deadlines.add(new DeadlineSummaryItem(1, "Microsoft", "Intern", now.plusDays(5), 5, "Under Review"));
        stubRepo.interviewsList.add(new InterviewSummaryItem(1, 1, "Google", "SDE", "Round 1",
                LocalDateTime.now().plusDays(2), "Online", "https://meet.google.com/test", "Interviewer", "Scheduled"));

        DashboardSummary summary = dashboardService.getDashboardSummary(1);

        assertNotNull(summary);
        assertEquals(6, summary.getTotalApplications());
        assertEquals(4, summary.getInProgressApplications());
        assertEquals(1, summary.getScheduledInterviews());
        assertEquals(1, summary.getOffersCount());
        assertEquals(1, summary.getRejectedCount());
        assertEquals(6, summary.getStatusDistribution().size());

        // Verify month formatting ("2026-09" -> "Sep 2026", "2026-10" -> "Oct 2026")
        Map<String, Integer> trends = summary.getMonthlyTrends();
        assertTrue(trends.containsKey("Sep 2026"));
        assertEquals(1, trends.get("Sep 2026"));
        assertTrue(trends.containsKey("Oct 2026"));
        assertEquals(3, trends.get("Oct 2026"));
        assertTrue(trends.containsKey("Nov 2026"));
        assertEquals(2, trends.get("Nov 2026"));

        assertEquals(1, summary.getUpcomingDeadlines().size());
        assertEquals(1, summary.getUpcomingInterviews().size());
    }

    @Test
    @DisplayName("getDashboardSummary returns empty summary when userId <= 0")
    void testGetDashboardSummaryInvalidUser() throws SQLException {
        DashboardSummary summary = dashboardService.getDashboardSummary(0);
        assertNotNull(summary);
        assertEquals(0, summary.getTotalApplications());
        assertFalse(summary.hasApplications());

        DashboardSummary negativeUserSummary = dashboardService.getDashboardSummary(-5);
        assertNotNull(negativeUserSummary);
        assertEquals(0, negativeUserSummary.getTotalApplications());
    }

    @Test
    @DisplayName("formatMonthKey transforms ISO YearMonth strings safely")
    void testFormatMonthKey() {
        assertEquals("Oct 2026", DashboardService.formatMonthKey("2026-10"));
        assertEquals("Jan 2025", DashboardService.formatMonthKey("2025-01"));
        assertEquals("N/A", DashboardService.formatMonthKey(null));
        assertEquals("N/A", DashboardService.formatMonthKey("   "));
        // Invalid or fallback string
        assertEquals("InvalidDate", DashboardService.formatMonthKey("InvalidDate"));
    }

    @Test
    @DisplayName("getDashboardSummary returns empty collections when user has no applications")
    void testEmptyUserApplications() throws SQLException {
        DashboardSummary summary = dashboardService.getDashboardSummary(999);
        assertNotNull(summary);
        assertEquals(0, summary.getTotalApplications());
        assertEquals(0, summary.getInProgressApplications());
        assertEquals(0, summary.getOffersCount());
        assertEquals(0, summary.getRejectedCount());
        assertEquals(0.0, summary.getOfferRate());
        assertTrue(summary.getStatusDistribution().isEmpty());
        assertTrue(summary.getMonthlyTrends().isEmpty());
        assertTrue(summary.getUpcomingDeadlines().isEmpty());
        assertTrue(summary.getUpcomingInterviews().isEmpty());
    }

    // --- In-Memory Test Stub Implementation of DashboardRepository ---
    private static class StubDashboardRepository implements DashboardRepository {
        int total = 0;
        int inProgress = 0;
        int interviews = 0;
        int offers = 0;
        int rejected = 0;
        final Map<String, Integer> statusDistribution = new LinkedHashMap<>();
        final Map<String, Integer> monthlyTrends = new LinkedHashMap<>();
        final List<DeadlineSummaryItem> deadlines = new ArrayList<>();
        final List<InterviewSummaryItem> interviewsList = new ArrayList<>();

        @Override
        public int countTotalApplications(int userId) {
            return userId == 999 ? 0 : total;
        }

        @Override
        public int countInProgressApplications(int userId) {
            return userId == 999 ? 0 : inProgress;
        }

        @Override
        public int countScheduledInterviews(int userId) {
            return userId == 999 ? 0 : interviews;
        }

        @Override
        public int countOffers(int userId) {
            return userId == 999 ? 0 : offers;
        }

        @Override
        public int countRejected(int userId) {
            return userId == 999 ? 0 : rejected;
        }

        @Override
        public Map<String, Integer> getStatusDistribution(int userId) {
            return userId == 999 ? Collections.emptyMap() : statusDistribution;
        }

        @Override
        public Map<String, Integer> getMonthlyTrends(int userId) {
            return userId == 999 ? Collections.emptyMap() : monthlyTrends;
        }

        @Override
        public List<DeadlineSummaryItem> getUpcomingDeadlines(int userId, int limit) {
            return userId == 999 ? Collections.emptyList() : deadlines;
        }

        @Override
        public List<InterviewSummaryItem> getUpcomingInterviews(int userId, int limit) {
            return userId == 999 ? Collections.emptyList() : interviewsList;
        }
    }
}
