package com.applytrack.model.enums;

/**
 * Enumeration representing the complete lifecycle status of a job application.
 *
 * Demonstrates:
 * - Java Enums with custom fields, constructors, and methods (Unit 2)
 * - Enhanced switch & static parsing logic (Unit 1 & Unit 2)
 */
public enum ApplicationStatus {
    APPLIED("Applied", "#6366f1", false),
    UNDER_REVIEW("Under Review", "#0ea5e9", false),
    SHORTLISTED("Shortlisted", "#8b5cf6", false),
    INTERVIEW_SCHEDULED("Interview Scheduled", "#f59e0b", false),
    SELECTED("Selected", "#10b981", true),
    REJECTED("Rejected", "#ef4444", true),
    WITHDRAWN("Withdrawn", "#64748b", true),
    NO_RESPONSE("No Response", "#94a3b8", true);

    private final String displayName;
    private final String badgeColor;
    private final boolean terminal;

    ApplicationStatus(String displayName, String badgeColor, boolean terminal) {
        this.displayName = displayName;
        this.badgeColor = badgeColor;
        this.terminal = terminal;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeColor() {
        return badgeColor;
    }

    public boolean isTerminal() {
        return terminal;
    }

    /**
     * Converts a database or UI string into its corresponding enum constant.
     * Fallback to APPLIED if unrecognized.
     */
    public static ApplicationStatus fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return APPLIED;
        }
        for (ApplicationStatus status : values()) {
            if (status.name().equalsIgnoreCase(text.trim()) ||
                status.displayName.equalsIgnoreCase(text.trim()) ||
                status.name().replace("_", " ").equalsIgnoreCase(text.trim())) {
                return status;
            }
        }
        return APPLIED;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
