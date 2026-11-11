package com.applytrack.model;

import com.applytrack.model.enums.AppTheme;
import com.applytrack.model.enums.DateRangeFilter;

import java.time.LocalDateTime;

/**
 * Model entity representing user-specific configuration, theme choices, and application preferences.
 *
 * Demonstrates:
 * - Domain Entity encapsulation with validation (Unit 3)
 * - Safe default values preventing NullPointerExceptions (Unit 1 & Unit 3)
 * - Java Enums integration (Unit 2)
 */
public class UserPreferences {

    private int preferenceId;
    private int userId;
    private AppTheme theme = AppTheme.LIGHT;
    private String defaultLandingPage = "DASHBOARD";
    private DateRangeFilter preferredAnalyticsRange = DateRangeFilter.ALL_TIME;
    private boolean reminderUpcomingInterviews = true;
    private boolean reminderApplicationDeadlines = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserPreferences() {
    }

    public UserPreferences(int userId) {
        this.userId = userId;
    }

    public UserPreferences(int preferenceId, int userId, AppTheme theme, String defaultLandingPage,
                           DateRangeFilter preferredAnalyticsRange, boolean reminderUpcomingInterviews,
                           boolean reminderApplicationDeadlines, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.preferenceId = preferenceId;
        this.userId = userId;
        this.theme = (theme != null) ? theme : AppTheme.LIGHT;
        this.defaultLandingPage = (defaultLandingPage != null && !defaultLandingPage.isBlank()) ? defaultLandingPage.toUpperCase() : "DASHBOARD";
        this.preferredAnalyticsRange = (preferredAnalyticsRange != null) ? preferredAnalyticsRange : DateRangeFilter.ALL_TIME;
        this.reminderUpcomingInterviews = reminderUpcomingInterviews;
        this.reminderApplicationDeadlines = reminderApplicationDeadlines;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserPreferences defaultForUser(int userId) {
        UserPreferences p = new UserPreferences(userId);
        p.setTheme(AppTheme.LIGHT);
        p.setDefaultLandingPage("DASHBOARD");
        p.setPreferredAnalyticsRange(DateRangeFilter.ALL_TIME);
        p.setReminderUpcomingInterviews(true);
        p.setReminderApplicationDeadlines(true);
        return p;
    }

    public int getPreferenceId() {
        return preferenceId;
    }

    public void setPreferenceId(int preferenceId) {
        this.preferenceId = preferenceId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public AppTheme getTheme() {
        return theme;
    }

    public void setTheme(AppTheme theme) {
        this.theme = (theme != null) ? theme : AppTheme.LIGHT;
    }

    public String getDefaultLandingPage() {
        return defaultLandingPage;
    }

    public void setDefaultLandingPage(String defaultLandingPage) {
        this.defaultLandingPage = (defaultLandingPage != null && !defaultLandingPage.isBlank())
                ? defaultLandingPage.trim().toUpperCase()
                : "DASHBOARD";
    }

    public DateRangeFilter getPreferredAnalyticsRange() {
        return preferredAnalyticsRange;
    }

    public void setPreferredAnalyticsRange(DateRangeFilter preferredAnalyticsRange) {
        this.preferredAnalyticsRange = (preferredAnalyticsRange != null) ? preferredAnalyticsRange : DateRangeFilter.ALL_TIME;
    }

    public boolean isReminderUpcomingInterviews() {
        return reminderUpcomingInterviews;
    }

    public void setReminderUpcomingInterviews(boolean reminderUpcomingInterviews) {
        this.reminderUpcomingInterviews = reminderUpcomingInterviews;
    }

    public boolean isReminderApplicationDeadlines() {
        return reminderApplicationDeadlines;
    }

    public void setReminderApplicationDeadlines(boolean reminderApplicationDeadlines) {
        this.reminderApplicationDeadlines = reminderApplicationDeadlines;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "UserPreferences{" +
                "userId=" + userId +
                ", theme=" + theme +
                ", defaultLandingPage='" + defaultLandingPage + '\'' +
                ", preferredAnalyticsRange=" + preferredAnalyticsRange +
                ", reminderUpcomingInterviews=" + reminderUpcomingInterviews +
                ", reminderApplicationDeadlines=" + reminderApplicationDeadlines +
                '}';
    }
}

