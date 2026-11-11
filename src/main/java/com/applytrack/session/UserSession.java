package com.applytrack.session;

import com.applytrack.model.User;

/**
 * Manages the client-side authentication session state for ApplyTrack.
 *
 * Demonstrates:
 * - Singleton Design Pattern (private constructor, static instance) - Unit 2 & 3
 * - Controlled Encapsulation & Session state tracking - Unit 3
 * - Thread-safe synchronization on session state mutations - Unit 3
 * - Security: Does not cache raw passwords, references only authenticated User entity
 */
public class UserSession {

    private static UserSession instance;
    private User currentUser;

    // Private constructor prevents direct instantiation
    private UserSession() {}

    /**
     * Obtains the global singleton session manager.
     */
    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    /**
     * Initializes the session with an authenticated user.
     *
     * @param user The authenticated candidate user
     */
    public synchronized void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * Returns the currently authenticated user, or null if no user is signed in.
     */
    public synchronized User getCurrentUser() {
        return currentUser;
    }

    /**
     * Retrieves the active user's ID, or 0 if unauthenticated.
     */
    public synchronized int getUserId() {
        return (currentUser != null) ? currentUser.getId() : 0;
    }

    /**
     * Retrieves the active user's full name, or "Guest".
     */
    public synchronized String getUserFullName() {
        return (currentUser != null) ? currentUser.getFullName() : "Guest";
    }

    /**
     * Retrieves the active user's email, or "".
     */
    public synchronized String getUserEmail() {
        return (currentUser != null) ? currentUser.getEmail() : "";
    }

    /**
     * Checks if a user is currently authenticated in the active session.
     */
    public synchronized boolean isAuthenticated() {
        return currentUser != null;
    }

    /**
     * Alias for isAuthenticated() for intuitive status checking.
     */
    public synchronized boolean isLoggedIn() {
        return isAuthenticated();
    }

    /**
     * Clears all session information upon user logout.
     */
    public synchronized void clearSession() {
        this.currentUser = null;
    }
}

