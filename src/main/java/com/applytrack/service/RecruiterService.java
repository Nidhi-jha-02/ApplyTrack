package com.applytrack.service;

import com.applytrack.exception.ValidationException;
import com.applytrack.model.Application;
import com.applytrack.model.Company;
import com.applytrack.model.Interview;
import com.applytrack.model.Recruiter;
import com.applytrack.repository.ApplicationRepository;
import com.applytrack.repository.CompanyRepository;
import com.applytrack.repository.RecruiterRepository;
import com.applytrack.repository.impl.JdbcApplicationRepository;
import com.applytrack.repository.impl.JdbcCompanyRepository;
import com.applytrack.repository.impl.JdbcRecruiterRepository;
import com.applytrack.util.ValidationUtil;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service orchestrating Recruiter lifecycle, business rules, company associations,
 * and user-specific security validations.
 *
 * Demonstrates:
 * - OOP: Layered Architecture & Separation of Concerns (Unit 3)
 * - Custom Exception Handling: ValidationException (Unit 3)
 * - Dependency Injection via constructor overloading (Unit 2 & 3)
 * - Indirect Ownership Security: Enforces company/application pipeline verification
 * - Defensive validation for email, phone, and URLs (Unit 1 & 2)
 */
public class RecruiterService {

    private final RecruiterRepository recruiterRepository;
    private final CompanyRepository companyRepository;
    private final ApplicationRepository applicationRepository;

    public RecruiterService() {
        this(new JdbcRecruiterRepository(), new JdbcCompanyRepository(), new JdbcApplicationRepository());
    }

    public RecruiterService(RecruiterRepository recruiterRepository, CompanyRepository companyRepository, ApplicationRepository applicationRepository) {
        this.recruiterRepository = Objects.requireNonNull(recruiterRepository, "RecruiterRepository must not be null");
        this.companyRepository = Objects.requireNonNull(companyRepository, "CompanyRepository must not be null");
        this.applicationRepository = Objects.requireNonNull(applicationRepository, "ApplicationRepository must not be null");
    }

    /**
     * Creates and persists a new recruiter contact.
     * Verifies that any associated company is accessible in the user's application pipeline.
     */
    public Recruiter createRecruiter(int userId, String recruiterName, Integer companyId,
                                    String email, String phone, String linkedinUrl, String notes)
            throws ValidationException, SQLException {

        if (userId <= 0) {
            throw new ValidationException("User must be authenticated to add a recruiter.");
        }

        validateRecruiterInput(recruiterName, email, phone, linkedinUrl);

        Company company = null;
        if (companyId != null && companyId > 0) {
            // Verify company exists
            Optional<Company> compOpt = companyRepository.findById(companyId);
            if (compOpt.isEmpty()) {
                throw new ValidationException("Selected company does not exist.");
            }
            // Verify company association: user must have applications with this company
            boolean accessible = recruiterRepository.isCompanyAccessibleToUser(companyId, userId);
            if (!accessible) {
                throw new ValidationException("You can only associate recruiters with companies in your job application pipeline.");
            }
            company = compOpt.get();
        }

        Recruiter recruiter = new Recruiter(
                0,
                companyId,
                recruiterName.trim(),
                email != null && !email.isBlank() ? email.trim().toLowerCase() : null,
                phone != null && !phone.isBlank() ? phone.trim() : null,
                linkedinUrl != null && !linkedinUrl.isBlank() ? linkedinUrl.trim() : null,
                notes != null && !notes.isBlank() ? notes.trim() : null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        recruiter.setCompany(company);

        return recruiterRepository.save(recruiter);
    }

    /**
     * Updates an existing recruiter's details.
     * Verifies user permission/ownership before saving changes.
     */
    public Recruiter updateRecruiter(int recruiterId, int userId, String recruiterName, Integer companyId,
                                    String email, String phone, String linkedinUrl, String notes)
            throws ValidationException, SQLException {

        if (userId <= 0) {
            throw new ValidationException("User must be authenticated to update a recruiter.");
        }
        if (recruiterId <= 0) {
            throw new ValidationException("Invalid recruiter ID specified.");
        }

        // Verify recruiter ownership/accessibility
        Optional<Recruiter> existingOpt = recruiterRepository.findById(recruiterId, userId);
        if (existingOpt.isEmpty()) {
            throw new ValidationException("Recruiter #" + recruiterId + " not found or you do not have permission to modify this recruiter.");
        }

        validateRecruiterInput(recruiterName, email, phone, linkedinUrl);

        Company company = null;
        if (companyId != null && companyId > 0) {
            Optional<Company> compOpt = companyRepository.findById(companyId);
            if (compOpt.isEmpty()) {
                throw new ValidationException("Selected company does not exist.");
            }
            boolean accessible = recruiterRepository.isCompanyAccessibleToUser(companyId, userId);
            if (!accessible) {
                throw new ValidationException("You can only associate recruiters with companies in your job application pipeline.");
            }
            company = compOpt.get();
        }

        Recruiter existing = existingOpt.get();
        existing.setRecruiterName(recruiterName.trim());
        existing.setCompanyId(companyId);
        existing.setCompany(company);
        existing.setEmail(email != null && !email.isBlank() ? email.trim().toLowerCase() : null);
        existing.setPhone(phone != null && !phone.isBlank() ? phone.trim() : null);
        existing.setLinkedinUrl(linkedinUrl != null && !linkedinUrl.isBlank() ? linkedinUrl.trim() : null);
        existing.setNotes(notes != null && !notes.isBlank() ? notes.trim() : null);

        boolean updated = recruiterRepository.update(existing, userId);
        if (!updated) {
            throw new SQLException("Failed to update recruiter #" + recruiterId + " in the database.");
        }

        return existing;
    }

    /**
     * Deletes a recruiter record with ownership verification.
     * Database foreign key constraints automatically set recruiter_id = NULL on any linked applications.
     */
    public boolean deleteRecruiter(int recruiterId, int userId) throws ValidationException, SQLException {
        if (userId <= 0) {
            throw new ValidationException("User must be authenticated to delete a recruiter.");
        }
        if (recruiterId <= 0) {
            throw new ValidationException("Invalid recruiter ID specified.");
        }

        Optional<Recruiter> existingOpt = recruiterRepository.findById(recruiterId, userId);
        if (existingOpt.isEmpty()) {
            throw new ValidationException("Recruiter #" + recruiterId + " not found or you do not have permission to delete this recruiter.");
        }

        return recruiterRepository.delete(recruiterId, userId);
    }

    /**
     * Retrieves all recruiters accessible to the user.
     */
    public List<Recruiter> getRecruitersForUser(int userId) {
        if (userId <= 0) {
            return Collections.emptyList();
        }
        try {
            return recruiterRepository.findByUserId(userId);
        } catch (SQLException e) {
            System.err.println("Error fetching recruiters for user #" + userId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves a single recruiter by ID, verifying user accessibility.
     */
    public Optional<Recruiter> getRecruiterById(int recruiterId, int userId) {
        if (recruiterId <= 0 || userId <= 0) {
            return Optional.empty();
        }
        try {
            return recruiterRepository.findById(recruiterId, userId);
        } catch (SQLException e) {
            System.err.println("Error fetching recruiter #" + recruiterId + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Retrieves recruiters filtered by search keyword, company, and sort order.
     */
    public List<Recruiter> getFilteredRecruiters(int userId, String searchKeyword, Integer companyId,
                                                String sortBy, boolean ascending) {
        if (userId <= 0) {
            return Collections.emptyList();
        }
        try {
            return recruiterRepository.findWithFilters(userId, searchKeyword, companyId, sortBy, ascending);
        } catch (SQLException e) {
            System.err.println("Error filtering recruiters: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Searches recruiters by keyword across name, email, phone, company, and notes.
     */
    public List<Recruiter> searchRecruiters(int userId, String keyword) {
        if (userId <= 0) {
            return Collections.emptyList();
        }
        try {
            return recruiterRepository.searchByKeyword(userId, keyword);
        } catch (SQLException e) {
            System.err.println("Error searching recruiters: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves all recruiters for a specific company accessible to the user.
     */
    public List<Recruiter> getRecruitersByCompany(int companyId, int userId) {
        if (companyId <= 0 || userId <= 0) {
            return Collections.emptyList();
        }
        try {
            return recruiterRepository.findByCompanyId(companyId, userId);
        } catch (SQLException e) {
            System.err.println("Error fetching recruiters by company #" + companyId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves applications linked to this recruiter for details inspection.
     */
    public List<Application> getAssociatedApplications(int recruiterId, int userId) {
        if (recruiterId <= 0 || userId <= 0) {
            return Collections.emptyList();
        }
        try {
            return recruiterRepository.findAssociatedApplications(recruiterId, userId);
        } catch (SQLException e) {
            System.err.println("Error fetching associated applications: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves interview rounds linked to this recruiter's applications for details inspection.
     */
    public List<Interview> getAssociatedInterviews(int recruiterId, int userId) {
        if (recruiterId <= 0 || userId <= 0) {
            return Collections.emptyList();
        }
        try {
            return recruiterRepository.findAssociatedInterviews(recruiterId, userId);
        } catch (SQLException e) {
            System.err.println("Error fetching associated interviews: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Returns total count of accessible recruiters for the user.
     */
    public int getRecruiterCount(int userId) {
        if (userId <= 0) return 0;
        try {
            return recruiterRepository.countRecruitersByUserId(userId);
        } catch (SQLException e) {
            return 0;
        }
    }

    /**
     * Returns count of applications linked to this recruiter.
     */
    public int getAssociatedApplicationsCount(int recruiterId, int userId) {
        if (recruiterId <= 0 || userId <= 0) return 0;
        try {
            return recruiterRepository.countAssociatedApplications(recruiterId, userId);
        } catch (SQLException e) {
            return 0;
        }
    }

    /**
     * Returns count of interviews linked to this recruiter's applications.
     */
    public int getAssociatedInterviewsCount(int recruiterId, int userId) {
        if (recruiterId <= 0 || userId <= 0) return 0;
        try {
            return recruiterRepository.countAssociatedInterviews(recruiterId, userId);
        } catch (SQLException e) {
            return 0;
        }
    }

    /**
     * Validates recruiter input formats and requirements.
     */
    private void validateRecruiterInput(String recruiterName, String email, String phone, String linkedinUrl)
            throws ValidationException {
        if (!ValidationUtil.isNotEmpty(recruiterName)) {
            throw new ValidationException("Recruiter name is required.");
        }
        if (email != null && !email.isBlank() && !ValidationUtil.isValidEmail(email.trim())) {
            throw new ValidationException("Invalid email format: " + email.trim());
        }
        if (phone != null && !phone.isBlank() && !ValidationUtil.isValidPhone(phone.trim())) {
            throw new ValidationException("Invalid phone number: " + phone.trim() + ". Must contain 7-20 digits and standard phone symbols.");
        }
        if (linkedinUrl != null && !linkedinUrl.isBlank() && !ValidationUtil.isValidUrl(linkedinUrl.trim())) {
            throw new ValidationException("Invalid LinkedIn profile URL: " + linkedinUrl.trim());
        }
    }
}

