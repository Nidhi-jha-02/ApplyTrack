package com.applytrack.model.enums;

/**
 * Enumeration representing the employment type for a job position.
 */
public enum JobType {
    FULL_TIME("Full-time"),
    INTERNSHIP("Internship"),
    PART_TIME("Part-time");

    private final String displayName;

    JobType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static JobType fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return FULL_TIME;
        }
        for (JobType type : values()) {
            if (type.name().equalsIgnoreCase(text.trim()) ||
                type.displayName.equalsIgnoreCase(text.trim()) ||
                type.name().replace("_", "-").equalsIgnoreCase(text.trim())) {
                return type;
            }
        }
        return FULL_TIME;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
