package com.applytrack.service;

import com.applytrack.exception.AuthenticationException;
import com.applytrack.exception.ValidationException;
import com.applytrack.model.User;
import com.applytrack.repository.UserRepository;
import com.applytrack.repository.impl.JdbcUserRepository;
import com.applytrack.session.UserSession;
import com.applytrack.util.ValidationUtil;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Service orchestrating user registration, BCrypt password hashing, and login authentication.
 *
 * Demonstrates:
 * - Service Layer Architecture (decoupling business logic from UI and DB) - Unit 3
 * - BCrypt secure password hashing and salting - Unit 3
 * - Strict input validation with custom exceptions - Unit 3
 * - Safe security practices: Generic error messages preventing user enumeration
 * - Dependency Injection via constructor overloading (Unit 2 & 3)
 */
public class AuthService {

    public static final int MIN_PASSWORD_LENGTH = 8;
    private final UserRepository userRepository;

    /**
     * Default constructor using standard JDBC repository.
     */
    public AuthService() {
        this(new JdbcUserRepository());
    }

    /**
     * Parameterized constructor allowing repository injection for unit testing.
     */
    public AuthService(UserRepository userRepository) {
        if (userRepository == null) {
            throw new IllegalArgumentException("UserRepository cannot be null.");
        }
        this.userRepository = userRepository;
    }

    /**
     * Registers a new applicant account in ApplyTrack.
     *
     * @param fullName Full name of the applicant
     * @param email Valid unique email address
     * @param password Password meeting minimum length criteria
     * @param confirmPassword Password confirmation matching the password
     * @return The persisted User entity with its generated ID
     * @throws ValidationException If validation rules fail or email already exists
     * @throws SQLException If database insertion fails
     */
    public User register(String fullName, String email, String password, String confirmPassword)
            throws ValidationException, SQLException {

        // 1. Validate required fields
        if (!ValidationUtil.isNotEmpty(fullName)) {
            throw new ValidationException("Full name is required.");
        }
        if (!ValidationUtil.isNotEmpty(email)) {
            throw new ValidationException("Email address is required.");
        }
        if (!ValidationUtil.isNotEmpty(password)) {
            throw new ValidationException("Password is required.");
        }

        // 2. Validate email format
        String cleanEmail = email.trim().toLowerCase();
        if (!ValidationUtil.isValidEmail(cleanEmail)) {
            throw new ValidationException("Invalid email format. Example: name@university.edu");
        }

        // 3. Validate password length
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.");
        }

        // 4. Validate password confirmation
        if (!password.equals(confirmPassword)) {
            throw new ValidationException("Passwords do not match. Please re-enter.");
        }

        // 5. Check duplicate email existence
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new ValidationException("An account with email '" + cleanEmail + "' already exists. Please log in.");
        }

        // 6. Securely hash password with BCrypt salt
        String passwordHash = hashPassword(password);

        // 7. Persist user entity
        User newUser = new User(fullName.trim(), cleanEmail, passwordHash);
        return userRepository.save(newUser);
    }

    /**
     * Authenticates an applicant using their email and password.
     *
     * @param email The applicant's email
     * @param password The plain-text password entered
     * @return Authenticated User object
     * @throws ValidationException If inputs are blank
     * @throws AuthenticationException If credentials are invalid
     * @throws SQLException On database failure
     */
    public User login(String email, String password) throws ValidationException, AuthenticationException, SQLException {
        // 1. Input checks
        if (!ValidationUtil.isNotEmpty(email) || !ValidationUtil.isNotEmpty(password)) {
            throw new ValidationException("Email and password are required.");
        }

        String cleanEmail = email.trim().toLowerCase();

        // 2. Lookup user by email
        Optional<User> userOpt = userRepository.findByEmail(cleanEmail);
        if (userOpt.isEmpty()) {
            // Generic error message prevents username enumeration attack
            throw new AuthenticationException("Invalid email or password.");
        }

        User user = userOpt.get();

        // 3. Verify BCrypt password hash
        if (!verifyPassword(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password.");
        }

        // 4. Bind to active user session
        UserSession.getInstance().setCurrentUser(user);
        return user;
    }

    /**
     * Clears the current user's session.
     */
    public void logout() {
        UserSession.getInstance().clearSession();
    }

    /**
     * Hashes a plain-text password using BCrypt with salt factor 10.
     */
    public String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty for hashing.");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));
    }

    /**
     * Verifies a plain-text password against a stored BCrypt hash.
     */
    public boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, storedHash);
        } catch (IllegalArgumentException e) {
            // Malformed hash
            return false;
        }
    }
}

