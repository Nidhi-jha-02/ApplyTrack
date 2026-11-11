package com.applytrack.model;

import com.applytrack.model.base.BaseEntity;
import com.applytrack.model.enums.ApplicationStatus;
import com.applytrack.model.enums.JobType;
import com.applytrack.model.interfaces.Searchable;
import com.applytrack.util.ValidationUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The central entity of ApplyTrack representing a job or internship application.
 *
 * Demonstrates:
 * - Inheritance: extends BaseEntity (Unit 3)
 * - Interfaces: implements Searchable (Unit 3)
 * - Modern Java Date/Time & Monetary types: LocalDate, LocalDateTime, BigDecimal (Unit 1 & 2)
 * - Java Enums: ApplicationStatus, JobType (Unit 2)
 * - 1:N Relationship modeling: List<Interview> with defensive unmodifiable access (Unit 3)
 * - Constructor Overloading and Chaining (Unit 2 & 3)
 * - Strict Encapsulation and Data Validation (Unit 3)
 */
public class Application extends BaseEntity implements Searchable {

    private int userId;
    private User user;

    private int companyId;
    private Company company;

    private Integer recruiterId;
    private Recruiter recruiter;

    private String jobRole;
    private JobType jobType;
    private LocalDate applicationDate;
    private LocalDate deadline;
    private String jobDescription;
    private String jobUrl;
    private BigDecimal expectedSalary;
    private ApplicationStatus status;
    private String notes;

    // 1:N relationship with interview rounds (Collections Framework - Unit 3)
    private final List<Interview> interviews = new ArrayList<>();

    public Application() {
        super();
        this.jobType = JobType.FULL_TIME;
        this.status = ApplicationStatus.APPLIED;
        this.applicationDate = LocalDate.now();
    }

    /**
     * Minimal constructor for quickly creating a job application (Unit 2 & 3 Overloading).
     */
    public Application(int userId, int companyId, String jobRole, LocalDate applicationDate) {
        this(0, userId, companyId, null, jobRole, JobType.FULL_TIME, applicationDate, null,
                null, null, null, ApplicationStatus.APPLIED, null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    /**
     * Standard constructor for typical UI form submissions.
     */
    public Application(int userId, int companyId, Integer recruiterId, String jobRole, JobType jobType,
                       LocalDate applicationDate, LocalDate deadline, BigDecimal expectedSalary,
                       ApplicationStatus status, String notes) {
        this(0, userId, companyId, recruiterId, jobRole, jobType, applicationDate, deadline,
                null, null, expectedSalary, status, notes,
                LocalDateTime.now(), LocalDateTime.now());
    }

    /**
     * Full parameterized constructor matching database rows.
     */
    public Application(int applicationId, int userId, int companyId, Integer recruiterId,
                       String jobRole, JobType jobType, LocalDate applicationDate, LocalDate deadline,
                       String jobDescription, String jobUrl, BigDecimal expectedSalary,
                       ApplicationStatus status, String notes,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(applicationId, createdAt, updatedAt);
        this.userId = userId;
        this.companyId = companyId;
        this.recruiterId = recruiterId;
        setJobRole(jobRole);
        this.jobType = (jobType != null) ? jobType : JobType.FULL_TIME;
        setApplicationDate(applicationDate);
        setDeadline(deadline);
        this.jobDescription = jobDescription;
        setJobUrl(jobUrl);
        setExpectedSalary(expectedSalary);
        this.status = (status != null) ? status : ApplicationStatus.APPLIED;
        this.notes = notes;
    }

    // Convenience alias for entity ID
    public int getApplicationId() {
        return getId();
    }

    public void setApplicationId(int applicationId) {
        setId(applicationId);
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.userId = user.getId();
        }
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
        if (company != null) {
            this.companyId = company.getId();
        }
    }

    /**
     * Convenience getter for TableView PropertyValueFactory and UI bindings.
     */
    public String getCompanyName() {
        return (company != null && company.getCompanyName() != null) ? company.getCompanyName() : "Unknown Company";
    }

    public Integer getRecruiterId() {
        return recruiterId;
    }

    public void setRecruiterId(Integer recruiterId) {
        this.recruiterId = recruiterId;
    }

    public Recruiter getRecruiter() {
        return recruiter;
    }

    public void setRecruiter(Recruiter recruiter) {
        this.recruiter = recruiter;
        if (recruiter != null) {
            this.recruiterId = recruiter.getId();
        }
    }

    public String getJobRole() {
        return jobRole;
    }

    public void setJobRole(String jobRole) {
        if (!ValidationUtil.isNotEmpty(jobRole)) {
            throw new IllegalArgumentException("Job role cannot be null or empty.");
        }
        this.jobRole = jobRole.trim();
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = (jobType != null) ? jobType : JobType.FULL_TIME;
    }

    public LocalDate getApplicationDate() {
        return applicationDate;
    }

    public void setApplicationDate(LocalDate applicationDate) {
        if (applicationDate == null) {
            throw new IllegalArgumentException("Application date cannot be null.");
        }
        this.applicationDate = applicationDate;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        if (deadline != null && !ValidationUtil.isValidDeadline(deadline, applicationDate)) {
            throw new IllegalArgumentException("Application deadline (" + deadline +
                    ") cannot be earlier than application date (" + applicationDate + ").");
        }
        this.deadline = deadline;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = (jobDescription != null) ? jobDescription.trim() : null;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public void setJobUrl(String jobUrl) {
        if (jobUrl != null && !jobUrl.trim().isEmpty() && !ValidationUtil.isValidUrl(jobUrl)) {
            throw new IllegalArgumentException("Invalid job URL: " + jobUrl);
        }
        this.jobUrl = (jobUrl != null) ? jobUrl.trim() : null;
    }

    public BigDecimal getExpectedSalary() {
        return expectedSalary;
    }

    public void setExpectedSalary(BigDecimal expectedSalary) {
        if (expectedSalary != null && !ValidationUtil.isNonNegativeSalary(expectedSalary)) {
            throw new IllegalArgumentException("Expected salary cannot be negative: " + expectedSalary);
        }
        this.expectedSalary = expectedSalary;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = (status != null) ? status : ApplicationStatus.APPLIED;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = (notes != null) ? notes.trim() : null;
    }

    // Interview collection management (1:N Relationship)
    public List<Interview> getInterviews() {
        return Collections.unmodifiableList(interviews);
    }

    public void addInterview(Interview interview) {
        if (interview != null && !interviews.contains(interview)) {
            interviews.add(interview);
            interview.setApplicationId(this.getId());
        }
    }

    public void removeInterview(Interview interview) {
        interviews.remove(interview);
    }

    public void setInterviews(List<Interview> newInterviews) {
        this.interviews.clear();
        if (newInterviews != null) {
            for (Interview i : newInterviews) {
                addInterview(i);
            }
        }
    }

    @Override
    public boolean matches(String query) {
        if (!ValidationUtil.isNotEmpty(query)) {
            return true;
        }
        String q = query.trim().toLowerCase();
        return (jobRole != null && jobRole.toLowerCase().contains(q)) ||
               (status != null && status.getDisplayName().toLowerCase().contains(q)) ||
               (jobType != null && jobType.getDisplayName().toLowerCase().contains(q)) ||
               (notes != null && notes.toLowerCase().contains(q)) ||
               (jobDescription != null && jobDescription.toLowerCase().contains(q)) ||
               (company != null && company.matches(q)) ||
               (recruiter != null && recruiter.matches(q));
    }

    @Override
    public String getDisplayTitle() {
        String companyStr = (company != null) ? company.getCompanyName() : "Company #" + companyId;
        return jobRole + " @ " + companyStr + " [" + status.getDisplayName() + "]";
    }

    @Override
    public String toString() {
        return "Application{" +
                "applicationId=" + getId() +
                ", userId=" + userId +
                ", companyId=" + companyId +
                ", jobRole='" + jobRole + '\'' +
                ", jobType=" + jobType +
                ", status=" + status +
                ", applicationDate=" + applicationDate +
                ", deadline=" + deadline +
                '}';
    }
}

