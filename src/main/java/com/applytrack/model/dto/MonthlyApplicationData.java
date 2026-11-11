package com.applytrack.model.dto;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Data Transfer Object representing monthly aggregated application submissions.
 *
 * Demonstrates:
 * - DTO Pattern & Encapsulation (Unit 2 & Unit 3)
 * - Comparable interface implementation for chronological sorting (Unit 2)
 * - Modern Java Date/Time parsing and formatting (Unit 1)
 */
public class MonthlyApplicationData implements Comparable<MonthlyApplicationData> {

    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US);

    private String yearMonth; // e.g. "2026-10"
    private String displayLabel; // e.g. "Oct 2026"
    private int count;

    public MonthlyApplicationData() {
    }

    public MonthlyApplicationData(String yearMonth, int count) {
        this.yearMonth = yearMonth;
        this.count = count;
        this.displayLabel = formatYearMonth(yearMonth);
    }

    public MonthlyApplicationData(String yearMonth, String displayLabel, int count) {
        this.yearMonth = yearMonth;
        this.displayLabel = displayLabel;
        this.count = count;
    }

    private String formatYearMonth(String ym) {
        if (ym == null || ym.trim().isEmpty()) {
            return "Unknown";
        }
        try {
            YearMonth parsed = YearMonth.parse(ym.trim());
            return parsed.format(DISPLAY_FORMATTER);
        } catch (Exception e) {
            return ym;
        }
    }

    public String getYearMonth() {
        return yearMonth;
    }

    public void setYearMonth(String yearMonth) {
        this.yearMonth = yearMonth;
        this.displayLabel = formatYearMonth(yearMonth);
    }

    public String getDisplayLabel() {
        return displayLabel != null ? displayLabel : yearMonth;
    }

    public void setDisplayLabel(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public int compareTo(MonthlyApplicationData other) {
        if (other == null || other.yearMonth == null) {
            return 1;
        }
        if (this.yearMonth == null) {
            return -1;
        }
        return this.yearMonth.compareTo(other.yearMonth);
    }

    @Override
    public String toString() {
        return "MonthlyApplicationData{" +
                "yearMonth='" + yearMonth + '\'' +
                ", displayLabel='" + displayLabel + '\'' +
                ", count=" + count +
                '}';
    }
}

