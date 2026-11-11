package com.applytrack.model;

import com.applytrack.model.base.BaseEntity;
import com.applytrack.model.interfaces.Searchable;
import com.applytrack.util.ValidationUtil;

import java.time.LocalDateTime;

/**
 * Represents a hiring organization, enterprise, or startup.
 *
 * Demonstrates:
 * - Inheritance: extends BaseEntity (Unit 3)
 * - Interfaces: implements Searchable (Unit 3)
 * - Constructor Overloading (Unit 2 & 3)
 * - Encapsulation and Data Validation (Unit 3)
 * - Polymorphism via search and display title contracts (Unit 3)
 */
public class Company extends BaseEntity implements Searchable {

    private String companyName;
    private String website;
    private String industry;
    private String location;

    public Company() {
        super();
    }

    public Company(String companyName) {
        this(0, companyName, null, null, null, LocalDateTime.now(), LocalDateTime.now());
    }

    public Company(String companyName, String website, String industry, String location) {
        this(0, companyName, website, industry, location, LocalDateTime.now(), LocalDateTime.now());
    }

    public Company(int companyId, String companyName, String website, String industry, String location,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(companyId, createdAt, updatedAt);
        setCompanyName(companyName);
        setWebsite(website);
        setIndustry(industry);
        setLocation(location);
    }

    // Convenience alias for entity ID
    public int getCompanyId() {
        return getId();
    }

    public void setCompanyId(int companyId) {
        setId(companyId);
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        if (!ValidationUtil.isNotEmpty(companyName)) {
            throw new IllegalArgumentException("Company name cannot be null or empty.");
        }
        this.companyName = companyName.trim();
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        if (website != null && !website.trim().isEmpty() && !ValidationUtil.isValidUrl(website)) {
            throw new IllegalArgumentException("Invalid website URL: " + website);
        }
        this.website = (website != null) ? website.trim() : null;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = (industry != null) ? industry.trim() : null;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = (location != null) ? location.trim() : null;
    }

    @Override
    public boolean matches(String query) {
        if (!ValidationUtil.isNotEmpty(query)) {
            return true;
        }
        String q = query.trim().toLowerCase();
        return (companyName != null && companyName.toLowerCase().contains(q)) ||
               (industry != null && industry.toLowerCase().contains(q)) ||
               (location != null && location.toLowerCase().contains(q));
    }

    @Override
    public String getDisplayTitle() {
        return companyName + (location != null ? " (" + location + ")" : "");
    }

    @Override
    public String toString() {
        return "Company{" +
                "companyId=" + getId() +
                ", companyName='" + companyName + '\'' +
                ", website='" + website + '\'' +
                ", industry='" + industry + '\'' +
                ", location='" + location + '\'' +
                '}';
    }
}

