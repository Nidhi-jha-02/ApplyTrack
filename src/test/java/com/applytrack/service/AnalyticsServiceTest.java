package com.applytrack.service;

import com.applytrack.model.dto.AnalyticsSummary;
import com.applytrack.model.dto.CompanyApplicationData;
import com.applytrack.model.dto.KeyInsights;
import com.applytrack.model.dto.MonthlyApplicationData;
import com.applytrack.model.dto.StatusDistribution;
import com.applytrack.model.enums.DateRangeFilter;
import com.applytrack.repository.AnalyticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Unit Tests for AnalyticsService and analytics calculation logic.
 *
 * Demonstrates:
 * - Unit Testing with Mock/Stub Repository (Unit 3)
 * - Safe mathematical calculations: division by zero protection and percentage rounding (Unit 1)
 * - Comparable sorting across multi-year trends and rankings (Unit 2)
 * - Date Range Filter logic verification (Unit 1)
 */
public class AnalyticsServiceTest {

    private StubAnalyticsRepository stubRepo;
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        stubRepo = new StubAnalyticsRepository();
        analyticsService = new AnalyticsService(stubRepo);
    }

    @Test
    @DisplayName("Conversion rate calculation with standard values")
    void testCalculateConversionRateNormal() {
        // 2 distinct interviewed applications out of 6 total = 33.333% -> 33.3%
        double rate = analyticsService.calculateConversionRate(2, 6);
        assertEquals(33.3, rate, 0.01);
    }

    @Test
    @DisplayName("Conversion rate calculation handles zero applications safely (avoids divide-by-zero)")
    void testCalculateConversionRateZeroApplications() {
        double rate = analyticsService.calculateConversionRate(0, 0);
        assertEquals(0.0, rate);

        rate = analyticsService.calculateConversionRate(5, 0);
        assertEquals(0.0, rate);
    }

    @Test
    @DisplayName("Conversion rate calculation handles zero interviewed applications")
    void testCalculateConversionRateZeroInterviewed() {
        double rate = analyticsService.calculateConversionRate(0, 10);
        assertEquals(0.0, rate);
    }

    @Test
    @DisplayName("Offer rate calculation with standard values")
    void testCalculateOfferRateNormal() {
        // 1 offer out of 6 total applications = 16.666% -> 16.7%
        double rate = analyticsService.calculateOfferRate(1, 6);
        assertEquals(16.7, rate, 0.01);
    }

    @Test
    @DisplayName("Offer rate calculation handles zero applications safely")
    void testCalculateOfferRateZeroApplications() {
        double rate = analyticsService.calculateOfferRate(0, 0);
        assertEquals(0.0, rate);

        rate = analyticsService.calculateOfferRate(2, 0);
        assertEquals(0.0, rate);
    }

    @Test
    @DisplayName("Status distribution processing computes percentages and assigns badge colors")
    void testProcessStatusDistributionsNormal() {
        Map<String, Integer> raw = new LinkedHashMap<>();
        raw.put("Applied", 3);
        raw.put("Selected", 1);

        List<StatusDistribution> list = analyticsService.processStatusDistributions(raw, 4);
        assertNotNull(list);
        assertEquals(2, list.size());

        // Sorted descending by count
        StatusDistribution first = list.get(0);
        assertEquals("Applied", first.getStatusName());
        assertEquals(3, first.getCount());
        assertEquals(75.0, first.getPercentage());
        assertEquals("75.0%", first.getFormattedPercentage());
        assertNotNull(first.getBadgeColor());

        StatusDistribution second = list.get(1);
        assertEquals("Selected", second.getStatusName());
        assertEquals(1, second.getCount());
        assertEquals(25.0, second.getPercentage());
    }

    @Test
    @DisplayName("Status distribution processing handles empty raw map")
    void testProcessStatusDistributionsEmpty() {
        List<StatusDistribution> list = analyticsService.processStatusDistributions(new HashMap<>(), 0);
        assertNotNull(list);
        assertTrue(list.isEmpty());

        list = analyticsService.processStatusDistributions(null, 0);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("Generate key insights with realistic data")
    void testGenerateKeyInsightsWithRealData() {
        List<CompanyApplicationData> companies = List.of(
                new CompanyApplicationData("Google", 3),
                new CompanyApplicationData("Microsoft", 2)
        );
        List<MonthlyApplicationData> monthly = List.of(
                new MonthlyApplicationData("2026-09", 2),
                new MonthlyApplicationData("2026-10", 4),
                new MonthlyApplicationData("2026-11", 1)
        );

        KeyInsights insights = analyticsService.generateKeyInsights(
                7, 3, 1, 4, 3, companies, monthly, 42.9, 14.3
        );

        assertTrue(insights.isHasData());
        assertEquals("Google", insights.getTopCompany());
        assertEquals(3, insights.getTopCompanyCount());
        assertEquals("Oct 2026", insights.getPeakMonth());
        assertEquals(4, insights.getPeakMonthCount());
        assertEquals(3, insights.getCompletedInterviews());
        assertEquals(3, insights.getPendingDecisions());
        assertEquals(14.3, insights.getOfferRate());
        assertTrue(insights.getSummaryText().contains("job offer"));
    }

    @Test
    @DisplayName("Generate key insights handles empty dataset with graceful message")
    void testGenerateKeyInsightsEmptyData() {
        KeyInsights insights = analyticsService.generateKeyInsights(
                0, 0, 0, 0, 0, Collections.emptyList(), Collections.emptyList(), 0.0, 0.0
        );

        assertFalse(insights.isHasData());
        assertEquals("None", insights.getTopCompany());
        assertEquals("None", insights.getPeakMonth());
        assertTrue(insights.getSummaryText().contains("No application data available"));
    }

    @Test
    @DisplayName("Full AnalyticsSummary retrieval from repository stub")
    void testGetAnalyticsSummaryMasterDTO() throws SQLException {
        stubRepo.totalApps = 8;
        stubRepo.activeApps = 4;
        stubRepo.offers = 2;
        stubRepo.rejected = 1;
        stubRepo.totalInterviews = 6;
        stubRepo.distinctInterviewed = 4;

        AnalyticsSummary summary = analyticsService.getAnalyticsSummary(1, DateRangeFilter.ALL_TIME);

        assertNotNull(summary);
        assertEquals(8, summary.getTotalApplications());
        assertEquals(4, summary.getActiveApplications());
        assertEquals(2, summary.getOffersReceived());
        assertEquals(1, summary.getRejectedApplications());
        assertEquals(6, summary.getTotalInterviews());
        assertEquals(4, summary.getDistinctApplicationsInterviewed());
        // 4 distinct interviewed out of 8 = 50.0%
        assertEquals(50.0, summary.getInterviewConversionRate());
        assertEquals("50.0%", summary.getFormattedConversionRate());
        // 2 offers out of 8 = 25.0%
        assertEquals(25.0, summary.getOfferRate());
        assertEquals("25.0%", summary.getFormattedOfferRate());
    }

    @Test
    @DisplayName("DateRangeFilter calculate start dates deterministically")
    void testDateRangeFilterCalculations() {
        LocalDate fixedNow = LocalDate.of(2026, 11, 10);

        assertNull(DateRangeFilter.ALL_TIME.getStartDate(fixedNow));
        assertEquals(LocalDate.of(2026, 10, 11), DateRangeFilter.LAST_30_DAYS.getStartDate(fixedNow));
        assertEquals(LocalDate.of(2026, 8, 10), DateRangeFilter.LAST_3_MONTHS.getStartDate(fixedNow));
        assertEquals(LocalDate.of(2026, 5, 10), DateRangeFilter.LAST_6_MONTHS.getStartDate(fixedNow));
        assertEquals(LocalDate.of(2025, 11, 10), DateRangeFilter.LAST_12_MONTHS.getStartDate(fixedNow));

        // fromString parsing
        assertEquals(DateRangeFilter.LAST_30_DAYS, DateRangeFilter.fromString("Last 30 Days"));
        assertEquals(DateRangeFilter.ALL_TIME, DateRangeFilter.fromString(null));
        assertEquals(DateRangeFilter.ALL_TIME, DateRangeFilter.fromString("unknown"));
    }

    @Test
    @DisplayName("MonthlyApplicationData multi-year chronological sorting")
    void testMonthlyApplicationDataSortingMultiYear() {
        List<MonthlyApplicationData> list = new ArrayList<>();
        list.add(new MonthlyApplicationData("2026-03", 5));
        list.add(new MonthlyApplicationData("2025-11", 2));
        list.add(new MonthlyApplicationData("2026-01", 4));
        list.add(new MonthlyApplicationData("2025-08", 1));

        Collections.sort(list);

        assertEquals("2025-08", list.get(0).getYearMonth());
        assertEquals("Aug 2025", list.get(0).getDisplayLabel());
        assertEquals("2025-11", list.get(1).getYearMonth());
        assertEquals("2026-01", list.get(2).getYearMonth());
        assertEquals("2026-03", list.get(3).getYearMonth());
    }

    @Test
    @DisplayName("CompanyApplicationData sorting by count descending, then company name")
    void testCompanyApplicationDataSorting() {
        List<CompanyApplicationData> list = new ArrayList<>();
        list.add(new CompanyApplicationData("Apple", 3));
        list.add(new CompanyApplicationData("Google", 5));
        list.add(new CompanyApplicationData("Amazon", 3));

        Collections.sort(list);

        assertEquals("Google", list.get(0).getCompanyName());
        assertEquals(5, list.get(0).getApplicationCount());

        // For equal count (3), alphabetical order Amazon before Apple
        assertEquals("Amazon", list.get(1).getCompanyName());
        assertEquals("Apple", list.get(2).getCompanyName());
    }

    @Test
    @DisplayName("StatusDistribution sorting by count descending")
    void testStatusDistributionSorting() {
        List<StatusDistribution> list = new ArrayList<>();
        list.add(new StatusDistribution("Applied", 2, 20.0));
        list.add(new StatusDistribution("Selected", 1, 10.0));
        list.add(new StatusDistribution("Under Review", 5, 50.0));

        Collections.sort(list);

        assertEquals("Under Review", list.get(0).getStatusName());
        assertEquals(5, list.get(0).getCount());
        assertEquals("Applied", list.get(1).getStatusName());
        assertEquals(2, list.get(1).getCount());
        assertEquals("Selected", list.get(2).getStatusName());
        assertEquals(1, list.get(2).getCount());
    }

    @Test
    @DisplayName("AnalyticsService constructor rejects null repository")
    void testConstructorNullRepositoryThrows() {
        assertThrows(IllegalArgumentException.class, () -> new AnalyticsService(null));
    }

    // =========================================================================
    // Test Stub Implementation
    // =========================================================================
    private static class StubAnalyticsRepository implements AnalyticsRepository {
        int totalApps = 0;
        int activeApps = 0;
        int offers = 0;
        int rejected = 0;
        int totalInterviews = 0;
        int distinctInterviewed = 0;
        int upcomingInterviews = 0;
        int completedInterviews = 0;
        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        List<MonthlyApplicationData> monthlyAppCounts = new ArrayList<>();
        List<CompanyApplicationData> companyAppCounts = new ArrayList<>();
        Map<String, Integer> outcomeCounts = new LinkedHashMap<>();
        List<MonthlyApplicationData> monthlyInterviewCounts = new ArrayList<>();

        @Override
        public int countTotalApplications(int userId, LocalDate startDate) {
            return totalApps;
        }

        @Override
        public int countActiveApplications(int userId, LocalDate startDate) {
            return activeApps;
        }

        @Override
        public int countOffers(int userId, LocalDate startDate) {
            return offers;
        }

        @Override
        public int countRejected(int userId, LocalDate startDate) {
            return rejected;
        }

        @Override
        public int countTotalInterviews(int userId, LocalDate startDate) {
            return totalInterviews;
        }

        @Override
        public int countDistinctApplicationsInterviewed(int userId, LocalDate startDate) {
            return distinctInterviewed;
        }

        @Override
        public int countUpcomingInterviews(int userId) {
            return upcomingInterviews;
        }

        @Override
        public int countCompletedInterviews(int userId, LocalDate startDate) {
            return completedInterviews;
        }

        @Override
        public Map<String, Integer> getStatusCounts(int userId, LocalDate startDate) {
            return statusCounts;
        }

        @Override
        public List<MonthlyApplicationData> getMonthlyApplicationCounts(int userId, LocalDate startDate) {
            return monthlyAppCounts;
        }

        @Override
        public List<CompanyApplicationData> getCompanyApplicationCounts(int userId, LocalDate startDate, int limit) {
            return companyAppCounts;
        }

        @Override
        public Map<String, Integer> getInterviewOutcomeCounts(int userId, LocalDate startDate) {
            return outcomeCounts;
        }

        @Override
        public List<MonthlyApplicationData> getMonthlyInterviewCounts(int userId, LocalDate startDate) {
            return monthlyInterviewCounts;
        }
    }
}

