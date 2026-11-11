package com.applytrack.repository;

import com.applytrack.model.dto.CompanyApplicationData;
import com.applytrack.model.dto.MonthlyApplicationData;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Repository interface contract for retrieving multi-dimensional career analytics,
 * conversion funnels, and trend data for the authenticated candidate.
 *
 * Demonstrates:
 * - OOP: Interface Abstraction and Decoupled Architecture (Unit 3)
 * - User-Specific Data Security: Every query strictly binds user_id (Unit 3)
 * - Collections Framework: List, Map interfaces for multi-attribute metrics (Unit 3)
 * - Checked Exception handling: throws SQLException (Unit 3)
 */
public interface AnalyticsRepository {

    /**
     * Counts all applications submitted by the user on or after startDate (or all time if startDate is null).
     */
    int countTotalApplications(int userId, LocalDate startDate) throws SQLException;

    /**
     * Counts active ongoing applications (Applied, Under Review, Shortlisted, Interview Scheduled).
     */
    int countActiveApplications(int userId, LocalDate startDate) throws SQLException;

    /**
     * Counts applications resulting in job/internship offers (status = 'Selected').
     */
    int countOffers(int userId, LocalDate startDate) throws SQLException;

    /**
     * Counts rejected applications (status = 'Rejected').
     */
    int countRejected(int userId, LocalDate startDate) throws SQLException;

    /**
     * Counts total interview rounds associated with the user's applications within the timeframe.
     */
    int countTotalInterviews(int userId, LocalDate startDate) throws SQLException;

    /**
     * Counts distinct applications that have at least one interview round scheduled or conducted.
     * Used as the numerator in the Interview Conversion Rate calculation.
     */
    int countDistinctApplicationsInterviewed(int userId, LocalDate startDate) throws SQLException;

    /**
     * Counts upcoming scheduled interviews (interview_date > NOW() and outcome = 'Scheduled').
     */
    int countUpcomingInterviews(int userId) throws SQLException;

    /**
     * Counts completed interviews (outcome in 'Passed', 'Failed').
     */
    int countCompletedInterviews(int userId, LocalDate startDate) throws SQLException;

    /**
     * Retrieves distribution of applications across distinct statuses for the user and timeframe.
     * Returns Map where key = status name, value = count.
     */
    Map<String, Integer> getStatusCounts(int userId, LocalDate startDate) throws SQLException;

    /**
     * Retrieves monthly application counts grouped by year-month in chronological order.
     */
    List<MonthlyApplicationData> getMonthlyApplicationCounts(int userId, LocalDate startDate) throws SQLException;

    /**
     * Retrieves application counts grouped by company for the user, sorted descending by count.
     *
     * @param limit maximum number of top companies to return
     */
    List<CompanyApplicationData> getCompanyApplicationCounts(int userId, LocalDate startDate, int limit) throws SQLException;

    /**
     * Retrieves distribution of interview outcomes for the user's applications.
     * Returns Map where key = outcome name, value = count.
     */
    Map<String, Integer> getInterviewOutcomeCounts(int userId, LocalDate startDate) throws SQLException;

    /**
     * Retrieves monthly interview counts grouped by year-month in chronological order.
     */
    List<MonthlyApplicationData> getMonthlyInterviewCounts(int userId, LocalDate startDate) throws SQLException;
}

