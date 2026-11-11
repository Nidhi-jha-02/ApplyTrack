package com.applytrack.repository;

import com.applytrack.model.Application;
import com.applytrack.model.Interview;
import com.applytrack.model.Recruiter;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Recruiter directory persistence, relationship mapping,
 * and user-specific security queries.
 *
 * Demonstrates:
 * - OOP: Interface Abstraction & Polymorphism (Unit 3)
 * - User-Specific Data Security: Scoped to user's accessible company/application pipeline
 * - Collections Framework: List<Recruiter>, List<Application>, List<Interview> (Unit 3)
 * - Checked Exception Handling: throws SQLException (Unit 3)
 */
public interface RecruiterRepository {

    /**
     * Persists a new recruiter record in the database.
     * Generates and sets the auto-incremented primary key.
     */
    Recruiter save(Recruiter recruiter) throws SQLException;

    /**
     * Finds a recruiter by primary key, verifying user accessibility.
     */
    Optional<Recruiter> findById(int recruiterId, int userId) throws SQLException;

    /**
     * Retrieves all recruiters accessible to the specified user
     * (recruiters linked to the user's applications or companies in the user's pipeline).
     * Ordered alphabetically by recruiter_name ASC.
     */
    List<Recruiter> findByUserId(int userId) throws SQLException;

    /**
     * Retrieves all recruiters belonging to a specific company that is accessible to the user.
     */
    List<Recruiter> findByCompanyId(int companyId, int userId) throws SQLException;

    /**
     * Updates an existing recruiter record, verifying user accessibility.
     * Returns true if updated, false otherwise.
     */
    boolean update(Recruiter recruiter, int userId) throws SQLException;

    /**
     * Deletes a recruiter record, verifying user accessibility.
     * Foreign key constraint will SET NULL on any linked applications.
     * Returns true if deleted, false otherwise.
     */
    boolean delete(int recruiterId, int userId) throws SQLException;

    /**
     * Searches recruiters by keyword across recruiter name, email, phone, notes, and company name.
     */
    List<Recruiter> searchByKeyword(int userId, String keyword) throws SQLException;

    /**
     * Advanced filter and sort matching UI controls.
     *
     * @param userId authenticated user ID
     * @param searchKeyword optional partial-text search
     * @param companyId optional company filter (or null for all)
     * @param sortBy column to sort by ("name", "company", "email", "apps")
     * @param ascending true for ASC, false for DESC
     */
    List<Recruiter> findWithFilters(int userId, String searchKeyword, Integer companyId,
                                   String sortBy, boolean ascending) throws SQLException;

    /**
     * Counts the total accessible recruiters for a user.
     */
    int countRecruitersByUserId(int userId) throws SQLException;

    /**
     * Counts how many applications belonging to the user are linked to this recruiter.
     */
    int countAssociatedApplications(int recruiterId, int userId) throws SQLException;

    /**
     * Counts how many interview rounds belonging to the user are linked to this recruiter's applications.
     */
    int countAssociatedInterviews(int recruiterId, int userId) throws SQLException;

    /**
     * Retrieves all job applications belonging to the user that are linked to this recruiter.
     */
    List<Application> findAssociatedApplications(int recruiterId, int userId) throws SQLException;

    /**
     * Retrieves all interview rounds belonging to the user that are associated with this recruiter's applications.
     */
    List<Interview> findAssociatedInterviews(int recruiterId, int userId) throws SQLException;

    /**
     * Checks if a company is accessible to the user (i.e. the user has an application for this company).
     */
    boolean isCompanyAccessibleToUser(int companyId, int userId) throws SQLException;
}

