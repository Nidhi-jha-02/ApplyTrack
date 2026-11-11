package com.applytrack.model.base;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Abstract base class providing common persistence attributes for all domain models.
 *
 * Demonstrates:
 * - Abstract Classes and Abstract Methods (Unit 3)
 * - Protected Access Modifiers (Unit 3)
 * - Constructor Overloading and Chaining with 'this' (Unit 2 & 3)
 * - Method Overriding (equals, hashCode, toString) - Unit 3
 */
public abstract class BaseEntity {

    protected int id;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;

    /**
     * Default constructor initializing timestamps to current time (Unit 3)
     */
    public BaseEntity() {
        this(0, LocalDateTime.now(), LocalDateTime.now());
    }

    /**
     * Overloaded constructor for entities with existing IDs (Unit 2 & 3)
     */
    public BaseEntity(int id) {
        this(id, LocalDateTime.now(), LocalDateTime.now());
    }

    /**
     * Full parameterized constructor (Constructor Chaining - Unit 3)
     */
    public BaseEntity(int id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
        this.updatedAt = (updatedAt != null) ? updatedAt : LocalDateTime.now();
    }

    /**
     * Abstract method that every concrete entity must implement.
     * Demonstrates Polymorphism and Abstract Class Contracts (Unit 3).
     *
     * @return A concise, user-friendly display title for the entity.
     */
    public abstract String getDisplayTitle();

    // Getters and Setters (Encapsulation - Unit 3)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return id != 0 && id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, getClass());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

