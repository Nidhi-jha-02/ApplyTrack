package com.applytrack.repository;

import com.applytrack.database.DatabaseConnection;
import com.applytrack.model.User;
import com.applytrack.model.UserPreferences;
import com.applytrack.model.enums.AppTheme;
import com.applytrack.model.enums.DateRangeFilter;
import com.applytrack.repository.impl.JdbcSettingsRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Live Database Integration Tests for JdbcSettingsRepository against real MariaDB instance.
 *
 * Demonstrates:
 * - Integration Testing against real relational schema (Unit 3)
 * - Safe test rollbacks preserving baseline demo state (Unit 3)
 * - UPSERT (ON DUPLICATE KEY UPDATE) verification (Unit 3)
 * - Multi-tenant security isolation (Unit 3)
 */
public class JdbcSettingsRepositoryLiveTest {

    private static final int DEMO_USER_ID = 1; // Alex Mercer demo user
    private static final int NON_EXISTENT_USER_ID = 999999;
    private static JdbcSettingsRepository repository;

    @BeforeAll
    static void checkDatabaseAvailability() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            assumeTrue(conn != null && !conn.isClosed(), "MariaDB database is available for live testing.");
            repository = new JdbcSettingsRepository();
        } catch (SQLException e) {
            assumeTrue(false, "Skipping live tests: MariaDB connection unavailable (" + e.getMessage() + ")");
        }
    }

    @Test
    @DisplayName("Live: getUserProfile returns seeded demo user records")
    void testGetUserProfileLive() throws SQLException {
        Optional<User> userOpt = repository.getUserProfile(DEMO_USER_ID);
        assertTrue(userOpt.isPresent(), "Demo user with ID 1 should exist in database");

        User user = userOpt.get();
        assertEquals(DEMO_USER_ID, user.getId());
        assertNotNull(user.getFullName());
        assertNotNull(user.getEmail());
        assertNotNull(user.getPasswordHash());
    }

    @Test
    @DisplayName("Live: updateUserProfile updates candidate information and safely reverts")
    void testUpdateUserProfileLive() throws SQLException {
        User original = repository.getUserProfile(DEMO_USER_ID).orElseThrow();
        String originalName = original.getFullName();
        String originalEmail = original.getEmail();

        try {
            // Update profile
            boolean updated = repository.updateUserProfile(DEMO_USER_ID, "Alex Mercer Updated", "alex.updated@applytrack.io");
            assertTrue(updated, "Profile update statement should affect 1 row");

            User fetched = repository.getUserProfile(DEMO_USER_ID).orElseThrow();
            assertEquals("Alex Mercer Updated", fetched.getFullName());
            assertEquals("alex.updated@applytrack.io", fetched.getEmail());

        } finally {
            // Revert back to original values to preserve test repeatability
            repository.updateUserProfile(DEMO_USER_ID, originalName, originalEmail);
        }
    }

    @Test
    @DisplayName("Live: updateUserPassword updates hash and safely reverts")
    void testUpdateUserPasswordLive() throws SQLException {
        User original = repository.getUserProfile(DEMO_USER_ID).orElseThrow();
        String originalHash = original.getPasswordHash();

        try {
            String tempHash = "$2a$10$abcdefghijklmnopqrstuvwxyzA0B1C2D3E4F5G6H7I8J9K0L1M2N";
            boolean updated = repository.updateUserPassword(DEMO_USER_ID, tempHash);
            assertTrue(updated, "Password update statement should affect 1 row");

            User fetched = repository.getUserProfile(DEMO_USER_ID).orElseThrow();
            assertEquals(tempHash, fetched.getPasswordHash());

        } finally {
            // Revert back to original password hash
            repository.updateUserPassword(DEMO_USER_ID, originalHash);
        }
    }

    @Test
    @DisplayName("Live: saveOrUpdateUserPreferences persists preferences and reads back")
    void testSaveOrUpdatePreferencesLive() throws SQLException {
        UserPreferences prefs = new UserPreferences(DEMO_USER_ID);
        prefs.setTheme(AppTheme.DARK);
        prefs.setDefaultLandingPage("APPLICATIONS");
        prefs.setPreferredAnalyticsRange(DateRangeFilter.LAST_6_MONTHS);
        prefs.setReminderUpcomingInterviews(true);
        prefs.setReminderApplicationDeadlines(false);

        UserPreferences saved = repository.saveOrUpdateUserPreferences(prefs);
        assertNotNull(saved);
        assertEquals(DEMO_USER_ID, saved.getUserId());
        assertEquals(AppTheme.DARK, saved.getTheme());
        assertEquals("APPLICATIONS", saved.getDefaultLandingPage());
        assertEquals(DateRangeFilter.LAST_6_MONTHS, saved.getPreferredAnalyticsRange());
        assertTrue(saved.isReminderUpcomingInterviews());
        assertFalse(saved.isReminderApplicationDeadlines());

        // Read back independently
        Optional<UserPreferences> readBack = repository.getUserPreferences(DEMO_USER_ID);
        assertTrue(readBack.isPresent());
        assertEquals(AppTheme.DARK, readBack.get().getTheme());
    }

    @Test
    @DisplayName("Live: Non-existent user profile returns Optional.empty()")
    void testNonExistentUserReturnsEmptyProfile() throws SQLException {
        Optional<User> userOpt = repository.getUserProfile(NON_EXISTENT_USER_ID);
        assertTrue(userOpt.isEmpty(), "Non-existent user ID should return empty Optional");
    }

    @Test
    @DisplayName("Live: Multi-tenant security: non-existent user preferences returns Optional.empty()")
    void testPreferencesUserIsolationLive() throws SQLException {
        Optional<UserPreferences> prefs = repository.getUserPreferences(NON_EXISTENT_USER_ID);
        assertTrue(prefs.isEmpty(), "Preferences for unlinked user ID must return empty");
    }
}
