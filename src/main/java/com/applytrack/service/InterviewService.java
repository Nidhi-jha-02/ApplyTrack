package com.applytrack.service;

import com.applytrack.exception.ValidationException;
import com.applytrack.model.Application;
import com.applytrack.model.Interview;
import com.applytrack.model.enums.InterviewMode;
import com.applytrack.model.enums.InterviewOutcome;
import com.applytrack.repository.ApplicationRepository;
import com.applytrack.repository.InterviewRepository;
import com.applytrack.repository.impl.JdbcApplicationRepository;
import com.applytrack.repository.impl.JdbcInterviewRepository;
import com.applytrack.util.ValidationUtil;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service orchestrating Interview evaluation round lifecycle, business rules,
 * ownership security checks, and cross-module synchronization.
 *
 * Demonstrates:
 * - OOP: Layered Architecture & Separation of Concerns (Unit 3)
 * - Custom Exception Handling: ValidationException (Unit 3)
 * - Dependency Injection via constructor overloading (Unit 2 & 3)
 * - Strict Data Isolation: Validates both Application and Interview ownership for userId
 * - Input validation & URL format verification (Unit 1 & 2)
 */
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;

    public InterviewService() {
        this(new JdbcInterviewRepository(), new JdbcApplicationRepository());
    }

    public InterviewService(InterviewRepository interviewRepository, ApplicationRepository applicationRepository) {
        this.interviewRepository = Objects.requireNonNull(interviewRepository, "InterviewRepository must not be null");
        this.applicationRepository = Objects.requireNonNull(applicationRepository, "ApplicationRepository must not be null");
    }

    /**
     * Schedules a new interview round for an existing job application.
     * Verifies that the application belongs to the authenticated user before insertion.
     */
    public Interview scheduleInterview(int applicationId, int userId, String roundName,
                                      LocalDateTime interviewDate, InterviewMode mode,
                                      String location, String meetingLink, String interviewer,
                                      String feedback, InterviewOutcome outcome)
            throws ValidationException, SQLException {

        if (userId <= 0) {
            throw new ValidationException("User must be authenticated to schedule an interview.");
        }
        if (applicationId <= 0) {
            throw new ValidationException("A valid job application must be selected for this interview.");
        }

        // Verify application ownership
        Optional<Application> appOpt = applicationRepository.findById(applicationId, userId);
        if (appOpt.isEmpty()) {
            throw new ValidationException("Job application not found or does not belong to the authenticated user.");
        }
        Application application = appOpt.get();

        validateInterviewInput(roundName, interviewDate, meetingLink);

        Interview interview = new Interview(
                0,
                applicationId,
                roundName.trim(),
                interviewDate,
                mode != null ? mode : InterviewMode.ONLINE,
                location != null ? location.trim() : null,
                meetingLink != null && !meetingLink.isBlank() ? meetingLink.trim() : null,
                interviewer != null ? interviewer.trim() : null,
                feedback != null ? feedback.trim() : null,
                outcome != null ? outcome : InterviewOutcome.SCHEDULED,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        interview.setApplication(application);

        return interviewRepository.save(interview);
    }

    /**
     * Updates an existing interview round.
     * Verifies that the interview exists and belongs to an application owned by the user.
     */
    public Interview updateInterview(int interviewId, int userId, String roundName,
                                    LocalDateTime interviewDate, InterviewMode mode,
                                    String location, String meetingLink, String interviewer,
                                    String feedback, InterviewOutcome outcome)
            throws ValidationException, SQLException {

        if (userId <= 0) {
            throw new ValidationException("User must be authenticated to update an interview.");
        }
        if (interviewId <= 0) {
            throw new ValidationException("Invalid interview ID specified.");
        }

        // Verify interview ownership
        Optional<Interview> existingOpt = interviewRepository.findById(interviewId, userId);
        if (existingOpt.isEmpty()) {
            throw new ValidationException("Interview record #" + interviewId + " not found or does not belong to this user.");
        }

        validateInterviewInput(roundName, interviewDate, meetingLink);

        Interview existing = existingOpt.get();
        existing.setRoundName(roundName.trim());
        existing.setInterviewDate(interviewDate);
        existing.setInterviewMode(mode != null ? mode : InterviewMode.ONLINE);
        existing.setInterviewLocation(location != null ? location.trim() : null);
        existing.setMeetingLink(meetingLink != null && !meetingLink.isBlank() ? meetingLink.trim() : null);
        existing.setInterviewerName(interviewer != null ? interviewer.trim() : null);
        existing.setFeedback(feedback != null ? feedback.trim() : null);
        existing.setOutcome(outcome != null ? outcome : InterviewOutcome.SCHEDULED);

        boolean updated = interviewRepository.update(existing, userId);
        if (!updated) {
            throw new SQLException("Failed to update interview #" + interviewId + " in the database.");
        }

        return existing;
    }

    /**
     * Deletes an interview round record.
     * Verifies ownership before removing from database.
     */
    public boolean deleteInterview(int interviewId, int userId) throws ValidationException, SQLException {
        if (userId <= 0) {
            throw new ValidationException("User must be authenticated to delete an interview.");
        }
        if (interviewId <= 0) {
            throw new ValidationException("Invalid interview ID specified.");
        }

        Optional<Interview> existingOpt = interviewRepository.findById(interviewId, userId);
        if (existingOpt.isEmpty()) {
            throw new ValidationException("Interview record #" + interviewId + " not found or does not belong to this user.");
        }

        return interviewRepository.delete(interviewId, userId);
    }

    /**
     * Retrieves all interviews for the specified user.
     */
    public List<Interview> getInterviewsForUser(int userId) {
        if (userId <= 0) {
            return Collections.emptyList();
        }
        try {
            return interviewRepository.findByUserId(userId);
        } catch (SQLException e) {
            System.err.println("Error fetching interviews for user #" + userId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves all interview rounds for a specific job application.
     */
    public List<Interview> getInterviewsForApplication(int applicationId, int userId) {
        if (applicationId <= 0 || userId <= 0) {
            return Collections.emptyList();
        }
        try {
            return interviewRepository.findByApplicationId(applicationId, userId);
        } catch (SQLException e) {
            System.err.println("Error fetching interviews for app #" + applicationId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves upcoming scheduled interviews for the user.
     */
    public List<Interview> getUpcomingInterviews(int userId) {
        if (userId <= 0) {
            return Collections.emptyList();
        }
        try {
            return interviewRepository.findUpcoming(userId);
        } catch (SQLException e) {
            System.err.println("Error fetching upcoming interviews for user #" + userId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves completed or past interviews for the user.
     */
    public List<Interview> getCompletedInterviews(int userId) {
        if (userId <= 0) {
            return Collections.emptyList();
        }
        try {
            return interviewRepository.findCompleted(userId);
        } catch (SQLException e) {
            System.err.println("Error fetching completed interviews for user #" + userId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves a single interview by ID, ensuring user ownership.
     */
    public Optional<Interview> getInterviewById(int interviewId, int userId) {
        if (interviewId <= 0 || userId <= 0) {
            return Optional.empty();
        }
        try {
            return interviewRepository.findById(interviewId, userId);
        } catch (SQLException e) {
            System.err.println("Error fetching interview #" + interviewId + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Filters, searches and sorts interviews according to UI criteria.
     */
    public List<Interview> getFilteredInterviews(int userId, String searchKeyword, String outcomeFilter,
                                                String timelineFilter, String sortBy, boolean ascending) {
        if (userId <= 0) {
            return Collections.emptyList();
        }
        try {
            return interviewRepository.findWithFilters(userId, searchKeyword, outcomeFilter, timelineFilter, sortBy, ascending);
        } catch (SQLException e) {
            System.err.println("Error filtering interviews for user #" + userId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Searches interviews by keyword.
     */
    public List<Interview> searchInterviews(int userId, String keyword) {
        if (userId <= 0) {
            return Collections.emptyList();
        }
        try {
            return interviewRepository.searchByKeyword(userId, keyword);
        } catch (SQLException e) {
            System.err.println("Error searching interviews: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Returns total interview count for the user.
     */
    public int getInterviewCount(int userId) {
        if (userId <= 0) return 0;
        try {
            return interviewRepository.countInterviewsByUserId(userId);
        } catch (SQLException e) {
            return 0;
        }
    }

    /**
     * Returns upcoming interview count for the user.
     */
    public int getUpcomingInterviewCount(int userId) {
        if (userId <= 0) return 0;
        try {
            return interviewRepository.countUpcomingByUserId(userId);
        } catch (SQLException e) {
            return 0;
        }
    }

    /**
     * Validates required interview fields and URL formats.
     */
    private void validateInterviewInput(String roundName, LocalDateTime interviewDate, String meetingLink)
            throws ValidationException {
        if (!ValidationUtil.isNotEmpty(roundName)) {
            throw new ValidationException("Interview round name is required (e.g., 'Technical Assessment', 'HR Round').");
        }
        if (interviewDate == null) {
            throw new ValidationException("Interview date and time must be specified.");
        }
        if (meetingLink != null && !meetingLink.isBlank() && !ValidationUtil.isValidUrl(meetingLink.trim())) {
            throw new ValidationException("Meeting link must be a valid HTTP or HTTPS URL (e.g., https://meet.google.com/abc-defg-hij).");
        }
    }
}

