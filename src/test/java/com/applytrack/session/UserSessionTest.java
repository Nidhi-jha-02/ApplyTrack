package com.applytrack.session;

import com.applytrack.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests verifying UserSession singleton and state management.
 *
 * Demonstrates:
 * - Singleton pattern testing - Unit 2/3
 * - State lifecycle testing - Unit 3
 */
public class UserSessionTest {

    private UserSession session;

    @BeforeEach
    public void setUp() {
        session = UserSession.getInstance();
        session.clearSession();
    }

    @Test
    @DisplayName("Verify initial unauthenticated session state")
    public void testInitialState() {
        assertFalse(session.isAuthenticated());
        assertNull(session.getCurrentUser());
        assertEquals(0, session.getUserId());
        assertEquals("Guest", session.getUserFullName());
        assertEquals("", session.getUserEmail());
    }

    @Test
    @DisplayName("Verify session binding with authenticated user")
    public void testSessionBinding() {
        User user = new User("Alex Mercer", "alex@applytrack.io", "hashedPass123");
        user.setId(99);

        session.setCurrentUser(user);

        assertTrue(session.isAuthenticated());
        assertNotNull(session.getCurrentUser());
        assertEquals(99, session.getUserId());
        assertEquals("Alex Mercer", session.getUserFullName());
        assertEquals("alex@applytrack.io", session.getUserEmail());
    }

    @Test
    @DisplayName("Verify session clearing on logout")
    public void testClearSession() {
        User user = new User("Test Candidate", "candidate@test.com", "hash");
        session.setCurrentUser(user);
        assertTrue(session.isAuthenticated());

        session.clearSession();
        assertFalse(session.isAuthenticated());
        assertNull(session.getCurrentUser());
        assertEquals(0, session.getUserId());
    }
}

