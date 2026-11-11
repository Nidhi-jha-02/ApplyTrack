package com.applytrack.repository;

import com.applytrack.model.User;
import com.applytrack.model.UserPreferences;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Repository interface contract for retrieving and persisting user profile data and application preferences.
 *
 * Demonstrates:
 * - Interface abstraction & Decoupled Architecture (Unit 3)
 * - Strict User-Specific Scoping (user_id) (Unit 3)
 * - Checked Exception handling with SQLException (Unit 3)
 */
public interface SettingsRepository {

    /**
     * Retrieves the profile and account details for the specified user ID.
     */
    Optional<User> getUserProfile(int userId) throws SQLException;

    /**
     * Updates the full name and email address of the specified user.
     */
    boolean updateUserProfile(int userId, String fullName, String email) throws SQLException;

    /**
     * Updates the BCrypt password hash of the specified user.
     */
    boolean updateUserPassword(int userId, String newPasswordHash) throws SQLException;

    /**
     * Retrieves the saved preferences for the specified user, if any.
     */
    Optional<UserPreferences> getUserPreferences(int userId) throws SQLException;

    /**
     * Saves or updates the preferences for the user (upsert).
     */
    UserPreferences saveOrUpdateUserPreferences(UserPreferences preferences) throws SQLException;
}

