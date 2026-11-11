package com.applytrack.model.dto;

import com.applytrack.exception.ValidationException;
import com.applytrack.model.enums.DateRangeFilter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Filter criteria for report generation, supporting predefined date ranges and custom boundaries.
 *
 * Demonstrates:
 * - DTO pattern & Encapsulation (Unit 2 & Unit 3)
 * - Custom domain validation with ValidationException (Unit 3)
 * - Modern Java Date/Time operations (Unit 1)
 */
public class ReportFilter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US);

    private DateRangeFilter dateRangeFilter;
    private boolean customRange;
    private LocalDate customStartDate;
    private LocalDate customEndDate;

    public ReportFilter() {
        this.dateRangeFilter = DateRangeFilter.ALL_TIME;
        this.customRange = false;
    }

    public static ReportFilter predefined(DateRangeFilter filter) {
        ReportFilter rf = new ReportFilter();
        rf.setDateRangeFilter(filter != null ? filter : DateRangeFilter.ALL_TIME);
        rf.setCustomRange(false);
        return rf;
    }

    public static ReportFilter custom(LocalDate startDate, LocalDate endDate) throws ValidationException {
        ReportFilter rf = new ReportFilter();
        rf.setCustomRange(true);
        rf.setCustomStartDate(startDate);
        rf.setCustomEndDate(endDate);
        rf.validate();
        return rf;
    }

    public void validate() throws ValidationException {
        if (customRange) {
            if (customStartDate != null && customEndDate != null && customStartDate.isAfter(customEndDate)) {
                throw new ValidationException("Start date cannot be after end date.");
            }
        }
    }

    public LocalDate getEffectiveStartDate() {
        if (customRange) {
            return customStartDate;
        }
        return dateRangeFilter != null ? dateRangeFilter.getStartDate() : null;
    }

    public LocalDate getEffectiveEndDate() {
        if (customRange) {
            return customEndDate != null ? customEndDate : LocalDate.now();
        }
        return LocalDate.now();
    }

    public String getFormattedRange() {
        if (customRange) {
            if (customStartDate == null && customEndDate == null) {
                return "All Time (Custom)";
            }
            if (customStartDate != null && customEndDate != null) {
                return customStartDate.format(DATE_FORMATTER) + " to " + customEndDate.format(DATE_FORMATTER);
            }
            if (customStartDate != null) {
                return "From " + customStartDate.format(DATE_FORMATTER);
            }
            return "Until " + customEndDate.format(DATE_FORMATTER);
        }
        return dateRangeFilter != null ? dateRangeFilter.getDisplayName() : "All Time";
    }

    public DateRangeFilter getDateRangeFilter() {
        return dateRangeFilter;
    }

    public void setDateRangeFilter(DateRangeFilter dateRangeFilter) {
        this.dateRangeFilter = dateRangeFilter;
    }

    public boolean isCustomRange() {
        return customRange;
    }

    public void setCustomRange(boolean customRange) {
        this.customRange = customRange;
    }

    public LocalDate getCustomStartDate() {
        return customStartDate;
    }

    public void setCustomStartDate(LocalDate customStartDate) {
        this.customStartDate = customStartDate;
    }

    public LocalDate getCustomEndDate() {
        return customEndDate;
    }

    public void setCustomEndDate(LocalDate customEndDate) {
        this.customEndDate = customEndDate;
    }

    @Override
    public String toString() {
        return "ReportFilter{" +
                "range=" + getFormattedRange() +
                ", custom=" + customRange +
                '}';
    }
}

