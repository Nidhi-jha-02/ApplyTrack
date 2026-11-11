package com.applytrack.service;

import com.applytrack.exception.AuthenticationException;
import com.applytrack.exception.ValidationException;
import com.applytrack.model.User;
import com.applytrack.model.UserPreferences;
import com.applytrack.model.enums.AppTheme;
import com.applytrack.model.enums.DateRangeFilter;
import com.applytrack.repository.SettingsRepository;
import com.applytrack.repository.UserRepository;
import com.applytrack.session.UserSession;
import com.applytrack.util.ThemeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Unit Tests for SettingsService covering User Profiles, Security, and Preferences.
 *
 * Demonstrates:
 * - Mock/Stub Unit Testing (Unit 3)
 * - Domain Validation & Custom Exception handling (Unit 3)
 * - BCrypt Password Hashing & Verification (Unit 3)
 * - Multi-tenant security isolation across users (Unit 3)
 * - Session state management & logout cleanup (Unit 3)
 */
public class SettingsServiceTest {

    private StubSettingsRepository stubSettingsRepo;
    private StubUserRepository stubUserRepo;
    private AuthService authService;
    private SettingsService settingsService;

    @BeforeEach
    void setUp() {
        stubUserRepo = new StubUserRepository();
        stubSettingsRepo = new StubSettingsRepository(stubUserRepo);
        authService = new AuthService(stubUserRepo);
        settingsService = new SettingsService(stubSettingsRepo, stubUserRepo, authService);

        // Clear session before each test
        UserSession.getInstance().clearSession();

        // Seed primary user (ID 1: Alex Mercer)
        String hash1 = BCrypt.hashpw("password123", BCrypt.gensalt(10));
        User user1 = new User(1, "Alex Mercer", "alex@applytrack.io", hash1, LocalDateTime.now().minusDays(30), LocalDateTime.now());
        stubUserRepo.users.put(1, user1);

        // Seed secondary user (ID 2: Jane Doe)
        String hash2 = BCrypt.hashpw("janeSecure123", BCrypt.gensalt(10));
        User user2 = new User(2, "Jane Doe", "jane@applytrack.io", hash2, LocalDateTime.now().minusDays(10), LocalDateTime.now());
        stubUserRepo.users.put(2, user2);
    }

    // =========================================================================
    // 1. Profile Management Tests
    // =========================================================================

    @Test
    @DisplayName("1. Fetching the current user profile succeeds for valid user ID")
    void testFetchUserProfileSuccess() throws SQLException, ValidationException {
        User user = settingsService.getUserProfile(1);
        assertNotNull(user);
        assertEquals(1, user.getId());
        assertEquals("Alex Mercer", user.getFullName());
        assertEquals("alex@applytrack.io", user.getEmail());
    }

    @Test
    @DisplayName("2. Updating user's name updates record and returns new profile")
    void testUpdateUserNameSuccess() throws SQLException, ValidationException {
        User updated = settingsService.updateProfile(1, "Alexander Mercer Jr.", "alex@applytrack.io");
        assertNotNull(updated);
        assertEquals("Alexander Mercer Jr.", updated.getFullName());
        assertEquals("alex@applytrack.io", updated.getEmail());

        User fromRepo = stubSettingsRepo.getUserProfile(1).orElseThrow();
        assertEquals("Alexander Mercer Jr.", fromRepo.getFullName());
    }

    @Test
    @DisplayName("3. Updating user's email updates record successfully")
    void testUpdateUserEmailSuccess() throws SQLException, ValidationException {
        User updated = settingsService.updateProfile(1, "Alex Mercer", "alex.new@university.edu");
        assertNotNull(updated);
        assertEquals("alex.new@university.edu", updated.getEmail());

        User fromRepo = stubSettingsRepo.getUserProfile(1).orElseThrow();
        assertEquals("alex.new@university.edu", fromRepo.getEmail());
    }

    @Test
    @DisplayName("4. Rejecting invalid email format throws ValidationException")
    void testUpdateProfileInvalidEmailThrows() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> settingsService.updateProfile(1, "Alex Mercer", "not-a-valid-email"));
        assertTrue(ex.getMessage().contains("Invalid email format"));
    }

    @Test
    @DisplayName("5. Handling duplicate email: throws ValidationException if email is taken by another user")
    void testUpdateProfileDuplicateEmailThrows() {
        // Attempt to change Alex Mercer's email to jane@applytrack.io (taken by Jane Doe ID 2)
        ValidationException ex = assertThrows(ValidationException.class,
                () -> settingsService.updateProfile(1, "Alex Mercer", "jane@applytrack.io"));
        assertTrue(ex.getMessage().contains("already registered"));
    }

    @Test
    @DisplayName("6. Preventing unauthorized profile updates: invalid user ID <= 0 throws ValidationException")
    void testUpdateProfileInvalidUserIdThrows() {
        assertThrows(ValidationException.class,
                () -> settingsService.updateProfile(0, "Name", "valid@test.com"));
        assertThrows(ValidationException.class,
                () -> settingsService.updateProfile(-1, "Name", "valid@test.com"));
    }

    @Test
    @DisplayName("Profile update synchronizes active UserSession immediately")
    void testProfileUpdateSyncsUserSession() throws SQLException, ValidationException {
        User user1 = stubUserRepo.users.get(1);
        UserSession.getInstance().setCurrentUser(user1);

        settingsService.updateProfile(1, "Alex Renamed", "alex.renamed@applytrack.io");

        assertEquals("Alex Renamed", UserSession.getInstance().getUserFullName());
        assertEquals("alex.renamed@applytrack.io", UserSession.getInstance().getUserEmail());
    }

    // =========================================================================
    // 2. Password & Security Tests
    // =========================================================================

    @Test
    @DisplayName("7. Changing password with correct current password succeeds")
    void testChangePasswordSuccess() throws SQLException, ValidationException, AuthenticationException {
        settingsService.changePassword(1, "password123", "newSecretPassword456", "newSecretPassword456");

        User user = stubSettingsRepo.getUserProfile(1).orElseThrow();
        // Verify new password verifies against stored hash
        assertTrue(BCrypt.checkpw("newSecretPassword456", user.getPasswordHash()));
    }

    @Test
    @DisplayName("8. Rejecting incorrect current password throws AuthenticationException")
    void testChangePasswordIncorrectCurrentPasswordThrows() {
        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> settingsService.changePassword(1, "wrongCurrentPassword", "newSecretPassword456", "newSecretPassword456"));
        assertTrue(ex.getMessage().contains("Current password is incorrect"));
    }

    @Test
    @DisplayName("9. Rejecting mismatched confirmation throws ValidationException")
    void testChangePasswordMismatchedConfirmationThrows() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> settingsService.changePassword(1, "password123", "newSecretPassword456", "differentPassword999"));
        assertTrue(ex.getMessage().contains("do not match"));
    }

    @Test
    @DisplayName("10. Rejecting invalid new password (< 8 chars) throws ValidationException")
    void testChangePasswordTooShortThrows() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> settingsService.changePassword(1, "password123", "short", "short"));
        assertTrue(ex.getMessage().contains("at least 8 characters"));
    }

    @Test
    @DisplayName("Rejecting password reuse: new password cannot equal current password")
    void testChangePasswordReuseCurrentThrows() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> settingsService.changePassword(1, "password123", "password123", "password123"));
        assertTrue(ex.getMessage().contains("cannot be identical"));
    }

    @Test
    @DisplayName("11. Verifying that only BCrypt hashes are stored in the database")
    void testChangePasswordStoresBcryptHash() throws SQLException, ValidationException, AuthenticationException {
        settingsService.changePassword(1, "password123", "brandNewPassword789", "brandNewPassword789");

        User user = stubSettingsRepo.getUserProfile(1).orElseThrow();
        String hash = user.getPasswordHash();

        assertNotEquals("brandNewPassword789", hash);
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"),
                "Stored hash must be a valid BCrypt hash string");
        assertEquals(60, hash.length(), "Standard BCrypt hash length is 60 characters");
    }

    @Test
    @DisplayName("12. Verifying that the new password works for subsequent login with AuthService")
    void testNewPasswordWorksForLogin() throws SQLException, ValidationException, AuthenticationException {
        settingsService.changePassword(1, "password123", "brandNewPassword789", "brandNewPassword789");

        // Old password should now fail login
        assertThrows(AuthenticationException.class, () -> authService.login("alex@applytrack.io", "password123"));

        // New password should successfully authenticate
        User loggedIn = authService.login("alex@applytrack.io", "brandNewPassword789");
        assertNotNull(loggedIn);
        assertEquals(1, loggedIn.getId());
        assertEquals(1, UserSession.getInstance().getUserId());
    }

    // =========================================================================
    // 3. Preferences Tests
    // =========================================================================

    @Test
    @DisplayName("13. Saving theme preference persists to repository")
    void testSaveThemePreference() throws SQLException, ValidationException {
        UserPreferences prefs = UserPreferences.defaultForUser(1);
        prefs.setTheme(AppTheme.DARK);

        settingsService.savePreferences(prefs);

        UserPreferences saved = stubSettingsRepo.getUserPreferences(1).orElseThrow();
        assertEquals(AppTheme.DARK, saved.getTheme());
        assertEquals(AppTheme.DARK, ThemeManager.getCurrentTheme());
    }

    @Test
    @DisplayName("14. Loading saved theme preference returns user's custom theme")
    void testLoadThemePreference() throws SQLException, ValidationException {
        UserPreferences prefs = UserPreferences.defaultForUser(1);
        prefs.setTheme(AppTheme.DARK);
        settingsService.savePreferences(prefs);

        UserPreferences loaded = settingsService.getPreferences(1);
        assertEquals(AppTheme.DARK, loaded.getTheme());
    }

    @Test
    @DisplayName("15. Restoring theme after application restart returns correct theme")
    void testThemeRestoration() throws SQLException, ValidationException {
        // User saves dark theme
        UserPreferences prefs = UserPreferences.defaultForUser(1);
        prefs.setTheme(AppTheme.DARK);
        settingsService.savePreferences(prefs);

        // Simulate app restart: re-instantiate service and query preferences
        SettingsService freshService = new SettingsService(stubSettingsRepo, stubUserRepo, authService);
        UserPreferences restoredPrefs = freshService.getPreferences(1);

        assertEquals(AppTheme.DARK, restoredPrefs.getTheme());
    }

    @Test
    @DisplayName("16. Saving and loading dashboard preferences (landing page & analytics scope)")
    void testSaveAndLoadDashboardPreferences() throws SQLException, ValidationException {
        UserPreferences prefs = UserPreferences.defaultForUser(1);
        prefs.setDefaultLandingPage("APPLICATIONS");
        prefs.setPreferredAnalyticsRange(DateRangeFilter.LAST_3_MONTHS);

        settingsService.savePreferences(prefs);

        UserPreferences loaded = settingsService.getPreferences(1);
        assertEquals("APPLICATIONS", loaded.getDefaultLandingPage());
        assertEquals(DateRangeFilter.LAST_3_MONTHS, loaded.getPreferredAnalyticsRange());
    }

    @Test
    @DisplayName("17. Saving and loading notification alert preferences")
    void testSaveAndLoadNotificationPreferences() throws SQLException, ValidationException {
        UserPreferences prefs = UserPreferences.defaultForUser(1);
        prefs.setReminderUpcomingInterviews(false);
        prefs.setReminderApplicationDeadlines(false);

        settingsService.savePreferences(prefs);

        UserPreferences loaded = settingsService.getPreferences(1);
        assertFalse(loaded.isReminderUpcomingInterviews());
        assertFalse(loaded.isReminderApplicationDeadlines());
    }

    @Test
    @DisplayName("18. Preventing access to another user's preferences (multi-tenant isolation)")
    void testPreferencesUserIsolation() throws SQLException, ValidationException {
        // User 1 sets Dark theme
        UserPreferences prefs1 = UserPreferences.defaultForUser(1);
        prefs1.setTheme(AppTheme.DARK);
        settingsService.savePreferences(prefs1);

        // User 2 sets Light theme
        UserPreferences prefs2 = UserPreferences.defaultForUser(2);
        prefs2.setTheme(AppTheme.LIGHT);
        settingsService.savePreferences(prefs2);

        // Assert User 1 and User 2 have completely isolated preferences
        assertEquals(AppTheme.DARK, settingsService.getPreferences(1).getTheme());
        assertEquals(AppTheme.LIGHT, settingsService.getPreferences(2).getTheme());
    }

    // =========================================================================
    // 4. Session & Logout Tests
    // =========================================================================

    @Test
    @DisplayName("19. Clearing the session on logout removes active user")
    void testLogoutClearsSession() {
        UserSession.getInstance().setCurrentUser(stubUserRepo.users.get(1));
        assertTrue(UserSession.getInstance().isAuthenticated());

        settingsService.logout();

        assertFalse(UserSession.getInstance().isAuthenticated());
        assertNull(UserSession.getInstance().getCurrentUser());
        assertEquals(0, UserSession.getInstance().getUserId());
    }

    @Test
    @DisplayName("20. Preventing access to protected actions after logout")
    void testProtectedActionsAfterLogout() {
        UserSession.getInstance().setCurrentUser(stubUserRepo.users.get(1));
        settingsService.logout();

        assertFalse(UserSession.getInstance().isLoggedIn());
    }

    @Test
    @DisplayName("21. Loading correct user settings after switching accounts")
    void testSwitchAccountsLoadsCorrectSettings() throws SQLException, ValidationException {
        // Account 1 (Alex) has Dark theme
        UserPreferences p1 = UserPreferences.defaultForUser(1);
        p1.setTheme(AppTheme.DARK);
        settingsService.savePreferences(p1);

        // Account 2 (Jane) has Light theme
        UserPreferences p2 = UserPreferences.defaultForUser(2);
        p2.setTheme(AppTheme.LIGHT);
        settingsService.savePreferences(p2);

        // Sign in as Alex
        UserSession.getInstance().setCurrentUser(stubUserRepo.users.get(1));
        UserPreferences alexPrefs = settingsService.getPreferences(UserSession.getInstance().getUserId());
        assertEquals(AppTheme.DARK, alexPrefs.getTheme());

        // Logout Alex
        settingsService.logout();
        assertFalse(UserSession.getInstance().isAuthenticated());

        // Sign in as Jane
        UserSession.getInstance().setCurrentUser(stubUserRepo.users.get(2));
        UserPreferences janePrefs = settingsService.getPreferences(UserSession.getInstance().getUserId());
        assertEquals(AppTheme.LIGHT, janePrefs.getTheme());
    }

    // =========================================================================
    // In-Memory Stubs for Fast Unit Testing
    // =========================================================================

    private static class StubUserRepository implements UserRepository {
        final Map<Integer, User> users = new HashMap<>();

        @Override
        public Optional<User> findById(Integer id) {
            return Optional.ofNullable(users.get(id));
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return users.values().stream()
                    .filter(u -> u.getEmail().equalsIgnoreCase(email.trim()))
                    .findFirst();
        }

        @Override
        public boolean existsByEmail(String email) {
            return findByEmail(email).isPresent();
        }

        @Override
        public User save(User user) {
            int nextId = users.keySet().stream().max(Integer::compareTo).orElse(0) + 1;
            user.setId(nextId);
            users.put(nextId, user);
            return user;
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(users.values());
        }

        @Override
        public boolean update(User user) {
            if (user == null || !users.containsKey(user.getId())) return false;
            users.put(user.getId(), user);
            return true;
        }

        @Override
        public boolean deleteById(Integer id) {
            return users.remove(id) != null;
        }
    }

    private static class StubSettingsRepository implements SettingsRepository {
        final StubUserRepository userRepo;
        final Map<Integer, UserPreferences> preferencesMap = new HashMap<>();

        StubSettingsRepository(StubUserRepository userRepo) {
            this.userRepo = userRepo;
        }

        @Override
        public Optional<User> getUserProfile(int userId) {
            return userRepo.findById(userId);
        }

        @Override
        public boolean updateUserProfile(int userId, String fullName, String email) {
            User user = userRepo.users.get(userId);
            if (user == null) return false;
            user.setFullName(fullName);
            user.setEmail(email);
            return true;
        }

        @Override
        public boolean updateUserPassword(int userId, String newPasswordHash) {
            User user = userRepo.users.get(userId);
            if (user == null) return false;
            user.setPasswordHash(newPasswordHash);
            return true;
        }

        @Override
        public Optional<UserPreferences> getUserPreferences(int userId) {
            return Optional.ofNullable(preferencesMap.get(userId));
        }

        @Override
        public UserPreferences saveOrUpdateUserPreferences(UserPreferences preferences) {
            preferencesMap.put(preferences.getUserId(), preferences);
            return preferences;
        }
    }
}

