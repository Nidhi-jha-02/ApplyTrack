package com.applytrack.repository.impl;

import com.applytrack.database.DatabaseConnection;
import com.applytrack.model.Application;
import com.applytrack.model.Company;
import com.applytrack.model.Interview;
import com.applytrack.model.Recruiter;
import com.applytrack.model.enums.ApplicationStatus;
import com.applytrack.model.enums.InterviewMode;
import com.applytrack.model.enums.InterviewOutcome;
import com.applytrack.model.enums.JobType;
import com.applytrack.repository.RecruiterRepository;

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
 * JDBC Implementation of RecruiterRepository querying MariaDB.
 *
 * Demonstrates:
 * - PreparedStatement with parameter binding to prevent SQL Injection (Unit 3)
 * - Statement.RETURN_GENERATED_KEYS for auto-increment keys (Unit 3)
 * - try-with-resources with automated connection management (Unit 3)
 * - Multi-tenant security via subquery joins on user's application/company pipeline
 * - Foreign-key SET NULL lifecycle respect (Unit 2 & 3)
 */
public class JdbcRecruiterRepository implements RecruiterRepository {

    private final Connection injectedConnection;

    public JdbcRecruiterRepository() {
        this.injectedConnection = null;
    }

    public JdbcRecruiterRepository(Connection connection) {
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
            "SELECT r.recruiter_id, r.company_id, r.recruiter_name, r.email, r.phone, " +
            "       r.linkedin_url, r.notes, r.created_at, " +
            "       c.company_name, c.website AS company_website, c.industry AS company_industry, c.location AS company_location, " +
            "       (SELECT COUNT(*) FROM applications a2 WHERE a2.recruiter_id = r.recruiter_id AND a2.user_id = ?) AS app_count " +
            "FROM recruiters r " +
            "LEFT JOIN companies c ON r.company_id = c.company_id ";

    private static final String USER_SCOPE_CLAUSE =
            "WHERE ( " +
            "   r.recruiter_id IN (SELECT DISTINCT a.recruiter_id FROM applications a WHERE a.user_id = ? AND a.recruiter_id IS NOT NULL) " +
            "   OR " +
            "   r.company_id IN (SELECT DISTINCT a.company_id FROM applications a WHERE a.user_id = ?) " +
            ") ";

    @Override
    public Recruiter save(Recruiter recruiter) throws SQLException {
        if (recruiter == null) {
            throw new IllegalArgumentException("Recruiter cannot be null");
        }

        String sql = "INSERT INTO recruiters (company_id, recruiter_name, email, phone, linkedin_url, notes, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (recruiter.getCompanyId() != null && recruiter.getCompanyId() > 0) {
                stmt.setInt(1, recruiter.getCompanyId());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setString(2, recruiter.getRecruiterName());
            stmt.setString(3, recruiter.getEmail());
            stmt.setString(4, recruiter.getPhone());
            stmt.setString(5, recruiter.getLinkedinUrl());
            stmt.setString(6, recruiter.getNotes());

            LocalDateTime createdAt = recruiter.getCreatedAt() != null ? recruiter.getCreatedAt() : LocalDateTime.now();
            stmt.setTimestamp(7, Timestamp.valueOf(createdAt));

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Failed to insert recruiter, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    recruiter.setId(generatedKeys.getInt(1));
                }
            }
            return recruiter;
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public Optional<Recruiter> findById(int recruiterId, int userId) throws SQLException {
        String sql = BASE_SELECT +
                     USER_SCOPE_CLAUSE +
                     "AND r.recruiter_id = ?";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            stmt.setInt(4, recruiterId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRecruiter(rs));
                }
            }
            return Optional.empty();
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public List<Recruiter> findByUserId(int userId) throws SQLException {
        String sql = BASE_SELECT + USER_SCOPE_CLAUSE + "ORDER BY r.recruiter_name ASC";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                List<Recruiter> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapResultSetToRecruiter(rs));
                }
                return list;
            }
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public List<Recruiter> findByCompanyId(int companyId, int userId) throws SQLException {
        String sql = BASE_SELECT + USER_SCOPE_CLAUSE + "AND r.company_id = ? ORDER BY r.recruiter_name ASC";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            stmt.setInt(4, companyId);

            try (ResultSet rs = stmt.executeQuery()) {
                List<Recruiter> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapResultSetToRecruiter(rs));
                }
                return list;
            }
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public boolean update(Recruiter recruiter, int userId) throws SQLException {
        if (recruiter == null || recruiter.getId() <= 0) {
            return false;
        }

        // Verify ownership/accessibility first
        Optional<Recruiter> existing = findById(recruiter.getId(), userId);
        if (existing.isEmpty()) {
            return false;
        }

        String sql = "UPDATE recruiters " +
                     "SET company_id = ?, " +
                     "    recruiter_name = ?, " +
                     "    email = ?, " +
                     "    phone = ?, " +
                     "    linkedin_url = ?, " +
                     "    notes = ? " +
                     "WHERE recruiter_id = ?";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (recruiter.getCompanyId() != null && recruiter.getCompanyId() > 0) {
                stmt.setInt(1, recruiter.getCompanyId());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setString(2, recruiter.getRecruiterName());
            stmt.setString(3, recruiter.getEmail());
            stmt.setString(4, recruiter.getPhone());
            stmt.setString(5, recruiter.getLinkedinUrl());
            stmt.setString(6, recruiter.getNotes());
            stmt.setInt(7, recruiter.getId());

            return stmt.executeUpdate() > 0;
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public boolean delete(int recruiterId, int userId) throws SQLException {
        // Verify ownership/accessibility first
        Optional<Recruiter> existing = findById(recruiterId, userId);
        if (existing.isEmpty()) {
            return false;
        }

        String sql = "DELETE FROM recruiters WHERE recruiter_id = ?";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, recruiterId);
            return stmt.executeUpdate() > 0;
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public List<Recruiter> searchByKeyword(int userId, String keyword) throws SQLException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findByUserId(userId);
        }

        String sql = BASE_SELECT +
                     USER_SCOPE_CLAUSE +
                     "AND ( " +
                     "   LOWER(r.recruiter_name) LIKE ? OR " +
                     "   LOWER(r.email) LIKE ? OR " +
                     "   LOWER(r.phone) LIKE ? OR " +
                     "   LOWER(c.company_name) LIKE ? OR " +
                     "   LOWER(r.notes) LIKE ? " +
                     ") ORDER BY r.recruiter_name ASC";

        String pattern = "%" + keyword.trim().toLowerCase() + "%";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            stmt.setString(4, pattern);
            stmt.setString(5, pattern);
            stmt.setString(6, pattern);
            stmt.setString(7, pattern);
            stmt.setString(8, pattern);

            try (ResultSet rs = stmt.executeQuery()) {
                List<Recruiter> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapResultSetToRecruiter(rs));
                }
                return list;
            }
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public List<Recruiter> findWithFilters(int userId, String searchKeyword, Integer companyId,
                                          String sortBy, boolean ascending) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(USER_SCOPE_CLAUSE);
        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.add(userId);
        params.add(userId);

        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            sql.append("AND (LOWER(r.recruiter_name) LIKE ? OR LOWER(r.email) LIKE ? OR LOWER(r.phone) LIKE ? OR LOWER(c.company_name) LIKE ? OR LOWER(r.notes) LIKE ? OR LOWER(r.linkedin_url) LIKE ?) ");
            String pattern = "%" + searchKeyword.trim().toLowerCase() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (companyId != null && companyId > 0) {
            sql.append("AND r.company_id = ? ");
            params.add(companyId);
        }

        // Whitelist sort column
        String sortColumn = switch (sortBy != null ? sortBy.toLowerCase().trim() : "name") {
            case "company" -> "c.company_name";
            case "email" -> "r.email";
            case "apps" -> "app_count";
            default -> "r.recruiter_name";
        };

        String direction = ascending ? "ASC" : "DESC";
        sql.append("ORDER BY ").append(sortColumn).append(" ").append(direction);

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer intVal) {
                    stmt.setInt(i + 1, intVal);
                } else if (p instanceof String strVal) {
                    stmt.setString(i + 1, strVal);
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                List<Recruiter> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapResultSetToRecruiter(rs));
                }
                return list;
            }
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public int countRecruitersByUserId(int userId) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT r.recruiter_id) FROM recruiters r " + USER_SCOPE_CLAUSE;

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);

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
    public int countDistinctCompaniesByUserId(int userId) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT r.company_id) FROM recruiters r " + USER_SCOPE_CLAUSE + " AND r.company_id IS NOT NULL";
        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
            return 0;
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public int countLinkedApplicationsByUserId(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM applications WHERE user_id = ? AND recruiter_id IS NOT NULL";
        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
            return 0;
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public int countDirectContactsByUserId(int userId) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT r.recruiter_id) FROM recruiters r " + USER_SCOPE_CLAUSE +
                     "AND ((r.email IS NOT NULL AND r.email != '') OR (r.phone IS NOT NULL AND r.phone != ''))";
        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
            return 0;
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public int countAssociatedApplications(int recruiterId, int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM applications WHERE recruiter_id = ? AND user_id = ?";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, recruiterId);
            stmt.setInt(2, userId);

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
    public int countAssociatedInterviews(int recruiterId, int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM interviews i " +
                     "JOIN applications a ON i.application_id = a.application_id " +
                     "WHERE a.recruiter_id = ? AND a.user_id = ?";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, recruiterId);
            stmt.setInt(2, userId);

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
    public List<Application> findAssociatedApplications(int recruiterId, int userId) throws SQLException {
        String sql = "SELECT a.application_id, a.user_id, a.company_id, a.recruiter_id, a.job_role, a.job_type, " +
                     "       a.application_date, a.deadline, a.expected_salary, a.status, a.notes, a.created_at, " +
                     "       c.company_name, c.website AS company_website, c.industry AS company_industry, c.location AS company_location " +
                     "FROM applications a " +
                     "JOIN companies c ON a.company_id = c.company_id " +
                     "WHERE a.recruiter_id = ? AND a.user_id = ? " +
                     "ORDER BY a.application_date DESC";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, recruiterId);
            stmt.setInt(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                List<Application> list = new ArrayList<>();
                while (rs.next()) {
                    int appId = rs.getInt("application_id");
                    int companyId = rs.getInt("company_id");
                    String jobRole = rs.getString("job_role");
                    JobType jobType = JobType.fromString(rs.getString("job_type"));
                    Date appDateVal = rs.getDate("application_date");
                    LocalDate appDate = appDateVal != null ? appDateVal.toLocalDate() : LocalDate.now();
                    Date deadlineVal = rs.getDate("deadline");
                    LocalDate deadline = deadlineVal != null ? deadlineVal.toLocalDate() : null;
                    BigDecimal salary = rs.getBigDecimal("expected_salary");
                    ApplicationStatus status = ApplicationStatus.fromString(rs.getString("status"));
                    String notes = rs.getString("notes");

                    Company company = new Company(companyId, rs.getString("company_name"),
                            rs.getString("company_website"), rs.getString("company_industry"),
                            rs.getString("company_location"), null, null);

                    Application app = new Application(appId, userId, companyId, recruiterId,
                            jobRole, jobType, appDate, deadline, null, null, salary, status, notes, null, null);
                    app.setCompany(company);
                    list.add(app);
                }
                return list;
            }
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public List<Interview> findAssociatedInterviews(int recruiterId, int userId) throws SQLException {
        String sql = "SELECT i.interview_id, i.application_id, i.round_name, i.interview_date, i.interview_mode, " +
                     "       i.interview_location, i.meeting_link, i.interviewer_name, i.feedback, i.outcome, i.created_at, " +
                     "       a.job_role, c.company_name " +
                     "FROM interviews i " +
                     "JOIN applications a ON i.application_id = a.application_id " +
                     "JOIN companies c ON a.company_id = c.company_id " +
                     "WHERE a.recruiter_id = ? AND a.user_id = ? " +
                     "ORDER BY i.interview_date DESC";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, recruiterId);
            stmt.setInt(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                List<Interview> list = new ArrayList<>();
                while (rs.next()) {
                    int interviewId = rs.getInt("interview_id");
                    int appId = rs.getInt("application_id");
                    String roundName = rs.getString("round_name");
                    Timestamp ts = rs.getTimestamp("interview_date");
                    LocalDateTime dt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
                    InterviewMode mode = InterviewMode.fromString(rs.getString("interview_mode"));
                    String loc = rs.getString("interview_location");
                    String link = rs.getString("meeting_link");
                    String interviewer = rs.getString("interviewer_name");
                    String feedback = rs.getString("feedback");
                    InterviewOutcome outcome = InterviewOutcome.fromString(rs.getString("outcome"));
                    Timestamp cat = rs.getTimestamp("created_at");
                    LocalDateTime createdAt = cat != null ? cat.toLocalDateTime() : LocalDateTime.now();

                    Interview interview = new Interview(interviewId, appId, roundName, dt, mode, loc, link, interviewer, feedback, outcome, createdAt, createdAt);

                    Company comp = new Company(rs.getString("company_name"));
                    Application app = new Application(userId, 0, rs.getString("job_role"), LocalDate.now());
                    app.setId(appId);
                    app.setCompany(comp);
                    interview.setApplication(app);

                    list.add(interview);
                }
                return list;
            }
        } finally {
            releaseConnection(conn);
        }
    }

    @Override
    public boolean isCompanyAccessibleToUser(int companyId, int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM applications WHERE company_id = ? AND user_id = ?";

        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, companyId);
            stmt.setInt(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            return false;
        } finally {
            releaseConnection(conn);
        }
    }

    private Recruiter mapResultSetToRecruiter(ResultSet rs) throws SQLException {
        int recruiterId = rs.getInt("recruiter_id");
        int compIdVal = rs.getInt("company_id");
        Integer companyId = rs.wasNull() ? null : compIdVal;
        String name = rs.getString("recruiter_name");
        String email = rs.getString("email");
        String phone = rs.getString("phone");
        String linkedin = rs.getString("linkedin_url");
        String notes = rs.getString("notes");
        Timestamp cat = rs.getTimestamp("created_at");
        LocalDateTime createdAt = cat != null ? cat.toLocalDateTime() : LocalDateTime.now();

        Recruiter recruiter = new Recruiter(recruiterId, companyId, name, email, phone, linkedin, notes, createdAt, createdAt);

        if (companyId != null) {
            String companyName = rs.getString("company_name");
            String website = rs.getString("company_website");
            String industry = rs.getString("company_industry");
            String location = rs.getString("company_location");
            Company company = new Company(companyId, companyName, website, industry, location, null, null);
            recruiter.setCompany(company);
        }

        try {
            int appCount = rs.getInt("app_count");
            recruiter.setAssociatedApplicationsCount(appCount);
        } catch (SQLException ignored) {
        }

        return recruiter;
    }
}

