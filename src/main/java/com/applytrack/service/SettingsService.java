package com.applytrack.service;

import com.applytrack.exception.AuthenticationException;
import com.applytrack.exception.ValidationException;
import com.applytrack.model.User;
import com.applytrack.model.UserPreferences;
import com.applytrack.repository.SettingsRepository;
import com.applytrack.repository.UserRepository;
import com.applytrack.repository.impl.JdbcSettingsRepository;
import com.applytrack.repository.impl.JdbcUserRepository;
import com.applytrack.session.UserSession;
import com.applytrack.util.ThemeManager;
import com.applytrack.util.ValidationUtil;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Service orchestrating user profile updates, secure BCrypt password changes,
 * and user-specific application preferences management.
 *
 * Demonstrates:
 * - Layered Architecture & Separation of Concerns (Unit 3)
 * - Constructor Overloading for Dependency Injection (Unit 2 & Unit 3)
 * - Defensive Domain Validation with custom exceptions (Unit 3)
 * - BCrypt Password Hashing with Salting (Unit 3)
 * - User-Specific Data Security & Session synchronization (Unit 3)
 */
public class SettingsService {

    private final SettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    public SettingsService() {
        this(new JdbcSettingsRepository(), new JdbcUserRepository(), new AuthService());
    }

    public SettingsService(SettingsRepository settingsRepository, UserRepository userRepository, AuthService authService) {
        if (settingsRepository == null) throw new IllegalArgumentException("SettingsRepository cannot be null.");
        if (userRepository == null) throw new IllegalArgumentException("UserRepository cannot be null.");
        if (authService == null) throw new IllegalArgumentException("AuthService cannot be null.");

        this.settingsRepository = settingsRepository;
        this.userRepository = userRepository;
        this.authService = authService;
    }

    /**
     * Retrieves the profile of the candidate matching the specified user ID.
     */
    public User getUserProfile(int userId) throws SQLException, ValidationException {
        if (userId <= 0) {
            throw new ValidationException("Invalid user ID specified.");
        }
        return settingsRepository.getUserProfile(userId)
                .orElseThrow(() -> new ValidationException("User profile not found for ID: " + userId));
    }

    /**
     * Updates candidate profile information (full name and email address).
     * Validates input completeness, email syntax, and unique email constraints.
     */
    public User updateProfile(int userId, String fullName, String email)
            throws SQLException, ValidationException {

        if (userId <= 0) {
            throw new ValidationException("Invalid user ID specified.");
        }
        if (!ValidationUtil.isNotEmpty(fullName)) {
            throw new ValidationException("Full name is required.");
        }
        if (!ValidationUtil.isNotEmpty(email)) {
            throw new ValidationException("Email address is required.");
        }

        String cleanName = fullName.trim();
        String cleanEmail = email.trim().toLowerCase();

        if (!ValidationUtil.isValidEmail(cleanEmail)) {
            throw new ValidationException("Invalid email format. Please provide a valid address.");
        }

        // Check if new email is already claimed by a different user
        Optional<User> existingUserWithEmail = userRepository.findByEmail(cleanEmail);
        if (existingUserWithEmail.isPresent() && existingUserWithEmail.get().getId() != userId) {
            throw new ValidationException("The email address '" + cleanEmail + "' is already registered to another account.");
        }

        boolean updated = settingsRepository.updateUserProfile(userId, cleanName, cleanEmail);
        if (!updated) {
            throw new SQLException("Failed to update user profile in the database.");
        }

        User updatedUser = getUserProfile(userId);

        // Synchronize active session if current user is the one being updated
        UserSession session = UserSession.getInstance();
        if (session.isAuthenticated() && session.getUserId() == userId) {
            session.setCurrentUser(updatedUser);
        }

        return updatedUser;
    }

    /**
     * Securely changes the user's password following verification of the current password.
     */
    public void changePassword(int userId, String currentPassword, String newPassword, String confirmPassword)
            throws SQLException, ValidationException, AuthenticationException {

        if (userId <= 0) {
            throw new ValidationException("Invalid user ID specified.");
        }
        if (!ValidationUtil.isNotEmpty(currentPassword)) {
            throw new ValidationException("Current password is required.");
        }
        if (!ValidationUtil.isNotEmpty(newPassword)) {
            throw new ValidationException("New password is required.");
        }
        if (!ValidationUtil.isNotEmpty(confirmPassword)) {
            throw new ValidationException("Please confirm your new password.");
        }

        User user = getUserProfile(userId);

        // 1. Verify current password with BCrypt
        if (!authService.verifyPassword(currentPassword, user.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect.");
        }

        // 2. Enforce minimum length policy
        if (newPassword.length() < AuthService.MIN_PASSWORD_LENGTH) {
            throw new ValidationException("New password must be at least " + AuthService.MIN_PASSWORD_LENGTH + " characters long.");
        }

        // 3. Confirm matching passwords
        if (!newPassword.equals(confirmPassword)) {
            throw new ValidationException("New password and confirmation do not match.");
        }

        // 4. Reject password reuse
        if (currentPassword.equals(newPassword)) {
            throw new ValidationException("New password cannot be identical to your current password.");
        }

        // 5. Generate secure BCrypt hash with 10 salt rounds
        String newHash = authService.hashPassword(newPassword);

        // 6. Update database record
        boolean updated = settingsRepository.updateUserPassword(userId, newHash);
        if (!updated) {
            throw new SQLException("Failed to update password in the database.");
        }
    }

    /**
     * Retrieves application preferences for the user, returning sensible defaults if not yet customized.
     */
    public UserPreferences getPreferences(int userId) throws SQLException {
        if (userId <= 0) {
            return UserPreferences.defaultForUser(0);
        }
        return settingsRepository.getUserPreferences(userId)
                .orElseGet(() -> UserPreferences.defaultForUser(userId));
    }

    /**
     * Persists customized application preferences and updates the active runtime theme.
     */
    public UserPreferences savePreferences(UserPreferences preferences) throws SQLException, ValidationException {
        if (preferences == null || preferences.getUserId() <= 0) {
            throw new ValidationException("Preferences must be associated with a valid user ID.");
        }

        UserPreferences saved = settingsRepository.saveOrUpdateUserPreferences(preferences);

        // Sync active runtime theme
        if (saved.getTheme() != null) {
            ThemeManager.setCurrentTheme(saved.getTheme());
        }

        return saved;
    }

    /**
     * Securely terminates the candidate user session.
     */
    public void logout() {
        authService.logout();
    }
}

