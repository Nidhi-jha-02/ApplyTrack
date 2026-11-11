package com.applytrack.model.dto;

/**
 * Data Transfer Object representing a row in the Recruiter Directory Report.
 *
 * Demonstrates:
 * - DTO pattern & Encapsulation (Unit 2 & Unit 3)
 */
public class RecruiterReportItem {

    private int recruiterId;
    private String recruiterName;
    private String companyName;
    private String email;
    private String phone;
    private String linkedinUrl;
    private int associatedAppsCount;
    private String notes;

    public RecruiterReportItem() {
    }

    public RecruiterReportItem(int recruiterId, String recruiterName, String companyName, String email,
                               String phone, String linkedinUrl, int associatedAppsCount, String notes) {
        this.recruiterId = recruiterId;
        this.recruiterName = recruiterName != null ? recruiterName : "Unknown";
        this.companyName = companyName != null ? companyName : "Independent / Agency";
        this.email = email != null ? email : "—";
        this.phone = phone != null ? phone : "—";
        this.linkedinUrl = linkedinUrl != null ? linkedinUrl : "—";
        this.associatedAppsCount = associatedAppsCount;
        this.notes = notes != null ? notes : "";
    }

    public int getRecruiterId() {
        return recruiterId;
    }

    public void setRecruiterId(int recruiterId) {
        this.recruiterId = recruiterId;
    }

    public String getRecruiterName() {
        return recruiterName;
    }

    public void setRecruiterName(String recruiterName) {
        this.recruiterName = recruiterName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public void setLinkedinUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
    }

    public int getAssociatedAppsCount() {
        return associatedAppsCount;
    }

    public void setAssociatedAppsCount(int associatedAppsCount) {
        this.associatedAppsCount = associatedAppsCount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

