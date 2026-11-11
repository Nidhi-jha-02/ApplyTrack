package com.applytrack.repository.impl;

import com.applytrack.database.DatabaseConnection;
import com.applytrack.model.dto.CompanyApplicationData;
import com.applytrack.model.dto.MonthlyApplicationData;
import com.applytrack.repository.AnalyticsRepository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC implementation of AnalyticsRepository.
 *
 * Demonstrates:
 * - Parameterized PreparedStatement queries preventing SQL injection (Unit 3)
 * - SQL Aggregate functions: COUNT, DISTINCT, GROUP BY, ORDER BY, DATE_FORMAT (Unit 3)
 * - Automatic resource cleanup with try-with-resources (Unit 3)
 * - Multi-tenant candidate data isolation (user_id = ?) (Unit 3)
 */
public class JdbcAnalyticsRepository implements AnalyticsRepository {

    @Override
    public int countTotalApplications(int userId, LocalDate startDate) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM applications WHERE user_id = ? ");
        if (startDate != null) {
            sql.append("AND application_date >= ? ");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setInt(1, userId);
            if (startDate != null) {
                stmt.setDate(2, Date.valueOf(startDate));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @Override
    public int countActiveApplications(int userId, LocalDate startDate) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM applications " +
                "WHERE user_id = ? " +
                "  AND status IN ('Applied', 'Under Review', 'Shortlisted', 'Interview Scheduled') "
        );
        if (startDate != null) {
            sql.append("AND application_date >= ? ");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setInt(1, userId);
            if (startDate != null) {
                stmt.setDate(2, Date.valueOf(startDate));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @Override
    public int countOffers(int userId, LocalDate startDate) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM applications WHERE user_id = ? AND status = 'Selected' "
        );
        if (startDate != null) {
            sql.append("AND application_date >= ? ");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setInt(1, userId);
            if (startDate != null) {
                stmt.setDate(2, Date.valueOf(startDate));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @Override
    public int countRejected(int userId, LocalDate startDate) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM applications WHERE user_id = ? AND status = 'Rejected' "
        );
        if (startDate != null) {
            sql.append("AND application_date >= ? ");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setInt(1, userId);
            if (startDate != null) {
                stmt.setDate(2, Date.valueOf(startDate));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @Override
    public int countTotalInterviews(int userId, LocalDate startDate) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) " +
                "FROM interviews i " +
                "JOIN applications a ON i.application_id = a.application_id " +
                "WHERE a.user_id = ? "
        );
        if (startDate != null) {
            sql.append("AND i.interview_date >= ? ");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setInt(1, userId);
            if (startDate != null) {
                stmt.setTimestamp(2, Timestamp.valueOf(startDate.atStartOfDay()));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @Override
    public int countDistinctApplicationsInterviewed(int userId, LocalDate startDate) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(DISTINCT a.application_id) " +
                "FROM applications a " +
                "JOIN interviews i ON a.application_id = i.application_id " +
                "WHERE a.user_id = ? "
        );
        if (startDate != null) {
            sql.append("AND a.application_date >= ? ");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setInt(1, userId);
            if (startDate != null) {
                stmt.setDate(2, Date.valueOf(startDate));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @Override
    public int countUpcomingInterviews(int userId) throws SQLException {
        String sql =
                "SELECT COUNT(*) " +
                "FROM interviews i " +
                "JOIN applications a ON i.application_id = a.application_id " +
                "WHERE a.user_id = ? " +
                "  AND i.interview_date > NOW() " +
                "  AND i.outcome = 'Scheduled'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @Override
    public int countCompletedInterviews(int userId, LocalDate startDate) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) " +
                "FROM interviews i " +
                "JOIN applications a ON i.application_id = a.application_id " +
                "WHERE a.user_id = ? " +
                "  AND i.outcome IN ('Passed', 'Failed') "
        );
        if (startDate != null) {
            sql.append("AND i.interview_date >= ? ");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setInt(1, userId);
            if (startDate != null) {
                stmt.setTimestamp(2, Timestamp.valueOf(startDate.atStartOfDay()));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @Override
    public Map<String, Integer> getStatusCounts(int userId, LocalDate startDate) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT status, COUNT(*) AS count " +
                "FROM applications " +
                "WHERE user_id = ? "
        );
        if (startDate != null) {
            sql.append("AND application_date >= ? ");
        }
        sql.append("GROUP BY status ORDER BY count DESC");

        Map<String, Integer> map = new LinkedHashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setInt(1, userId);
            if (startDate != null) {
                stmt.setDate(2, Date.valueOf(startDate));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("status"), rs.getInt("count"));
                }
            }
        }
        return map;
    }

    @Override
    public List<MonthlyApplicationData> getMonthlyApplicationCounts(int userId, LocalDate startDate) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT DATE_FORMAT(application_date, '%Y-%m') AS ym, COUNT(*) AS count " +
                "FROM applications " +
                "WHERE user_id = ? "
        );
        if (startDate != null) {
            sql.append("AND application_date >= ? ");
        }
        sql.append("GROUP BY ym ORDER BY ym ASC");

        List<MonthlyApplicationData> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setInt(1, userId);
            if (startDate != null) {
                stmt.setDate(2, Date.valueOf(startDate));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new MonthlyApplicationData(rs.getString("ym"), rs.getInt("count")));
                }
            }
        }
        return list;
    }

    @Override
    public List<CompanyApplicationData> getCompanyApplicationCounts(int userId, LocalDate startDate, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT c.company_name, COUNT(a.application_id) AS app_count " +
                "FROM applications a " +
                "JOIN companies c ON a.company_id = c.company_id " +
                "WHERE a.user_id = ? "
        );
        if (startDate != null) {
            sql.append("AND a.application_date >= ? ");
        }
        sql.append("GROUP BY c.company_name ORDER BY app_count DESC, c.company_name ASC ");
        if (limit > 0) {
            sql.append("LIMIT ?");
        }

        List<CompanyApplicationData> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setInt(1, userId);
            int paramIndex = 2;
            if (startDate != null) {
                stmt.setDate(paramIndex++, Date.valueOf(startDate));
            }
            if (limit > 0) {
                stmt.setInt(paramIndex, limit);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new CompanyApplicationData(rs.getString("company_name"), rs.getInt("app_count")));
                }
            }
        }
        return list;
    }

    @Override
    public Map<String, Integer> getInterviewOutcomeCounts(int userId, LocalDate startDate) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT i.outcome, COUNT(*) AS count " +
                "FROM interviews i " +
                "JOIN applications a ON i.application_id = a.application_id " +
                "WHERE a.user_id = ? "
        );
        if (startDate != null) {
            sql.append("AND i.interview_date >= ? ");
        }
        sql.append("GROUP BY i.outcome ORDER BY count DESC");

        Map<String, Integer> map = new LinkedHashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setInt(1, userId);
            if (startDate != null) {
                stmt.setTimestamp(2, Timestamp.valueOf(startDate.atStartOfDay()));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("outcome"), rs.getInt("count"));
                }
            }
        }
        return map;
    }

    @Override
    public List<MonthlyApplicationData> getMonthlyInterviewCounts(int userId, LocalDate startDate) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT DATE_FORMAT(i.interview_date, '%Y-%m') AS ym, COUNT(*) AS count " +
                "FROM interviews i " +
                "JOIN applications a ON i.application_id = a.application_id " +
                "WHERE a.user_id = ? "
        );
        if (startDate != null) {
            sql.append("AND i.interview_date >= ? ");
        }
        sql.append("GROUP BY ym ORDER BY ym ASC");

        List<MonthlyApplicationData> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setInt(1, userId);
            if (startDate != null) {
                stmt.setTimestamp(2, Timestamp.valueOf(startDate.atStartOfDay()));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new MonthlyApplicationData(rs.getString("ym"), rs.getInt("count")));
                }
            }
        }
        return list;
    }
}

