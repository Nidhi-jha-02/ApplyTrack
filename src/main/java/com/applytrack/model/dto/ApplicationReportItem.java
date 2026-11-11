package com.applytrack.model.dto;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Data Transfer Object representing a row in the Job Application Report.
 *
 * Demonstrates:
 * - DTO pattern & Encapsulation (Unit 2 & Unit 3)
 * - Safe numeric currency and date formatting (Unit 1)
 */
public class ApplicationReportItem {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private int applicationId;
    private String companyName;
    private String jobRole;
    private String jobType;
    private LocalDate applicationDate;
    private LocalDate deadline;
    private String status;
    private BigDecimal salary;
    private String recruiterName;
    private String notes;

    public ApplicationReportItem() {
    }

    public ApplicationReportItem(int applicationId, String companyName, String jobRole, String jobType,
                                 LocalDate applicationDate, LocalDate deadline, String status,
                                 BigDecimal salary, String recruiterName, String notes) {
        this.applicationId = applicationId;
        this.companyName = companyName != null ? companyName : "Unknown";
        this.jobRole = jobRole != null ? jobRole : "";
        this.jobType = jobType != null ? jobType : "Full-time";
        this.applicationDate = applicationDate;
        this.deadline = deadline;
        this.status = status != null ? status : "Applied";
        this.salary = salary;
        this.recruiterName = recruiterName != null && !recruiterName.isBlank() ? recruiterName : "None";
        this.notes = notes != null ? notes : "";
    }

    public int getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getJobRole() {
        return jobRole;
    }

    public void setJobRole(String jobRole) {
        this.jobRole = jobRole;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public LocalDate getApplicationDate() {
        return applicationDate;
    }

    public void setApplicationDate(LocalDate applicationDate) {
        this.applicationDate = applicationDate;
    }

    public String getFormattedApplicationDate() {
        return applicationDate != null ? applicationDate.format(DATE_FORMATTER) : "—";
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public String getFormattedDeadline() {
        return deadline != null ? deadline.format(DATE_FORMATTER) : "—";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }

    public String getFormattedSalary() {
        if (salary == null || salary.compareTo(BigDecimal.ZERO) <= 0) {
            return "—";
        }
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.US);
        return nf.format(salary);
    }

    public String getRecruiterName() {
        return recruiterName;
    }

    public void setRecruiterName(String recruiterName) {
        this.recruiterName = recruiterName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

