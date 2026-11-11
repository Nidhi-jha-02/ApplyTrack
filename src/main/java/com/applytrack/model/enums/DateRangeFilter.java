package com.applytrack.model.enums;

import java.time.LocalDate;

/**
 * Enumeration representing predefined date range filters for analytics.
 *
 * Demonstrates:
 * - Java Enums with custom fields, methods, and switch logic (Unit 2)
 * - Modern Date/Time calculations with LocalDate (Unit 1)
 */
public enum DateRangeFilter {
    ALL_TIME("All Time", null),
    LAST_30_DAYS("Last 30 Days", 30),
    LAST_3_MONTHS("Last 3 Months", 90),
    LAST_6_MONTHS("Last 6 Months", 180),
    LAST_12_MONTHS("Last 12 Months", 365);

    private final String displayName;
    private final Integer approximateDays;

    DateRangeFilter(String displayName, Integer approximateDays) {
        this.displayName = displayName;
        this.approximateDays = approximateDays;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Integer getApproximateDays() {
        return approximateDays;
    }

    /**
     * Calculates the lower bound start date relative to the current date.
     * Returns null if ALL_TIME is selected (no lower bound).
     */
    public LocalDate getStartDate() {
        return getStartDate(LocalDate.now());
    }

    /**
     * Calculates the lower bound start date relative to a provided reference date.
     * Useful for deterministic unit testing.
     */
    public LocalDate getStartDate(LocalDate referenceDate) {
        if (referenceDate == null) {
            referenceDate = LocalDate.now();
        }

        return switch (this) {
            case ALL_TIME -> null;
            case LAST_30_DAYS -> referenceDate.minusDays(30);
            case LAST_3_MONTHS -> referenceDate.minusMonths(3);
            case LAST_6_MONTHS -> referenceDate.minusMonths(6);
            case LAST_12_MONTHS -> referenceDate.minusMonths(12);
        };
    }

    public static DateRangeFilter fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return ALL_TIME;
        }
        for (DateRangeFilter filter : values()) {
            if (filter.name().equalsIgnoreCase(text.trim()) ||
                filter.displayName.equalsIgnoreCase(text.trim())) {
                return filter;
            }
        }
        return ALL_TIME;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

