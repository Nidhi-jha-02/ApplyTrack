package com.applytrack.service;

import com.applytrack.exception.AuthenticationException;
import com.applytrack.exception.ValidationException;
import com.applytrack.model.User;
import com.applytrack.repository.UserRepository;
import com.applytrack.session.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for AuthService business logic and BCrypt security.
 * Uses an in-memory repository mock to isolate business logic testing from network/database failures.
 *
 * Demonstrates:
 * - Unit 3: Exception Testing (assertThrows)
 * - Security: BCrypt salt & verification
 * - Polymorphism: Mocking UserRepository through its interface
 */
public class AuthServiceTest {

    private AuthService authService;
    private MockUserRepository mockRepo;

    @BeforeEach
    public void setUp() {
        mockRepo = new MockUserRepository();
        authService = new AuthService(mockRepo);
        UserSession.getInstance().clearSession();
    }

    @Test
    @DisplayName("Verify successful registration with BCrypt password hashing")
    public void testSuccessfulRegistration() throws ValidationException, SQLException {
        User registered = authService.register(
                "Jordan Lee",
                "jordan.lee@example.com",
                "securePassword123",
                "securePassword123"
        );

        assertNotNull(registered);
        assertEquals("Jordan Lee", registered.getFullName());
        assertEquals("jordan.lee@example.com", registered.getEmail());
        assertTrue(registered.getId() > 0);

        // Verify password is NOT stored in plain text
        assertFalse(registered.getPasswordHash().contains("securePassword123"));

        // Verify BCrypt hash can verify the original password
        assertTrue(BCrypt.checkpw("securePassword123", registered.getPasswordHash()));
    }

    @Test
    @DisplayName("Verify password mismatch throws ValidationException")
    public void testPasswordMismatch() {
        ValidationException ex = assertThrows(ValidationException.class, () -> authService.register(
                "Jordan Lee",
                "jordan@example.com",
                "Password123",
                "DifferentPassword123"
        ));
        assertTrue(ex.getMessage().contains("do not match"));
    }

    @Test
    @DisplayName("Verify short password (<8 chars) throws ValidationException")
    public void testShortPassword() {
        ValidationException ex = assertThrows(ValidationException.class, () -> authService.register(
                "Jordan Lee",
                "jordan@example.com",
                "short",
                "short"
        ));
        assertTrue(ex.getMessage().contains("at least 8 characters"));
    }

    @Test
    @DisplayName("Verify invalid email throws ValidationException")
    public void testInvalidEmail() {
        assertThrows(ValidationException.class, () -> authService.register(
                "Jordan Lee",
                "invalid-email",
                "securePassword123",
                "securePassword123"
        ));
    }

    @Test
    @DisplayName("Verify empty inputs throw ValidationException")
    public void testEmptyInputs() {
        assertThrows(ValidationException.class, () -> authService.register("", "jordan@example.com", "pass1234", "pass1234"));
        assertThrows(ValidationException.class, () -> authService.register("Jordan", "", "pass1234", "pass1234"));
        assertThrows(ValidationException.class, () -> authService.register("Jordan", "jordan@example.com", "", ""));
    }

    @Test
    @DisplayName("Verify duplicate email registration is rejected")
    public void testDuplicateEmail() throws ValidationException, SQLException {
        authService.register("Existing User", "duplicate@example.com", "password123", "password123");

        ValidationException ex = assertThrows(ValidationException.class, () -> authService.register(
                "Second User",
                "duplicate@example.com",
                "password123",
                "password123"
        ));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("Verify successful login and session binding")
    public void testSuccessfulLogin() throws ValidationException, AuthenticationException, SQLException {
        authService.register("Sarah Conner", "sarah@example.com", "mySecretPass2026", "mySecretPass2026");

        User loggedIn = authService.login("sarah@example.com", "mySecretPass2026");
        assertNotNull(loggedIn);
        assertEquals("Sarah Conner", loggedIn.getFullName());

        // Verify UserSession holds active user
        assertTrue(UserSession.getInstance().isAuthenticated());
        assertEquals(loggedIn.getId(), UserSession.getInstance().getUserId());
    }

    @Test
    @DisplayName("Verify incorrect password throws AuthenticationException with generic message")
    public void testIncorrectPassword() throws ValidationException, SQLException {
        authService.register("Sarah Conner", "sarah@example.com", "correctPassword123", "correctPassword123");

        AuthenticationException ex = assertThrows(AuthenticationException.class, () ->
                authService.login("sarah@example.com", "wrongPassword999")
        );
        assertEquals("Invalid email or password.", ex.getMessage());
    }

    @Test
    @DisplayName("Verify unknown email throws AuthenticationException with same generic message")
    public void testUnknownEmail() {
        AuthenticationException ex = assertThrows(AuthenticationException.class, () ->
                authService.login("ghost@example.com", "somePassword123")
        );
        assertEquals("Invalid email or password.", ex.getMessage());
    }

    @Test
    @DisplayName("Verify logout clears session")
    public void testLogout() throws ValidationException, AuthenticationException, SQLException {
        authService.register("Test User", "test@example.com", "password123", "password123");
        authService.login("test@example.com", "password123");
        assertTrue(UserSession.getInstance().isAuthenticated());

        authService.logout();
        assertFalse(UserSession.getInstance().isAuthenticated());
        assertNull(UserSession.getInstance().getCurrentUser());
    }

    /**
     * In-memory test mock for UserRepository.
     */
    private static class MockUserRepository implements UserRepository {
        private final Map<Integer, User> userMap = new HashMap<>();
        private int idSequence = 1;

        @Override
        public User save(User user) {
            int id = idSequence++;
            user.setId(id);
            userMap.put(id, user);
            return user;
        }

        @Override
        public Optional<User> findById(Integer id) {
            return Optional.ofNullable(userMap.get(id));
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return userMap.values().stream()
                    .filter(u -> u.getEmail().equalsIgnoreCase(email))
                    .findFirst();
        }

        @Override
        public boolean existsByEmail(String email) {
            return userMap.values().stream()
                    .anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(userMap.values());
        }

        @Override
        public boolean update(User entity) {
            if (userMap.containsKey(entity.getId())) {
                userMap.put(entity.getId(), entity);
                return true;
            }
            return false;
        }

        @Override
        public boolean deleteById(Integer id) {
            return userMap.remove(id) != null;
        }
    }
}

