package com.applytrack.model.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 5: Dashboard DTO & Models Unit Tests")
class DashboardSummaryTest {

    @Test
    @DisplayName("DashboardSummary properly aggregates metric counters")
    void testDashboardSummaryMetrics() {
        Map<String, Integer> statusMap = new LinkedHashMap<>();
        statusMap.put("Applied", 3);
        statusMap.put("Selected", 1);

        Map<String, Integer> trendsMap = new LinkedHashMap<>();
        trendsMap.put("Nov 2026", 4);

        DashboardSummary summary = new DashboardSummary(
                5, 3, 2, 1, 1,
                statusMap, trendsMap, Collections.emptyList(), Collections.emptyList()
        );

        assertEquals(5, summary.getTotalApplications());
        assertEquals(3, summary.getInProgressApplications());
        assertEquals(2, summary.getScheduledInterviews());
        assertEquals(1, summary.getOffersCount());
        assertEquals(1, summary.getRejectedCount());
        assertEquals(20.0, summary.getOfferRate(), 0.01);
        assertTrue(summary.hasApplications());
        assertFalse(summary.hasUpcomingDeadlines());
        assertFalse(summary.hasUpcomingInterviews());
        assertNotNull(summary.getGeneratedAt());
        assertNotNull(summary.toString());
    }

    @Test
    @DisplayName("Empty DashboardSummary handles zeros and empty collections safely")
    void testEmptyDashboardSummary() {
        DashboardSummary empty = DashboardSummary.empty();

        assertEquals(0, empty.getTotalApplications());
        assertEquals(0, empty.getInProgressApplications());
        assertEquals(0, empty.getScheduledInterviews());
        assertEquals(0, empty.getOffersCount());
        assertEquals(0, empty.getRejectedCount());
        assertEquals(0.0, empty.getOfferRate(), 0.001);
        assertFalse(empty.hasApplications());
        assertTrue(empty.getStatusDistribution().isEmpty());
        assertTrue(empty.getMonthlyTrends().isEmpty());
        assertTrue(empty.getUpcomingDeadlines().isEmpty());
        assertTrue(empty.getUpcomingInterviews().isEmpty());
    }

    @Test
    @DisplayName("DeadlineSummaryItem correctly assigns urgency levels based on days remaining")
    void testDeadlineSummaryUrgency() {
        LocalDate today = LocalDate.now();

        DeadlineSummaryItem overdue = new DeadlineSummaryItem(1, "Google", "SDE", today.minusDays(2), -2, "Applied");
        assertEquals(DeadlineSummaryItem.Urgency.OVERDUE, overdue.getUrgency());
        assertTrue(overdue.getFormattedRemaining().contains("overdue"));

        DeadlineSummaryItem urgent = new DeadlineSummaryItem(2, "Microsoft", "Intern", today.plusDays(2), 2, "Under Review");
        assertEquals(DeadlineSummaryItem.Urgency.URGENT_3_DAYS, urgent.getUrgency());
        assertEquals("2 days remaining", urgent.getFormattedRemaining());

        DeadlineSummaryItem soon = new DeadlineSummaryItem(3, "Atlassian", "Associate", today.plusDays(6), 6, "Shortlisted");
        assertEquals(DeadlineSummaryItem.Urgency.SOON_7_DAYS, soon.getUrgency());

        DeadlineSummaryItem upcoming = new DeadlineSummaryItem(4, "Amazon", "SDE II", today.plusDays(15), 15, "Applied");
        assertEquals(DeadlineSummaryItem.Urgency.UPCOMING, upcoming.getUrgency());

        DeadlineSummaryItem dueToday = new DeadlineSummaryItem(5, "Meta", "SWE", today, 0, "Applied");
        assertEquals("Due today!", dueToday.getFormattedRemaining());
    }

    @Test
    @DisplayName("DeadlineSummaryItem Comparable sorts chronologically (nearest deadline first)")
    void testDeadlineSorting() {
        LocalDate today = LocalDate.now();
        List<DeadlineSummaryItem> list = new ArrayList<>();
        list.add(new DeadlineSummaryItem(1, "Company C", "Role", today.plusDays(10), 10, "Applied"));
        list.add(new DeadlineSummaryItem(2, "Company A", "Role", today.plusDays(2), 2, "Applied"));
        list.add(new DeadlineSummaryItem(3, "Company B", "Role", today.plusDays(5), 5, "Applied"));

        Collections.sort(list);

        assertEquals("Company A", list.get(0).getCompanyName());
        assertEquals("Company B", list.get(1).getCompanyName());
        assertEquals("Company C", list.get(2).getCompanyName());
    }

    @Test
    @DisplayName("InterviewSummaryItem formats date/time and sorts chronologically")
    void testInterviewSummaryItem() {
        LocalDateTime d1 = LocalDateTime.of(2026, 11, 12, 18, 30);
        LocalDateTime d2 = LocalDateTime.of(2026, 11, 15, 10, 0);

        InterviewSummaryItem item1 = new InterviewSummaryItem(1, 10, "Google", "SDE", "Round 1",
                d1, "Online", "https://meet.google.com/xyz", "David Chen", "Scheduled");

        InterviewSummaryItem item2 = new InterviewSummaryItem(2, 11, "Amazon", "SDE I", "Bar Raiser",
                d2, "Online", null, "Marcus Vance", "Scheduled");

        assertEquals("Google", item1.getCompanyName());
        assertEquals("Round 1", item1.getRoundName());
        assertTrue(item1.getFormattedDateTime().contains("Nov 12, 2026"));
        assertTrue(item1.getFormattedDateTime().contains("18:30"));
        assertEquals("Nov 12, 2026", item1.getFormattedDate());
        assertEquals("18:30 IST", item1.getFormattedTime());

        // Comparable verification
        assertTrue(item1.compareTo(item2) < 0);
    }
}

