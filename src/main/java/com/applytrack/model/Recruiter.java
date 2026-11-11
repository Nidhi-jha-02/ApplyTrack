package com.applytrack.model;

import com.applytrack.model.base.BaseEntity;
import com.applytrack.model.interfaces.Searchable;
import com.applytrack.util.ValidationUtil;

import java.time.LocalDateTime;

/**
 * Represents a campus recruiter, talent partner, or hiring contact.
 *
 * Demonstrates:
 * - Inheritance: extends BaseEntity (Unit 3)
 * - Interfaces: implements Searchable (Unit 3)
 * - Constructor Overloading and Chaining (Unit 2 & 3)
 * - Encapsulation and Data Validation (Unit 3)
 * - Object Relationships: Optional Company reference without circular recursion (Unit 2 & 3)
 */
public class Recruiter extends BaseEntity implements Searchable {

    private Integer companyId;
    private Company company; // Optional domain reference
    private String recruiterName;
    private String email;
    private String phone;
    private String linkedinUrl;
    private String notes;

    public Recruiter() {
        super();
    }

    public Recruiter(String recruiterName, String email) {
        this(0, null, recruiterName, email, null, null, null, LocalDateTime.now(), LocalDateTime.now());
    }

    public Recruiter(String recruiterName, Integer companyId, String email, String phone,
                     String linkedinUrl, String notes) {
        this(0, companyId, recruiterName, email, phone, linkedinUrl, notes, LocalDateTime.now(), LocalDateTime.now());
    }

    public Recruiter(int recruiterId, Integer companyId, String recruiterName, String email,
                     String phone, String linkedinUrl, String notes,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(recruiterId, createdAt, updatedAt);
        this.companyId = companyId;
        setRecruiterName(recruiterName);
        setEmail(email);
        setPhone(phone);
        setLinkedinUrl(linkedinUrl);
        setNotes(notes);
    }

    // Convenience alias for entity ID
    public int getRecruiterId() {
        return getId();
    }

    public void setRecruiterId(int recruiterId) {
        setId(recruiterId);
    }

    public Integer getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Integer companyId) {
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

    public String getRecruiterName() {
        return recruiterName;
    }

    /**
     * Convenience getter for UI TableView bindings and presentation.
     */
    public String getCompanyName() {
        return (company != null && company.getCompanyName() != null)
                ? company.getCompanyName()
                : "Unaffiliated";
    }

    // Associated applications counter for UI presentation
    private int associatedApplicationsCount = 0;

    public int getAssociatedApplicationsCount() {
        return associatedApplicationsCount;
    }

    public void setAssociatedApplicationsCount(int associatedApplicationsCount) {
        this.associatedApplicationsCount = Math.max(0, associatedApplicationsCount);
    }

    public String getFormattedApplicationsCount() {
        return associatedApplicationsCount + (associatedApplicationsCount == 1 ? " Application" : " Applications");
    }

    public void setRecruiterName(String recruiterName) {
        if (!ValidationUtil.isNotEmpty(recruiterName)) {
            throw new IllegalArgumentException("Recruiter name cannot be null or empty.");
        }
        this.recruiterName = recruiterName.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email != null && !email.trim().isEmpty() && !ValidationUtil.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid recruiter email: " + email);
        }
        this.email = (email != null) ? email.trim().toLowerCase() : null;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        if (phone != null && !phone.trim().isEmpty() && !ValidationUtil.isValidPhone(phone)) {
            throw new IllegalArgumentException("Invalid phone number: " + phone);
        }
        this.phone = (phone != null) ? phone.trim() : null;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public void setLinkedinUrl(String linkedinUrl) {
        if (linkedinUrl != null && !linkedinUrl.trim().isEmpty() && !ValidationUtil.isValidUrl(linkedinUrl)) {
            throw new IllegalArgumentException("Invalid LinkedIn profile URL: " + linkedinUrl);
        }
        this.linkedinUrl = (linkedinUrl != null) ? linkedinUrl.trim() : null;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = (notes != null) ? notes.trim() : null;
    }

    @Override
    public boolean matches(String query) {
        if (!ValidationUtil.isNotEmpty(query)) {
            return true;
        }
        String q = query.trim().toLowerCase();
        return (recruiterName != null && recruiterName.toLowerCase().contains(q)) ||
               (email != null && email.toLowerCase().contains(q)) ||
               (phone != null && phone.toLowerCase().contains(q)) ||
               (notes != null && notes.toLowerCase().contains(q)) ||
               (company != null && company.matches(q));
    }

    @Override
    public String getDisplayTitle() {
        String title = recruiterName;
        if (company != null) {
            title += " @ " + company.getCompanyName();
        }
        return title;
    }

    @Override
    public String toString() {
        return "Recruiter{" +
                "recruiterId=" + getId() +
                ", recruiterName='" + recruiterName + '\'' +
                ", companyId=" + companyId +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}

