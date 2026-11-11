package com.applytrack.repository;

import com.applytrack.model.dto.DeadlineSummaryItem;
import com.applytrack.model.dto.InterviewSummaryItem;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Repository contract for retrieving aggregated analytics, KPI counters,
 * and upcoming deadlines/interviews for the dynamic dashboard.
 *
 * Demonstrates:
 * - OOP: Interface contract and abstraction (Unit 3)
 * - Collections Framework: List, Map interfaces (Unit 3)
 * - Checked Exception handling: throws SQLException (Unit 3)
 * - User-Specific Data Security: Every method mandates userId filtering (Unit 3)
 */
public interface DashboardRepository {

    /**
     * Counts all job applications submitted by the specified user.
     * SQL: SELECT COUNT(*) FROM applications WHERE user_id = ?
     */
    int countTotalApplications(int userId) throws SQLException;

    /**
     * Counts active in-progress applications for the specified user.
     * In-progress statuses: 'Applied', 'Under Review', 'Shortlisted', 'Interview Scheduled'
     */
    int countInProgressApplications(int userId) throws SQLException;

    /**
     * Counts upcoming scheduled interview rounds for the specified user.
     * SQL: Joins interviews and applications filtered by user_id and outcome = 'Scheduled'
     */
    int countScheduledInterviews(int userId) throws SQLException;

    /**
     * Counts applications resulting in job or internship offers (status = 'Selected').
     */
    int countOffers(int userId) throws SQLException;

    /**
     * Counts applications with status = 'Rejected'.
     */
    int countRejected(int userId) throws SQLException;

    /**
     * Retrieves distribution of applications across distinct statuses for the specified user.
     * Returns a LinkedHashMap mapping status name -> count, sorted by count descending.
     */
    Map<String, Integer> getStatusDistribution(int userId) throws SQLException;

    /**
     * Retrieves monthly application submission trends for the specified user.
     * Returns a LinkedHashMap mapping month key (e.g. "YYYY-MM") -> count, ordered chronologically.
     */
    Map<String, Integer> getMonthlyTrends(int userId) throws SQLException;

    /**
     * Retrieves upcoming application deadlines for active applications, sorted nearest first.
     */
    List<DeadlineSummaryItem> getUpcomingDeadlines(int userId, int limit) throws SQLException;

    /**
     * Retrieves upcoming interview rounds for the user, sorted earliest first.
     */
    List<InterviewSummaryItem> getUpcomingInterviews(int userId, int limit) throws SQLException;
}

