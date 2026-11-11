package com.applytrack.repository.impl;

import com.applytrack.database.DatabaseConnection;
import com.applytrack.model.dto.ApplicationReportItem;
import com.applytrack.model.dto.DateWiseActivityItem;
import com.applytrack.model.dto.InterviewReportItem;
import com.applytrack.model.dto.RecruiterReportItem;
import com.applytrack.repository.ReportRepository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JDBC implementation of ReportRepository.
 *
 * Demonstrates:
 * - Parameterized PreparedStatement queries preventing SQL injection (Unit 3)
 * - Multi-tenant candidate data isolation (WHERE user_id = ?) (Unit 3)
 * - Automatic resource management via try-with-resources (Unit 3)
 * - Multi-table SQL JOINs across applications, companies, interviews, recruiters (Unit 3)
 */
public class JdbcReportRepository implements ReportRepository {

    @Override
    public List<ApplicationReportItem> getApplicationReportData(int userId, LocalDate start, LocalDate end) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT a.application_id, c.company_name, a.job_role, a.job_type, a.application_date, " +
                "       a.deadline, a.status, a.expected_salary, r.recruiter_name, a.notes " +
                "FROM applications a " +
                "JOIN companies c ON a.company_id = c.company_id " +
                "LEFT JOIN recruiters r ON a.recruiter_id = r.recruiter_id " +
                "WHERE a.user_id = ? "
        );

        if (start != null) {
            sql.append("AND a.application_date >= ? ");
        }
        if (end != null) {
            sql.append("AND a.application_date <= ? ");
        }
        sql.append("ORDER BY a.application_date DESC, a.application_id DESC");

        List<ApplicationReportItem> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setInt(1, userId);
            int idx = 2;
            if (start != null) {
                stmt.setDate(idx++, Date.valueOf(start));
            }
            if (end != null) {
                stmt.setDate(idx, Date.valueOf(end));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Date appDate = rs.getDate("application_date");
                    Date deadDate = rs.getDate("deadline");

                    list.add(new ApplicationReportItem(
                            rs.getInt("application_id"),
                            rs.getString("company_name"),
                            rs.getString("job_role"),
                            rs.getString("job_type"),
                            appDate != null ? appDate.toLocalDate() : null,
                            deadDate != null ? deadDate.toLocalDate() : null,
                            rs.getString("status"),
                            rs.getBigDecimal("expected_salary"),
                            rs.getString("recruiter_name"),
                            rs.getString("notes")
                    ));
                }
            }
        }
        return list;
    }

    @Override
    public List<InterviewReportItem> getInterviewReportData(int userId, LocalDate start, LocalDate end) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT i.interview_id, c.company_name, a.job_role, i.round_name, i.interview_date, " +
                "       i.interview_mode, i.interview_location, i.meeting_link, i.interviewer_name, " +
                "       i.feedback, i.outcome " +
                "FROM interviews i " +
                "JOIN applications a ON i.application_id = a.application_id " +
                "JOIN companies c ON a.company_id = c.company_id " +
                "WHERE a.user_id = ? "
        );

        if (start != null) {
            sql.append("AND i.interview_date >= ? ");
        }
        if (end != null) {
            sql.append("AND i.interview_date <= ? ");
        }
        sql.append("ORDER BY i.interview_date DESC");

        List<InterviewReportItem> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setInt(1, userId);
            int idx = 2;
            if (start != null) {
                stmt.setTimestamp(idx++, Timestamp.valueOf(LocalDateTime.of(start, LocalTime.MIN)));
            }
            if (end != null) {
                stmt.setTimestamp(idx, Timestamp.valueOf(LocalDateTime.of(end, LocalTime.MAX)));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("interview_date");
                    list.add(new InterviewReportItem(
                            rs.getInt("interview_id"),
                            rs.getString("company_name"),
                            rs.getString("job_role"),
                            rs.getString("round_name"),
                            ts != null ? ts.toLocalDateTime() : null,
                            rs.getString("interview_mode"),
                            rs.getString("interview_location"),
                            rs.getString("meeting_link"),
                            rs.getString("interviewer_name"),
                            rs.getString("feedback"),
                            rs.getString("outcome")
                    ));
                }
            }
        }
        return list;
    }

    @Override
    public List<RecruiterReportItem> getRecruiterReportData(int userId) throws SQLException {
        String sql =
                "SELECT r.recruiter_id, r.recruiter_name, c.company_name, r.email, r.phone, " +
                "       r.linkedin_url, COUNT(DISTINCT a.application_id) AS apps_count, r.notes " +
                "FROM recruiters r " +
                "LEFT JOIN companies c ON r.company_id = c.company_id " +
                "JOIN applications a ON (a.recruiter_id = r.recruiter_id OR a.company_id = r.company_id) " +
                "WHERE a.user_id = ? " +
                "GROUP BY r.recruiter_id, c.company_name " +
                "ORDER BY r.recruiter_name ASC";

        List<RecruiterReportItem> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new RecruiterReportItem(
                            rs.getInt("recruiter_id"),
                            rs.getString("recruiter_name"),
                            rs.getString("company_name"),
                            rs.getString("email"),
                            rs.getString("phone"),
                            rs.getString("linkedin_url"),
                            rs.getInt("apps_count"),
                            rs.getString("notes")
                    ));
                }
            }
        }
        return list;
    }

    @Override
    public List<DateWiseActivityItem> getDateWiseActivityData(int userId, LocalDate start, LocalDate end) throws SQLException {
        List<DateWiseActivityItem> list = new ArrayList<>();

        // 1. Fetch Applications submitted
        StringBuilder appSql = new StringBuilder(
                "SELECT a.application_date, c.company_name, a.job_role, a.status " +
                "FROM applications a " +
                "JOIN companies c ON a.company_id = c.company_id " +
                "WHERE a.user_id = ? "
        );
        if (start != null) {
            appSql.append("AND a.application_date >= ? ");
        }
        if (end != null) {
            appSql.append("AND a.application_date <= ? ");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(appSql.toString())) {
            stmt.setInt(1, userId);
            int idx = 2;
            if (start != null) {
                stmt.setDate(idx++, Date.valueOf(start));
            }
            if (end != null) {
                stmt.setDate(idx, Date.valueOf(end));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Date date = rs.getDate("application_date");
                    list.add(new DateWiseActivityItem(
                            date != null ? date.toLocalDate() : null,
                            "Application Submitted",
                            rs.getString("company_name"),
                            rs.getString("job_role"),
                            rs.getString("status")
                    ));
                }
            }
        }

        // 2. Fetch Interview rounds
        StringBuilder intSql = new StringBuilder(
                "SELECT DATE(i.interview_date) AS idate, c.company_name, i.round_name, i.outcome, i.interview_mode " +
                "FROM interviews i " +
                "JOIN applications a ON i.application_id = a.application_id " +
                "JOIN companies c ON a.company_id = c.company_id " +
                "WHERE a.user_id = ? "
        );
        if (start != null) {
            intSql.append("AND i.interview_date >= ? ");
        }
        if (end != null) {
            intSql.append("AND i.interview_date <= ? ");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(intSql.toString())) {
            stmt.setInt(1, userId);
            int idx = 2;
            if (start != null) {
                stmt.setTimestamp(idx++, Timestamp.valueOf(LocalDateTime.of(start, LocalTime.MIN)));
            }
            if (end != null) {
                stmt.setTimestamp(idx, Timestamp.valueOf(LocalDateTime.of(end, LocalTime.MAX)));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Date date = rs.getDate("idate");
                    String mode = rs.getString("interview_mode");
                    list.add(new DateWiseActivityItem(
                            date != null ? date.toLocalDate() : null,
                            "Interview Round (" + mode + ")",
                            rs.getString("company_name"),
                            rs.getString("round_name"),
                            rs.getString("outcome")
                    ));
                }
            }
        }

        Collections.sort(list);
        return list;
    }
}

