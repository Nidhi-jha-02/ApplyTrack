package com.applytrack.repository.impl;

import com.applytrack.database.DatabaseConnection;
import com.applytrack.model.Application;
import com.applytrack.model.Company;
import com.applytrack.model.enums.ApplicationStatus;
import com.applytrack.model.enums.JobType;
import com.applytrack.repository.ApplicationRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC Implementation of ApplicationRepository querying MariaDB.
 *
 * Demonstrates:
 * - PreparedStatement with parameter binding to prevent SQL Injection (Unit 3)
 * - Strict multi-tenant data isolation: every operation requires user_id = ?
 * - Whitelisted dynamic sorting logic (Unit 1 & 2)
 * - try-with-resources with automated connection management (Unit 3)
 * - Statement.RETURN_GENERATED_KEYS for auto-increment keys (Unit 3)
 */
public class JdbcApplicationRepository implements ApplicationRepository {

    private final Connection injectedConnection;

    public JdbcApplicationRepository() {
        this.injectedConnection = null;
    }

    public JdbcApplicationRepository(Connection connection) {
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
            "SELECT a.application_id, a.user_id, a.company_id, a.recruiter_id, a.job_role, a.job_type, " +
            "       a.application_date, a.deadline, a.job_description, a.job_url, a.expected_salary, " +
            "       a.status, a.notes, a.created_at, a.updated_at, " +
            "       c.company_name, c.website AS company_website, c.industry AS company_industry, c.location AS company_location " +
            "FROM applications a " +
            "JOIN companies c ON a.company_id = c.company_id ";

    @Override
    public Application save(Application application) throws SQLException {
        if (application == null) {
            throw new IllegalArgumentException("Application cannot be null.");
        }
        if (application.getId() > 0) {
            update(application);
            return application;
        }

        String sql = "INSERT INTO applications (" +
                     "  user_id, company_id, recruiter_id, job_role, job_type, " +
                     "  application_date, deadline, job_description, job_url, " +
                     "  expected_salary, status, notes" +
                     ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, application.getUserId());
            stmt.setInt(2, application.getCompanyId());

            if (application.getRecruiterId() != null && application.getRecruiterId() > 0) {
                stmt.setInt(3, application.getRecruiterId());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }

            stmt.setString(4, application.getJobRole());
            stmt.setString(5, application.getJobType() != null ? application.getJobType().getDisplayName() : JobType.FULL_TIME.getDisplayName());
            stmt.setDate(6, Date.valueOf(application.getApplicationDate()));

            if (application.getDeadline() != null) {
                stmt.setDate(7, Date.valueOf(application.getDeadline()));
            } else {
                stmt.setNull(7, Types.DATE);
            }

            stmt.setString(8, application.getJobDescription());
            stmt.setString(9, application.getJobUrl());

            if (application.getExpectedSalary() != null) {
                stmt.setBigDecimal(10, application.getExpectedSalary());
            } else {
                stmt.setNull(10, Types.DECIMAL);
            }

            stmt.setString(11, application.getStatus() != null ? application.getStatus().getDisplayName() : ApplicationStatus.APPLIED.getDisplayName());
            stmt.setString(12, application.getNotes());

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    application.setId(keys.getInt(1));
                }
            }
            return application;
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public Optional<Application> findById(int applicationId, int userId) throws SQLException {
        String sql = BASE_SELECT + "WHERE a.application_id = ? AND a.user_id = ?";
        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, applicationId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToApplication(rs));
                }
            }
        } finally {
            releaseConnection(conn);
        }
        return Optional.empty();
    }

    @Override
    public List<Application> findByUserId(int userId) throws SQLException {
        String sql = BASE_SELECT + "WHERE a.user_id = ? ORDER BY a.application_date DESC, a.application_id DESC";
        List<Application> list = new ArrayList<>();
        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToApplication(rs));
                }
            }
        } finally {
            releaseConnection(conn);
        }
        return list;
    }

    @Override
    public boolean update(Application application) throws SQLException {
        if (application == null || application.getId() <= 0) {
            return false;
        }

        String sql = "UPDATE applications SET " +
                     "  company_id = ?, recruiter_id = ?, job_role = ?, job_type = ?, " +
                     "  application_date = ?, deadline = ?, job_description = ?, job_url = ?, " +
                     "  expected_salary = ?, status = ?, notes = ? " +
                     "WHERE application_id = ? AND user_id = ?";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, application.getCompanyId());

            if (application.getRecruiterId() != null && application.getRecruiterId() > 0) {
                stmt.setInt(2, application.getRecruiterId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }

            stmt.setString(3, application.getJobRole());
            stmt.setString(4, application.getJobType() != null ? application.getJobType().getDisplayName() : JobType.FULL_TIME.getDisplayName());
            stmt.setDate(5, Date.valueOf(application.getApplicationDate()));

            if (application.getDeadline() != null) {
                stmt.setDate(6, Date.valueOf(application.getDeadline()));
            } else {
                stmt.setNull(6, Types.DATE);
            }

            stmt.setString(7, application.getJobDescription());
            stmt.setString(8, application.getJobUrl());

            if (application.getExpectedSalary() != null) {
                stmt.setBigDecimal(9, application.getExpectedSalary());
            } else {
                stmt.setNull(9, Types.DECIMAL);
            }

            stmt.setString(10, application.getStatus() != null ? application.getStatus().getDisplayName() : ApplicationStatus.APPLIED.getDisplayName());
            stmt.setString(11, application.getNotes());

            // User-specific isolation clause
            stmt.setInt(12, application.getId());
            stmt.setInt(13, application.getUserId());

            int rows = stmt.executeUpdate();
            return rows > 0;
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public boolean delete(int applicationId, int userId) throws SQLException {
        String sql = "DELETE FROM applications WHERE application_id = ? AND user_id = ?";
        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, applicationId);
            stmt.setInt(2, userId);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public List<Application> searchByKeyword(int userId, String keyword) throws SQLException {
        return findWithFilters(userId, keyword, null, null, "application_date", false);
    }

    @Override
    public List<Application> filterByStatus(int userId, ApplicationStatus status) throws SQLException {
        return findWithFilters(userId, null, status, null, "application_date", false);
    }

    @Override
    public List<Application> findWithFilters(int userId, String searchKeyword, ApplicationStatus statusFilter,
                                            JobType jobTypeFilter, String sortBy, boolean ascending) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        sql.append("WHERE a.user_id = ? ");

        List<Object> parameters = new ArrayList<>();
        parameters.add(userId);

        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            sql.append("AND (LOWER(c.company_name) LIKE ? OR LOWER(a.job_role) LIKE ?) ");
            String pattern = "%" + searchKeyword.trim().toLowerCase() + "%";
            parameters.add(pattern);
            parameters.add(pattern);
        }

        if (statusFilter != null) {
            sql.append("AND a.status = ? ");
            parameters.add(statusFilter.getDisplayName());
        }

        if (jobTypeFilter != null) {
            sql.append("AND a.job_type = ? ");
            parameters.add(jobTypeFilter.getDisplayName());
        }

        // Whitelisted sort column resolution to completely prevent SQL injection
        String orderColumn;
        if (sortBy == null) {
            orderColumn = "a.application_date";
        } else {
            orderColumn = switch (sortBy.toLowerCase().trim()) {
                case "company", "company_name" -> "c.company_name";
                case "role", "job_role" -> "a.job_role";
                case "deadline" -> "a.deadline IS NULL, a.deadline";
                case "status" -> "a.status";
                default -> "a.application_date";
            };
        }

        String direction = ascending ? "ASC" : "DESC";
        sql.append("ORDER BY ").append(orderColumn).append(" ").append(direction).append(", a.application_id DESC");

        List<Application> list = new ArrayList<>();
        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToApplication(rs));
                }
            }
        } finally {
            releaseConnection(conn);
        }
        return list;
    }

    private Application mapRowToApplication(ResultSet rs) throws SQLException {
        int id = rs.getInt("application_id");
        int userId = rs.getInt("user_id");
        int companyId = rs.getInt("company_id");

        int recIdVal = rs.getInt("recruiter_id");
        Integer recruiterId = rs.wasNull() ? null : recIdVal;

        String role = rs.getString("job_role");
        JobType jobType = JobType.fromString(rs.getString("job_type"));

        Date appDateVal = rs.getDate("application_date");
        LocalDate applicationDate = appDateVal != null ? appDateVal.toLocalDate() : LocalDate.now();

        Date deadlineVal = rs.getDate("deadline");
        LocalDate deadline = deadlineVal != null ? deadlineVal.toLocalDate() : null;

        String desc = rs.getString("job_description");
        String url = rs.getString("job_url");
        BigDecimal salary = rs.getBigDecimal("expected_salary");
        ApplicationStatus status = ApplicationStatus.fromString(rs.getString("status"));
        String notes = rs.getString("notes");

        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");
        LocalDateTime createdAt = createdTs != null ? createdTs.toLocalDateTime() : LocalDateTime.now();
        LocalDateTime updatedAt = updatedTs != null ? updatedTs.toLocalDateTime() : LocalDateTime.now();

        Application app = new Application(id, userId, companyId, recruiterId, role, jobType,
                applicationDate, deadline, desc, url, salary, status, notes, createdAt, updatedAt);

        // Populate attached company object
        String companyName = rs.getString("company_name");
        String companyWebsite = rs.getString("company_website");
        String companyIndustry = rs.getString("company_industry");
        String companyLocation = rs.getString("company_location");

        Company company = new Company(companyId, companyName, companyWebsite, companyIndustry, companyLocation,
                createdAt, updatedAt);
        app.setCompany(company);

        return app;
    }
}

