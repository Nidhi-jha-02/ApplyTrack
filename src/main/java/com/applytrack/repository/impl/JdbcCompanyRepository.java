package com.applytrack.repository.impl;

import com.applytrack.database.DatabaseConnection;
import com.applytrack.model.Company;
import com.applytrack.repository.CompanyRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC Implementation of CompanyRepository querying MariaDB.
 *
 * Demonstrates:
 * - PreparedStatement with parameter binding to prevent SQL injection (Unit 3)
 * - Statement.RETURN_GENERATED_KEYS for auto-increment keys (Unit 3)
 * - try-with-resources and proper connection lifecycle management (Unit 3)
 * - Optional<T> return types (Unit 3)
 */
public class JdbcCompanyRepository implements CompanyRepository {

    private final Connection injectedConnection;

    public JdbcCompanyRepository() {
        this.injectedConnection = null;
    }

    public JdbcCompanyRepository(Connection connection) {
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
    public Optional<Company> findById(int companyId) throws SQLException {
        String sql = "SELECT company_id, company_name, website, industry, location, created_at, updated_at " +
                     "FROM companies WHERE company_id = ?";
        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, companyId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToCompany(rs));
                }
            }
        } finally {
            releaseConnection(conn);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Company> findByName(String companyName) throws SQLException {
        if (companyName == null || companyName.trim().isEmpty()) {
            return Optional.empty();
        }
        String sql = "SELECT company_id, company_name, website, industry, location, created_at, updated_at " +
                     "FROM companies WHERE LOWER(company_name) = LOWER(?) LIMIT 1";
        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, companyName.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToCompany(rs));
                }
            }
        } finally {
            releaseConnection(conn);
        }
        return Optional.empty();
    }

    @Override
    public Company findOrCreateByName(String companyName) throws SQLException {
        if (companyName == null || companyName.trim().isEmpty()) {
            throw new IllegalArgumentException("Company name cannot be blank.");
        }
        String cleanName = companyName.trim();
        Optional<Company> existing = findByName(cleanName);
        if (existing.isPresent()) {
            return existing.get();
        }

        Company newCompany = new Company(cleanName);
        return save(newCompany);
    }

    @Override
    public List<Company> findAll() throws SQLException {
        String sql = "SELECT company_id, company_name, website, industry, location, created_at, updated_at " +
                     "FROM companies ORDER BY company_name ASC";
        List<Company> list = new ArrayList<>();
        Connection conn = acquireConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToCompany(rs));
            }
        } finally {
            releaseConnection(conn);
        }
        return list;
    }

    @Override
    public Company save(Company company) throws SQLException {
        if (company == null) {
            throw new IllegalArgumentException("Company cannot be null.");
        }

        Connection conn = acquireConnection();
        try {
            if (company.getId() <= 0) {
                // INSERT
                String sql = "INSERT INTO companies (company_name, website, industry, location) VALUES (?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, company.getCompanyName());
                    stmt.setString(2, company.getWebsite());
                    stmt.setString(3, company.getIndustry());
                    stmt.setString(4, company.getLocation());

                    stmt.executeUpdate();

                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            company.setId(keys.getInt(1));
                        }
                    }
                }
            } else {
                // UPDATE
                String sql = "UPDATE companies SET company_name = ?, website = ?, industry = ?, location = ? " +
                             "WHERE company_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, company.getCompanyName());
                    stmt.setString(2, company.getWebsite());
                    stmt.setString(3, company.getIndustry());
                    stmt.setString(4, company.getLocation());
                    stmt.setInt(5, company.getId());

                    stmt.executeUpdate();
                }
            }
            return company;
        } finally {
            releaseConnection(conn);
        }
    }

    private Company mapRowToCompany(ResultSet rs) throws SQLException {
        int id = rs.getInt("company_id");
        String name = rs.getString("company_name");
        String website = rs.getString("website");
        String industry = rs.getString("industry");
        String location = rs.getString("location");
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");

        LocalDateTime created = createdAt != null ? createdAt.toLocalDateTime() : LocalDateTime.now();
        LocalDateTime updated = updatedAt != null ? updatedAt.toLocalDateTime() : LocalDateTime.now();

        return new Company(id, name, website, industry, location, created, updated);
    }
}

