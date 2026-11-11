package com.applytrack.model.enums;

/**
 * Enumeration representing the outcome of an interview round.
 *
 * Demonstrates:
 * - Enums with custom fields, constructors, and display logic (Unit 2)
 * - Safe string conversion from database records (Unit 2)
 */
public enum InterviewOutcome {
    SCHEDULED("Scheduled", "#f59e0b"),
    PENDING("Pending", "#0ea5e9"),
    PASSED("Passed", "#10b981"),
    FAILED("Failed", "#ef4444"),
    ON_HOLD("On Hold", "#8b5cf6"),
    CANCELLED("Cancelled", "#64748b");

    private final String displayName;
    private final String badgeColor;

    InterviewOutcome(String displayName, String badgeColor) {
        this.displayName = displayName;
        this.badgeColor = badgeColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeColor() {
        return badgeColor;
    }

    public static InterviewOutcome fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return SCHEDULED;
        }
        for (InterviewOutcome outcome : values()) {
            if (outcome.name().equalsIgnoreCase(text.trim()) ||
                outcome.displayName.equalsIgnoreCase(text.trim()) ||
                outcome.name().replace("_", " ").equalsIgnoreCase(text.trim())) {
                return outcome;
            }
        }
        return SCHEDULED;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
