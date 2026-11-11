package com.applytrack.repository;

import com.applytrack.model.Interview;
import com.applytrack.model.enums.InterviewOutcome;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Interview evaluation rounds and pipeline persistence.
 *
 * Demonstrates:
 * - OOP: Interface Abstraction & Polymorphism (Unit 3)
 * - User-Specific Data Security: Every query strictly scopes to user ownership
 *   via JOIN applications a ON i.application_id = a.application_id WHERE a.user_id = ?
 * - Collections Framework: List<Interview> return types (Unit 3)
 * - Exception Handling: throws SQLException (Unit 3)
 */
public interface InterviewRepository {

    /**
     * Persists a new interview round record.
     * Generates and sets the auto-incremented primary key.
     */
    Interview save(Interview interview) throws SQLException;

    /**
     * Finds an interview round by its primary key, strictly verifying that
     * the parent application belongs to the specified userId.
     */
    Optional<Interview> findById(int interviewId, int userId) throws SQLException;

    /**
     * Retrieves all interview rounds belonging to applications of the specified user.
     * Ordered by interview_date DESC.
     */
    List<Interview> findByUserId(int userId) throws SQLException;

    /**
     * Retrieves all interview rounds for a specific job application,
     * ensuring application ownership matches the specified userId.
     * Ordered by interview_date ASC.
     */
    List<Interview> findByApplicationId(int applicationId, int userId) throws SQLException;

    /**
     * Updates an existing interview record, ensuring user ownership.
     * Returns true if a row was updated, false otherwise.
     */
    boolean update(Interview interview, int userId) throws SQLException;

    /**
     * Deletes an interview round record, ensuring user ownership.
     * Returns true if a row was deleted, false otherwise.
     */
    boolean delete(int interviewId, int userId) throws SQLException;

    /**
     * Searches interview rounds by keyword across round name, company name,
     * job role, or interviewer name.
     */
    List<Interview> searchByKeyword(int userId, String keyword) throws SQLException;

    /**
     * Filters interview rounds by their outcome enum (e.g. Scheduled, Passed, Failed).
     */
    List<Interview> filterByOutcome(int userId, InterviewOutcome outcome) throws SQLException;

    /**
     * Retrieves upcoming interviews for the user (interview_date >= NOW() and outcome = 'Scheduled'),
     * ordered by interview_date ASC (nearest first).
     */
    List<Interview> findUpcoming(int userId) throws SQLException;

    /**
     * Retrieves completed or past interviews for the user (interview_date < NOW() or outcome != 'Scheduled'),
     * ordered by interview_date DESC.
     */
    List<Interview> findCompleted(int userId) throws SQLException;

    /**
     * Advanced multi-parameter filter, search and sort.
     *
     * @param userId authenticated user ID
     * @param searchKeyword optional text search (company, role, round, interviewer)
     * @param outcomeFilter optional outcome name (or null / "All")
     * @param timelineFilter optional timeline ("Upcoming", "Completed", or null / "All")
     * @param sortBy column to sort by ("date", "company", "round", "outcome")
     * @param ascending true for ASC, false for DESC
     */
    List<Interview> findWithFilters(int userId, String searchKeyword, String outcomeFilter,
                                   String timelineFilter, String sortBy, boolean ascending) throws SQLException;

    /**
     * Counts total interviews across all applications owned by the user.
     */
    int countInterviewsByUserId(int userId) throws SQLException;

    /**
     * Counts upcoming scheduled interviews for the user.
     */
    int countUpcomingByUserId(int userId) throws SQLException;
}

