package com.applytrack.service;

import com.applytrack.exception.ValidationException;
import com.applytrack.model.Application;
import com.applytrack.model.Company;
import com.applytrack.model.enums.ApplicationStatus;
import com.applytrack.model.enums.JobType;
import com.applytrack.repository.ApplicationRepository;
import com.applytrack.repository.CompanyRepository;
import com.applytrack.repository.impl.JdbcApplicationRepository;
import com.applytrack.repository.impl.JdbcCompanyRepository;
import com.applytrack.util.ValidationUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service orchestrating Job Application business logic, validation, ownership checks,
 * and company relationship resolution.
 *
 * Demonstrates:
 * - OOP: Layered Architecture & Separation of Concerns (Unit 3)
 * - Custom Exception Handling: ValidationException (Unit 3)
 * - Dependency Injection via constructor overloading (Unit 2 & 3)
 * - Ownership & Data Security Verification: Enforces userId matching (Unit 3)
 * - Input validation & business rule enforcement (Unit 1 & 2)
 */
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;

    public ApplicationService() {
        this(new JdbcApplicationRepository(), new JdbcCompanyRepository());
    }

    public ApplicationService(ApplicationRepository applicationRepository, CompanyRepository companyRepository) {
        this.applicationRepository = Objects.requireNonNull(applicationRepository, "ApplicationRepository must not be null");
        this.companyRepository = Objects.requireNonNull(companyRepository, "CompanyRepository must not be null");
    }

    /**
     * Creates and persists a new job application for the authenticated user.
     */
    public Application createApplication(int userId, String companyName, String jobRole, JobType jobType,
                                         LocalDate applicationDate, LocalDate deadline, BigDecimal expectedSalary,
                                         String jobUrl, String jobDescription, ApplicationStatus status,
                                         String notes) throws ValidationException, SQLException {
        return createApplication(userId, companyName, jobRole, jobType, applicationDate, deadline,
                expectedSalary, jobUrl, jobDescription, status, notes, null);
    }

    /**
     * Creates and persists a new job application with optional recruiter association.
     */
    public Application createApplication(int userId, String companyName, String jobRole, JobType jobType,
                                         LocalDate applicationDate, LocalDate deadline, BigDecimal expectedSalary,
                                         String jobUrl, String jobDescription, ApplicationStatus status,
                                         String notes, Integer recruiterId) throws ValidationException, SQLException {
        if (userId <= 0) {
            throw new ValidationException("User must be authenticated to create an application.");
        }

        validateApplicationInput(companyName, jobRole, applicationDate, deadline, expectedSalary, jobUrl);

        // Resolve or create company record
        Company company = companyRepository.findOrCreateByName(companyName.trim());

        Application application = new Application(
                userId,
                company.getId(),
                recruiterId,
                jobRole.trim(),
                jobType != null ? jobType : JobType.FULL_TIME,
                applicationDate,
                deadline,
                expectedSalary,
                status != null ? status : ApplicationStatus.APPLIED,
                notes != null ? notes.trim() : null
        );
        application.setJobUrl(jobUrl != null && !jobUrl.isBlank() ? jobUrl.trim() : null);
        application.setJobDescription(jobDescription != null && !jobDescription.isBlank() ? jobDescription.trim() : null);
        application.setCompany(company);

        return applicationRepository.save(application);
    }

    /**
     * Updates an existing job application with strict ownership verification.
     */
    public Application updateApplication(int applicationId, int userId, String companyName, String jobRole,
                                         JobType jobType, LocalDate applicationDate, LocalDate deadline,
                                         BigDecimal expectedSalary, String jobUrl, String jobDescription,
                                         ApplicationStatus status, String notes) throws ValidationException, SQLException {
        return updateApplication(applicationId, userId, companyName, jobRole, jobType, applicationDate,
                deadline, expectedSalary, jobUrl, jobDescription, status, notes, null);
    }

    /**
     * Updates an existing job application with optional recruiter association.
     */
    public Application updateApplication(int applicationId, int userId, String companyName, String jobRole,
                                         JobType jobType, LocalDate applicationDate, LocalDate deadline,
                                         BigDecimal expectedSalary, String jobUrl, String jobDescription,
                                         ApplicationStatus status, String notes, Integer recruiterId)
            throws ValidationException, SQLException {
        if (applicationId <= 0) {
            throw new ValidationException("Invalid application ID.");
        }
        if (userId <= 0) {
            throw new ValidationException("User must be authenticated to update an application.");
        }

        // Ownership verification: ensure application exists and belongs to this user
        Optional<Application> existingOpt = applicationRepository.findById(applicationId, userId);
        if (existingOpt.isEmpty()) {
            throw new ValidationException("Application not found or you do not have permission to modify it.");
        }

        validateApplicationInput(companyName, jobRole, applicationDate, deadline, expectedSalary, jobUrl);

        Company company = companyRepository.findOrCreateByName(companyName.trim());
        Application existing = existingOpt.get();

        existing.setCompanyId(company.getId());
        existing.setCompany(company);
        existing.setRecruiterId(recruiterId);
        existing.setJobRole(jobRole.trim());
        existing.setJobType(jobType != null ? jobType : JobType.FULL_TIME);
        existing.setApplicationDate(applicationDate);
        existing.setDeadline(deadline);
        existing.setExpectedSalary(expectedSalary);
        existing.setJobUrl(jobUrl != null && !jobUrl.isBlank() ? jobUrl.trim() : null);
        existing.setJobDescription(jobDescription != null && !jobDescription.isBlank() ? jobDescription.trim() : null);
        existing.setStatus(status != null ? status : ApplicationStatus.APPLIED);
        existing.setNotes(notes != null ? notes.trim() : null);

        boolean updated = applicationRepository.update(existing);
        if (!updated) {
            throw new SQLException("Failed to update application #" + applicationId + " in the database.");
        }

        return existing;
    }

    /**
     * Deletes an existing application with ownership verification.
     */
    public boolean deleteApplication(int applicationId, int userId) throws ValidationException, SQLException {
        if (applicationId <= 0) {
            throw new ValidationException("Invalid application ID.");
        }
        if (userId <= 0) {
            throw new ValidationException("User must be authenticated to delete an application.");
        }

        // Verify ownership first
        Optional<Application> existing = applicationRepository.findById(applicationId, userId);
        if (existing.isEmpty()) {
            throw new ValidationException("Application not found or you do not have permission to delete it.");
        }

        return applicationRepository.delete(applicationId, userId);
    }

    /**
     * Retrieves an application by ID scoped to the authenticated user.
     */
    public Optional<Application> getApplicationById(int applicationId, int userId) throws SQLException {
        if (applicationId <= 0 || userId <= 0) {
            return Optional.empty();
        }
        return applicationRepository.findById(applicationId, userId);
    }

    /**
     * Retrieves all applications submitted by the user.
     */
    public List<Application> getApplicationsForUser(int userId) throws SQLException {
        if (userId <= 0) {
            return Collections.emptyList();
        }
        return applicationRepository.findByUserId(userId);
    }

    /**
     * Searches, filters, and sorts applications for the user.
     */
    public List<Application> getFilteredApplications(int userId, String searchKeyword, ApplicationStatus statusFilter,
                                                    JobType jobTypeFilter, String sortBy, boolean ascending) throws SQLException {
        if (userId <= 0) {
            return Collections.emptyList();
        }
        return applicationRepository.findWithFilters(userId, searchKeyword, statusFilter, jobTypeFilter, sortBy, ascending);
    }

    /**
     * Retrieves all company profiles for auto-completion and suggestions.
     */
    public List<Company> getAllCompanies() throws SQLException {
        return companyRepository.findAll();
    }

    /**
     * Comprehensive validation of application form fields.
     */
    public void validateApplicationInput(String companyName, String jobRole, LocalDate applicationDate,
                                         LocalDate deadline, BigDecimal expectedSalary, String jobUrl)
            throws ValidationException {

        if (!ValidationUtil.isNotEmpty(companyName)) {
            throw new ValidationException("Company name is required.");
        }
        if (!ValidationUtil.isNotEmpty(jobRole)) {
            throw new ValidationException("Job role is required.");
        }
        if (applicationDate == null) {
            throw new ValidationException("Application date is required.");
        }
        if (deadline != null && deadline.isBefore(applicationDate)) {
            throw new ValidationException("Application deadline (" + deadline + ") cannot be earlier than application date (" + applicationDate + ").");
        }
        if (expectedSalary != null && expectedSalary.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Expected salary or stipend cannot be negative.");
        }
        if (jobUrl != null && !jobUrl.isBlank() && !ValidationUtil.isValidUrl(jobUrl)) {
            throw new ValidationException("Job URL is invalid. Please provide a valid HTTP/HTTPS web address.");
        }
    }
}

