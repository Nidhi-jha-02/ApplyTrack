package com.applytrack.model.enums;

/**
 * Enumeration representing supported visual themes in ApplyTrack.
 *
 * Demonstrates:
 * - Java Enums with metadata and custom fields (Unit 2)
 * - Safe string parsing with fallback default (Unit 2)
 */
public enum AppTheme {
    LIGHT("Light Theme", "Clean, high-readability SaaS theme with bright panels", "/com/applytrack/css/styles.css"),
    DARK("Dark Theme", "Modern sleek dark theme optimized for low-light environments", "/com/applytrack/css/dark-theme.css");

    private final String displayName;
    private final String description;
    private final String stylesheetPath;

    AppTheme(String displayName, String description, String stylesheetPath) {
        this.displayName = displayName;
        this.description = description;
        this.stylesheetPath = stylesheetPath;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getStylesheetPath() {
        return stylesheetPath;
    }

    /**
     * Case-insensitive parser with safe fallback to LIGHT.
     */
    public static AppTheme fromString(String themeStr) {
        if (themeStr == null || themeStr.isBlank()) {
            return LIGHT;
        }
        for (AppTheme t : values()) {
            if (t.name().equalsIgnoreCase(themeStr.trim()) ||
                t.displayName.equalsIgnoreCase(themeStr.trim())) {
                return t;
            }
        }
        return LIGHT;
    }
}

