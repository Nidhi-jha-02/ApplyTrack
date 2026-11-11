package com.applytrack.repository.impl;

import com.applytrack.database.DatabaseConnection;
import com.applytrack.model.dto.DeadlineSummaryItem;
import com.applytrack.model.dto.InterviewSummaryItem;
import com.applytrack.repository.DashboardRepository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC Implementation of DashboardRepository querying MariaDB.
 *
 * Demonstrates:
 * - OOP: Implementing an Interface contract (Unit 3)
 * - JDBC: PreparedStatement to prevent SQL Injection (Unit 3)
 * - Exception Handling: Standardized checked exception handling (Unit 3)
 * - Collections: LinkedHashMap, ArrayList (Unit 3)
 * - User-Specific Data Security: Every query strictly filters by user_id = ?
 */
public class JdbcDashboardRepository implements DashboardRepository {

    private final Connection injectedConnection;

    public JdbcDashboardRepository() {
        this.injectedConnection = null;
    }

    public JdbcDashboardRepository(Connection connection) {
        this.injectedConnection = connection;
    }

    private Connection acquireConnection() throws SQLException {
        if (injectedConnection != null && !injectedConnection.isClosed()) {
            return injectedConnection;
        }
        return DatabaseConnection.getConnection();
    }

    private void releaseConnection(Connection conn) {
        if (injectedConnection == null && conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }

    @Override
    public int countTotalApplications(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM applications WHERE user_id = ?";
        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public int countInProgressApplications(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM applications WHERE user_id = ? " +
                     "AND status IN ('Applied', 'Under Review', 'Shortlisted', 'Interview Scheduled')";
        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public int countScheduledInterviews(int userId) throws SQLException {
        String sql = "SELECT COUNT(i.interview_id) FROM interviews i " +
                     "JOIN applications a ON i.application_id = a.application_id " +
                     "WHERE a.user_id = ? AND i.outcome = 'Scheduled'";
        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public int countOffers(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM applications WHERE user_id = ? AND status = 'Selected'";
        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public int countRejected(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM applications WHERE user_id = ? AND status = 'Rejected'";
        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public Map<String, Integer> getStatusDistribution(int userId) throws SQLException {
        String sql = "SELECT status, COUNT(*) AS count_val FROM applications " +
                     "WHERE user_id = ? GROUP BY status ORDER BY count_val DESC";
        Map<String, Integer> distribution = new LinkedHashMap<>();
        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    distribution.put(rs.getString("status"), rs.getInt("count_val"));
                }
            }
        } finally {
            releaseConnection(conn);
        }
        return distribution;
    }

    @Override
    public Map<String, Integer> getMonthlyTrends(int userId) throws SQLException {
        String sql = "SELECT DATE_FORMAT(application_date, '%Y-%m') AS month_key, COUNT(*) AS count_val " +
                     "FROM applications WHERE user_id = ? " +
                     "GROUP BY DATE_FORMAT(application_date, '%Y-%m') " +
                     "ORDER BY month_key ASC";
        Map<String, Integer> trends = new LinkedHashMap<>();
        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    trends.put(rs.getString("month_key"), rs.getInt("count_val"));
                }
            }
        } finally {
            releaseConnection(conn);
        }
        return trends;
    }

    @Override
    public List<DeadlineSummaryItem> getUpcomingDeadlines(int userId, int limit) throws SQLException {
        String sql = "SELECT a.application_id, c.company_name, a.job_role, a.deadline, a.status, " +
                     "DATEDIFF(a.deadline, CURRENT_DATE) AS days_remaining " +
                     "FROM applications a " +
                     "JOIN companies c ON a.company_id = c.company_id " +
                     "WHERE a.user_id = ? " +
                     "  AND a.deadline IS NOT NULL " +
                     "  AND a.deadline >= CURRENT_DATE " +
                     "  AND a.status NOT IN ('Selected', 'Rejected', 'Withdrawn') " +
                     "ORDER BY a.deadline ASC " +
                     "LIMIT ?";
        List<DeadlineSummaryItem> list = new ArrayList<>();
        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, limit > 0 ? limit : 10);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int appId = rs.getInt("application_id");
                    String companyName = rs.getString("company_name");
                    String jobRole = rs.getString("job_role");
                    Date deadlineDate = rs.getDate("deadline");
                    LocalDate deadline = deadlineDate != null ? deadlineDate.toLocalDate() : null;
                    long daysRemaining = rs.getLong("days_remaining");
                    String status = rs.getString("status");

                    list.add(new DeadlineSummaryItem(appId, companyName, jobRole, deadline, daysRemaining, status));
                }
            }
        } finally {
            releaseConnection(conn);
        }
        return list;
    }

    @Override
    public List<InterviewSummaryItem> getUpcomingInterviews(int userId, int limit) throws SQLException {
        String sql = "SELECT i.interview_id, a.application_id, c.company_name, a.job_role, " +
                     "       i.round_name, i.interview_date, i.interview_mode, i.meeting_link, " +
                     "       i.interviewer_name, i.outcome " +
                     "FROM interviews i " +
                     "JOIN applications a ON i.application_id = a.application_id " +
                     "JOIN companies c ON a.company_id = c.company_id " +
                     "WHERE a.user_id = ? " +
                     "  AND (i.outcome = 'Scheduled' OR i.interview_date >= NOW()) " +
                     "ORDER BY i.interview_date ASC " +
                     "LIMIT ?";
        List<InterviewSummaryItem> list = new ArrayList<>();
        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, limit > 0 ? limit : 10);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int interviewId = rs.getInt("interview_id");
                    int appId = rs.getInt("application_id");
                    String companyName = rs.getString("company_name");
                    String jobRole = rs.getString("job_role");
                    String roundName = rs.getString("round_name");
                    Timestamp ts = rs.getTimestamp("interview_date");
                    LocalDateTime interviewDate = ts != null ? ts.toLocalDateTime() : null;
                    String mode = rs.getString("interview_mode");
                    String link = rs.getString("meeting_link");
                    String interviewer = rs.getString("interviewer_name");
                    String outcome = rs.getString("outcome");

                    list.add(new InterviewSummaryItem(interviewId, appId, companyName, jobRole,
                            roundName, interviewDate, mode, link, interviewer, outcome));
                }
            }
        } finally {
            releaseConnection(conn);
        }
        return list;
    }
}

