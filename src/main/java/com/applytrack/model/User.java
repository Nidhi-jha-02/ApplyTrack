package com.applytrack.model;

import com.applytrack.model.base.BaseEntity;
import com.applytrack.util.ValidationUtil;

import java.time.LocalDateTime;

/**
 * Represents an authenticated candidate or applicant user in ApplyTrack.
 *
 * Demonstrates:
 * - Inheritance: extends BaseEntity (Unit 3)
 * - 'super' keyword for constructor delegation (Unit 3)
 * - Encapsulation with strict input validation (Unit 3)
 * - Security: Never exposes passwordHash in toString() or logging (Unit 1/3)
 * - Polymorphic display title implementation (Unit 3)
 */
public class User extends BaseEntity {

    private String fullName;
    private String email;
    private String passwordHash;

    /**
     * Default constructor (Unit 3)
     */
    public User() {
        super();
    }

    /**
     * Overloaded convenience constructor for creating new users before database insertion.
     */
    public User(String fullName, String email, String passwordHash) {
        super();
        setFullName(fullName);
        setEmail(email);
        setPasswordHash(passwordHash);
    }

    /**
     * Full parameterized constructor matching database records.
     */
    public User(int userId, String fullName, String email, String passwordHash,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(userId, createdAt, updatedAt);
        setFullName(fullName);
        setEmail(email);
        setPasswordHash(passwordHash);
    }

    // Convenience alias for entity ID
    public int getUserId() {
        return getId();
    }

    public void setUserId(int userId) {
        setId(userId);
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        if (!ValidationUtil.isNotEmpty(fullName)) {
            throw new IllegalArgumentException("Full name cannot be null or empty.");
        }
        this.fullName = fullName.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (!ValidationUtil.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email address format: " + email);
        }
        this.email = email.trim().toLowerCase();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        if (!ValidationUtil.isNotEmpty(passwordHash)) {
            throw new IllegalArgumentException("Password hash cannot be null or empty.");
        }
        this.passwordHash = passwordHash;
    }

    @Override
    public String getDisplayTitle() {
        return fullName + " (" + email + ")";
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + getId() +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", passwordHash='[PROTECTED]'" +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

