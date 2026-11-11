package com.applytrack.repository.impl;

import com.applytrack.database.DatabaseConnection;
import com.applytrack.model.Application;
import com.applytrack.model.Company;
import com.applytrack.model.Interview;
import com.applytrack.model.enums.ApplicationStatus;
import com.applytrack.model.enums.InterviewMode;
import com.applytrack.model.enums.InterviewOutcome;
import com.applytrack.model.enums.JobType;
import com.applytrack.repository.InterviewRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC Implementation of InterviewRepository querying MariaDB.
 *
 * Demonstrates:
 * - PreparedStatement parameter binding to prevent SQL Injection (Unit 3)
 * - Multi-table JOIN queries connecting interviews, applications, and companies (Unit 2 & 3)
 * - Strict multi-tenant data isolation: all queries require a.user_id = ?
 * - Statement.RETURN_GENERATED_KEYS for auto-increment keys (Unit 3)
 * - Dynamic SQL construction with column whitelisting (Unit 1 & 2)
 * - Automated resource management with try-with-resources (Unit 3)
 */
public class JdbcInterviewRepository implements InterviewRepository {

    private final Connection injectedConnection;

    public JdbcInterviewRepository() {
        this.injectedConnection = null;
    }

    public JdbcInterviewRepository(Connection connection) {
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

    private static final String BASE_SELECT =
            "SELECT i.interview_id, i.application_id, i.round_name, i.interview_date, " +
            "       i.interview_mode, i.interview_location, i.meeting_link, i.interviewer_name, " +
            "       i.feedback, i.outcome, i.created_at, " +
            "       a.user_id, a.company_id, a.recruiter_id, a.job_role, a.job_type, " +
            "       a.application_date, a.deadline, a.expected_salary, a.status AS app_status, " +
            "       c.company_name, c.website AS company_website, c.industry AS company_industry, c.location AS company_location " +
            "FROM interviews i " +
            "JOIN applications a ON i.application_id = a.application_id " +
            "JOIN companies c ON a.company_id = c.company_id ";

    @Override
    public Interview save(Interview interview) throws SQLException {
        if (interview == null) {
            throw new IllegalArgumentException("Interview cannot be null");
        }

        String sql = "INSERT INTO interviews (application_id, round_name, interview_date, interview_mode, " +
                     "                       interview_location, meeting_link, interviewer_name, feedback, " +
                     "                       outcome, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, interview.getApplicationId());
            stmt.setString(2, interview.getRoundName());
            stmt.setTimestamp(3, Timestamp.valueOf(interview.getInterviewDate()));
            stmt.setString(4, interview.getInterviewMode() != null ? interview.getInterviewMode().getDisplayName() : "Online");
            stmt.setString(5, interview.getInterviewLocation());
            stmt.setString(6, interview.getMeetingLink());
            stmt.setString(7, interview.getInterviewerName());
            stmt.setString(8, interview.getFeedback());
            stmt.setString(9, interview.getOutcome() != null ? interview.getOutcome().getDisplayName() : "Scheduled");

            LocalDateTime createdAt = interview.getCreatedAt() != null ? interview.getCreatedAt() : LocalDateTime.now();
            stmt.setTimestamp(10, Timestamp.valueOf(createdAt));

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Failed to insert interview, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    interview.setId(generatedKeys.getInt(1));
                }
            }
            return interview;
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public Optional<Interview> findById(int interviewId, int userId) throws SQLException {
        String sql = BASE_SELECT + "WHERE i.interview_id = ? AND a.user_id = ?";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, interviewId);
            stmt.setInt(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToInterview(rs));
                }
            }
            return Optional.empty();
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public List<Interview> findByUserId(int userId) throws SQLException {
        String sql = BASE_SELECT + "WHERE a.user_id = ? ORDER BY i.interview_date DESC";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                List<Interview> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapResultSetToInterview(rs));
                }
                return list;
            }
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public List<Interview> findByApplicationId(int applicationId, int userId) throws SQLException {
        String sql = BASE_SELECT + "WHERE i.application_id = ? AND a.user_id = ? ORDER BY i.interview_date ASC";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, applicationId);
            stmt.setInt(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                List<Interview> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapResultSetToInterview(rs));
                }
                return list;
            }
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public boolean update(Interview interview, int userId) throws SQLException {
        if (interview == null || interview.getId() <= 0) {
            return false;
        }

        String sql = "UPDATE interviews i " +
                     "JOIN applications a ON i.application_id = a.application_id " +
                     "SET i.round_name = ?, " +
                     "    i.interview_date = ?, " +
                     "    i.interview_mode = ?, " +
                     "    i.interview_location = ?, " +
                     "    i.meeting_link = ?, " +
                     "    i.interviewer_name = ?, " +
                     "    i.feedback = ?, " +
                     "    i.outcome = ? " +
                     "WHERE i.interview_id = ? AND a.user_id = ?";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, interview.getRoundName());
            stmt.setTimestamp(2, Timestamp.valueOf(interview.getInterviewDate()));
            stmt.setString(3, interview.getInterviewMode() != null ? interview.getInterviewMode().getDisplayName() : "Online");
            stmt.setString(4, interview.getInterviewLocation());
            stmt.setString(5, interview.getMeetingLink());
            stmt.setString(6, interview.getInterviewerName());
            stmt.setString(7, interview.getFeedback());
            stmt.setString(8, interview.getOutcome() != null ? interview.getOutcome().getDisplayName() : "Scheduled");
            stmt.setInt(9, interview.getId());
            stmt.setInt(10, userId);

            return stmt.executeUpdate() > 0;
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public boolean delete(int interviewId, int userId) throws SQLException {
        String sql = "DELETE i FROM interviews i " +
                     "JOIN applications a ON i.application_id = a.application_id " +
                     "WHERE i.interview_id = ? AND a.user_id = ?";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, interviewId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public List<Interview> searchByKeyword(int userId, String keyword) throws SQLException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findByUserId(userId);
        }

        String sql = BASE_SELECT +
                     "WHERE a.user_id = ? AND (" +
                     "   LOWER(i.round_name) LIKE ? OR " +
                     "   LOWER(c.company_name) LIKE ? OR " +
                     "   LOWER(a.job_role) LIKE ? OR " +
                     "   LOWER(i.interviewer_name) LIKE ? OR " +
                     "   LOWER(i.feedback) LIKE ?" +
                     ") ORDER BY i.interview_date DESC";

        String pattern = "%" + keyword.trim().toLowerCase() + "%";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);
            stmt.setString(4, pattern);
            stmt.setString(5, pattern);
            stmt.setString(6, pattern);

            try (ResultSet rs = stmt.executeQuery()) {
                List<Interview> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapResultSetToInterview(rs));
                }
                return list;
            }
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public List<Interview> filterByOutcome(int userId, InterviewOutcome outcome) throws SQLException {
        if (outcome == null) {
            return findByUserId(userId);
        }

        String sql = BASE_SELECT + "WHERE a.user_id = ? AND i.outcome = ? ORDER BY i.interview_date DESC";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, outcome.getDisplayName());

            try (ResultSet rs = stmt.executeQuery()) {
                List<Interview> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapResultSetToInterview(rs));
                }
                return list;
            }
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public List<Interview> findUpcoming(int userId) throws SQLException {
        String sql = BASE_SELECT +
                     "WHERE a.user_id = ? AND i.interview_date >= NOW() AND i.outcome = 'Scheduled' " +
                     "ORDER BY i.interview_date ASC";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                List<Interview> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapResultSetToInterview(rs));
                }
                return list;
            }
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public List<Interview> findCompleted(int userId) throws SQLException {
        String sql = BASE_SELECT +
                     "WHERE a.user_id = ? AND (i.interview_date < NOW() OR i.outcome != 'Scheduled') " +
                     "ORDER BY i.interview_date DESC";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                List<Interview> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapResultSetToInterview(rs));
                }
                return list;
            }
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public List<Interview> findWithFilters(int userId, String searchKeyword, String outcomeFilter,
                                          String timelineFilter, String sortBy, boolean ascending) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE a.user_id = ? ");
        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            sql.append("AND (LOWER(i.round_name) LIKE ? OR LOWER(c.company_name) LIKE ? OR LOWER(a.job_role) LIKE ? OR LOWER(i.interviewer_name) LIKE ?) ");
            String pattern = "%" + searchKeyword.trim().toLowerCase() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (outcomeFilter != null && !outcomeFilter.equalsIgnoreCase("All") && !outcomeFilter.trim().isEmpty()) {
            sql.append("AND i.outcome = ? ");
            params.add(outcomeFilter.trim());
        }

        if (timelineFilter != null && !timelineFilter.equalsIgnoreCase("All") && !timelineFilter.trim().isEmpty()) {
            if ("Upcoming".equalsIgnoreCase(timelineFilter.trim())) {
                sql.append("AND i.interview_date >= NOW() AND i.outcome = 'Scheduled' ");
            } else if ("Completed".equalsIgnoreCase(timelineFilter.trim()) || "Past".equalsIgnoreCase(timelineFilter.trim())) {
                sql.append("AND (i.interview_date < NOW() OR i.outcome != 'Scheduled') ");
            }
        }

        // Whitelist sort column to prevent SQL injection
        String sortColumn = switch (sortBy != null ? sortBy.toLowerCase().trim() : "date") {
            case "company" -> "c.company_name";
            case "round" -> "i.round_name";
            case "outcome" -> "i.outcome";
            default -> "i.interview_date";
        };

        String direction = ascending ? "ASC" : "DESC";
        sql.append("ORDER BY ").append(sortColumn).append(" ").append(direction);

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int idx = 0; idx < params.size(); idx++) {
                Object val = params.get(idx);
                if (val instanceof Integer intVal) {
                    stmt.setInt(idx + 1, intVal);
                } else if (val instanceof String strVal) {
                    stmt.setString(idx + 1, strVal);
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                List<Interview> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapResultSetToInterview(rs));
                }
                return list;
            }
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public int countInterviewsByUserId(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM interviews i " +
                     "JOIN applications a ON i.application_id = a.application_id " +
                     "WHERE a.user_id = ?";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public int countUpcomingByUserId(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM interviews i " +
                     "JOIN applications a ON i.application_id = a.application_id " +
                     "WHERE a.user_id = ? AND i.interview_date >= NOW() AND i.outcome = 'Scheduled'";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Maps a joined ResultSet row into an Interview object with its parent Application and Company.
     */
    private Interview mapResultSetToInterview(ResultSet rs) throws SQLException {
        int interviewId = rs.getInt("interview_id");
        int applicationId = rs.getInt("application_id");
        String roundName = rs.getString("round_name");

        Timestamp interviewTs = rs.getTimestamp("interview_date");
        LocalDateTime interviewDate = interviewTs != null ? interviewTs.toLocalDateTime() : LocalDateTime.now();

        InterviewMode mode = InterviewMode.fromString(rs.getString("interview_mode"));
        String location = rs.getString("interview_location");
        String meetingLink = rs.getString("meeting_link");
        String interviewer = rs.getString("interviewer_name");
        String feedback = rs.getString("feedback");
        InterviewOutcome outcome = InterviewOutcome.fromString(rs.getString("outcome"));

        Timestamp createdTs = rs.getTimestamp("created_at");
        LocalDateTime createdAt = createdTs != null ? createdTs.toLocalDateTime() : LocalDateTime.now();

        Interview interview = new Interview(
                interviewId,
                applicationId,
                roundName,
                interviewDate,
                mode,
                location,
                meetingLink,
                interviewer,
                feedback,
                outcome,
                createdAt,
                createdAt
        );

        // Populate attached Company
        int companyId = rs.getInt("company_id");
        String companyName = rs.getString("company_name");
        String companyWebsite = rs.getString("company_website");
        String companyIndustry = rs.getString("company_industry");
        String companyLocation = rs.getString("company_location");

        Company company = new Company(companyId, companyName, companyWebsite, companyIndustry, companyLocation, null, null);

        // Populate attached Application
        int userId = rs.getInt("user_id");
        int recruiterIdVal = rs.getInt("recruiter_id");
        Integer recruiterId = rs.wasNull() ? null : recruiterIdVal;
        String jobRole = rs.getString("job_role");
        JobType jobType = JobType.fromString(rs.getString("job_type"));

        Date appDateVal = rs.getDate("application_date");
        LocalDate applicationDate = appDateVal != null ? appDateVal.toLocalDate() : LocalDate.now();

        Date deadlineVal = rs.getDate("deadline");
        LocalDate deadline = deadlineVal != null ? deadlineVal.toLocalDate() : null;

        BigDecimal salary = rs.getBigDecimal("expected_salary");
        ApplicationStatus appStatus = ApplicationStatus.fromString(rs.getString("app_status"));

        Application app = new Application(
                applicationId,
                userId,
                companyId,
                recruiterId,
                jobRole,
                jobType,
                applicationDate,
                deadline,
                null,
                null,
                salary,
                appStatus,
                null,
                null,
                null
        );
        app.setCompany(company);

        interview.setApplication(app);
        return interview;
    }
}

