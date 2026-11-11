package com.applytrack.repository.impl;

import com.applytrack.database.DatabaseConnection;
import com.applytrack.model.User;
import com.applytrack.model.UserPreferences;
import com.applytrack.model.enums.AppTheme;
import com.applytrack.model.enums.DateRangeFilter;
import com.applytrack.repository.SettingsRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * JDBC implementation of SettingsRepository using MariaDB / MySQL.
 *
 * Demonstrates:
 * - PreparedStatement with strict parameterized queries (Unit 3)
 * - Automatic schema migration / table initialization (Unit 3)
 * - Try-with-resources deterministic resource cleanup (Unit 3)
 * - Multi-tenant security (all queries bound to user_id = ?) (Unit 3)
 * - UPSERT logic with ON DUPLICATE KEY UPDATE (Unit 3)
 */
public class JdbcSettingsRepository implements SettingsRepository {

    public JdbcSettingsRepository() {
        ensurePreferencesTableExists();
    }

    private void ensurePreferencesTableExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS user_preferences (
                preference_id INT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NOT NULL UNIQUE,
                theme VARCHAR(20) NOT NULL DEFAULT 'LIGHT',
                default_landing_page VARCHAR(50) NOT NULL DEFAULT 'DASHBOARD',
                preferred_analytics_range VARCHAR(50) NOT NULL DEFAULT 'ALL_TIME',
                reminder_upcoming_interviews BOOLEAN NOT NULL DEFAULT TRUE,
                reminder_application_deadlines BOOLEAN NOT NULL DEFAULT TRUE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                CONSTRAINT fk_pref_user FOREIGN KEY (user_id)
                    REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            // Log warning but don't prevent repository instantiation if connection is currently closed
            System.err.println("Notice: Could not auto-verify user_preferences table: " + e.getMessage());
        }
    }

    @Override
    public Optional<User> getUserProfile(int userId) throws SQLException {
        if (userId <= 0) {
            return Optional.empty();
        }

        String sql = "SELECT user_id, full_name, email, password_hash, created_at, updated_at " +
                     "FROM users WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean updateUserProfile(int userId, String fullName, String email) throws SQLException {
        if (userId <= 0 || fullName == null || email == null) {
            return false;
        }

        String sql = "UPDATE users SET full_name = ?, email = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, fullName.trim());
            stmt.setString(2, email.trim().toLowerCase());
            stmt.setInt(3, userId);

            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean updateUserPassword(int userId, String newPasswordHash) throws SQLException {
        if (userId <= 0 || newPasswordHash == null || newPasswordHash.isBlank()) {
            return false;
        }

        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newPasswordHash);
            stmt.setInt(2, userId);

            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public Optional<UserPreferences> getUserPreferences(int userId) throws SQLException {
        if (userId <= 0) {
            return Optional.empty();
        }

        String sql = "SELECT preference_id, user_id, theme, default_landing_page, " +
                     "preferred_analytics_range, reminder_upcoming_interviews, reminder_application_deadlines, " +
                     "created_at, updated_at FROM user_preferences WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToPreferences(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public UserPreferences saveOrUpdateUserPreferences(UserPreferences preferences) throws SQLException {
        if (preferences == null || preferences.getUserId() <= 0) {
            throw new IllegalArgumentException("UserPreferences cannot be null and must have valid userId.");
        }

        String sql = """
            INSERT INTO user_preferences (
                user_id, theme, default_landing_page, preferred_analytics_range,
                reminder_upcoming_interviews, reminder_application_deadlines
            ) VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                theme = VALUES(theme),
                default_landing_page = VALUES(default_landing_page),
                preferred_analytics_range = VALUES(preferred_analytics_range),
                reminder_upcoming_interviews = VALUES(reminder_upcoming_interviews),
                reminder_application_deadlines = VALUES(reminder_application_deadlines)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, preferences.getUserId());
            stmt.setString(2, preferences.getTheme() != null ? preferences.getTheme().name() : AppTheme.LIGHT.name());
            stmt.setString(3, preferences.getDefaultLandingPage() != null ? preferences.getDefaultLandingPage() : "DASHBOARD");
            stmt.setString(4, preferences.getPreferredAnalyticsRange() != null ? preferences.getPreferredAnalyticsRange().name() : DateRangeFilter.ALL_TIME.name());
            stmt.setBoolean(5, preferences.isReminderUpcomingInterviews());
            stmt.setBoolean(6, preferences.isReminderApplicationDeadlines());

            stmt.executeUpdate();

            // Refresh and return complete stored preference object
            return getUserPreferences(preferences.getUserId()).orElse(preferences);
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("user_id");
        String name = rs.getString("full_name");
        String email = rs.getString("email");
        String hash = rs.getString("password_hash");

        Timestamp createdTs = rs.getTimestamp("created_at");
        LocalDateTime created = (createdTs != null) ? createdTs.toLocalDateTime() : null;

        Timestamp updatedTs = rs.getTimestamp("updated_at");
        LocalDateTime updated = (updatedTs != null) ? updatedTs.toLocalDateTime() : null;

        return new User(id, name, email, hash, created, updated);
    }

    private UserPreferences mapRowToPreferences(ResultSet rs) throws SQLException {
        int prefId = rs.getInt("preference_id");
        int userId = rs.getInt("user_id");
        AppTheme theme = AppTheme.fromString(rs.getString("theme"));
        String landing = rs.getString("default_landing_page");
        DateRangeFilter filter = DateRangeFilter.fromString(rs.getString("preferred_analytics_range"));
        boolean reminderInterviews = rs.getBoolean("reminder_upcoming_interviews");
        boolean reminderDeadlines = rs.getBoolean("reminder_application_deadlines");

        Timestamp createdTs = rs.getTimestamp("created_at");
        LocalDateTime created = (createdTs != null) ? createdTs.toLocalDateTime() : null;

        Timestamp updatedTs = rs.getTimestamp("updated_at");
        LocalDateTime updated = (updatedTs != null) ? updatedTs.toLocalDateTime() : null;

        return new UserPreferences(prefId, userId, theme, landing, filter, reminderInterviews, reminderDeadlines, created, updated);
    }
}

