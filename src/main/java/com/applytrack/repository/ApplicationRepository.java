package com.applytrack.repository;

import com.applytrack.model.Application;
import com.applytrack.model.enums.ApplicationStatus;
import com.applytrack.model.enums.JobType;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Repository contract for Job Application entity persistence and retrieval.
 *
 * Demonstrates:
 * - OOP: Interface Abstraction and Polymorphism (Unit 3)
 * - User-Specific Data Security: Every query strictly binds user_id = ?
 * - Collections Framework: List<Application> return types (Unit 3)
 * - Checked Exception handling: throws SQLException (Unit 3)
 */
public interface ApplicationRepository {

    /**
     * Persists a new application or updates an existing one for the user.
     * Uses generated keys on insert.
     */
    Application save(Application application) throws SQLException;

    /**
     * Finds an application by its primary key ID, strictly scoped to the owner's user ID.
     */
    Optional<Application> findById(int applicationId, int userId) throws SQLException;

    /**
     * Retrieves all applications submitted by the specified user, ordered by application_date DESC.
     */
    List<Application> findByUserId(int userId) throws SQLException;

    /**
     * Updates an existing application record belonging to the owner user.
     * Returns true if a row was updated, false otherwise.
     */
    boolean update(Application application) throws SQLException;

    /**
     * Deletes an application record strictly scoped to the user ID.
     * Cascades deletion to child interview records per foreign key constraints.
     * Returns true if a row was deleted, false otherwise.
     */
    boolean delete(int applicationId, int userId) throws SQLException;

    /**
     * Searches applications for a user by company name or job role (partial match).
     */
    List<Application> searchByKeyword(int userId, String keyword) throws SQLException;

    /**
     * Filters applications for a user by their status enum.
     */
    List<Application> filterByStatus(int userId, ApplicationStatus status) throws SQLException;

    /**
     * Advanced multi-criteria search, status filter, job type filter, and dynamic sorting.
     *
     * @param userId Authenticated user primary key
     * @param searchKeyword Optional search string matching company name or job role
     * @param statusFilter Optional status filter (null for all)
     * @param jobTypeFilter Optional job type filter (null for all)
     * @param sortBy Column to sort by ("application_date", "deadline", "company_name", "job_role")
     * @param ascending True for ASC, false for DESC
     * @return Filtered, sorted list of applications
     */
    List<Application> findWithFilters(int userId, String searchKeyword, ApplicationStatus statusFilter,
                                      JobType jobTypeFilter, String sortBy, boolean ascending) throws SQLException;
}

