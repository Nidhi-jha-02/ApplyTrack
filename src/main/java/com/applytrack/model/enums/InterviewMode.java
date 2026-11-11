package com.applytrack.model.enums;

/**
 * Enumeration representing the mode of an interview round.
 */
public enum InterviewMode {
    ONLINE("Online"),
    OFFLINE("Offline");

    private final String displayName;

    InterviewMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static InterviewMode fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return ONLINE;
        }
        for (InterviewMode mode : values()) {
            if (mode.name().equalsIgnoreCase(text.trim()) ||
                mode.displayName.equalsIgnoreCase(text.trim())) {
                return mode;
            }
        }
        return ONLINE;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
