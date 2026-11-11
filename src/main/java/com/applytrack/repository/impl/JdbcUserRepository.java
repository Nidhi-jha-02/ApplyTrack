package com.applytrack.repository.impl;

import com.applytrack.database.DatabaseConnection;
import com.applytrack.model.User;
import com.applytrack.repository.UserRepository;

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
 * JDBC implementation of UserRepository for MariaDB/MySQL.
 *
 * Demonstrates:
 * - Interface Realization (implements UserRepository) - Unit 3
 * - Polymorphic data access - Unit 3
 * - PreparedStatement with parameterized queries to prevent SQL Injection - Unit 3
 * - Auto-generated key retrieval (Statement.RETURN_GENERATED_KEYS) - Unit 3
 * - Try-with-resources for deterministic resource cleanup - Unit 3
 * - Exception Handling (SQLException) - Unit 3
 */
public class JdbcUserRepository implements UserRepository {

    @Override
    public User save(User user) throws SQLException {
        if (user == null) {
            throw new IllegalArgumentException("User entity to save cannot be null.");
        }

        String sql = "INSERT INTO users (full_name, email, password_hash) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getEmail().toLowerCase().trim());
            stmt.setString(3, user.getPasswordHash());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    user.setId(generatedId);
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }

            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        }
    }

    @Override
    public Optional<User> findById(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            return Optional.empty();
        }

        String sql = "SELECT user_id, full_name, email, password_hash, created_at, updated_at " +
                     "FROM users WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(String email) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }

        String sql = "SELECT user_id, full_name, email, password_hash, created_at, updated_at " +
                     "FROM users WHERE LOWER(email) = LOWER(?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean existsByEmail(String email) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT 1 FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, full_name, email, password_hash, created_at, updated_at FROM users";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
        }
        return users;
    }

    @Override
    public boolean update(User user) throws SQLException {
        if (user == null || user.getId() <= 0) {
            return false;
        }

        String sql = "UPDATE users SET full_name = ?, email = ?, password_hash = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getEmail().toLowerCase().trim());
            stmt.setString(3, user.getPasswordHash());
            stmt.setInt(4, user.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deleteById(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            return false;
        }

        String sql = "DELETE FROM users WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Helper method mapping a ResultSet row to a User entity (Unit 2 Modularization).
     */
    private User mapRowToUser(ResultSet rs) throws SQLException {
        int userId = rs.getInt("user_id");
        String fullName = rs.getString("full_name");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");

        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");

        LocalDateTime createdAt = (createdTs != null) ? createdTs.toLocalDateTime() : LocalDateTime.now();
        LocalDateTime updatedAt = (updatedTs != null) ? updatedTs.toLocalDateTime() : LocalDateTime.now();

        return new User(userId, fullName, email, passwordHash, createdAt, updatedAt);
    }
}

