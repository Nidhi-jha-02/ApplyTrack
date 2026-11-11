package com.applytrack.model;

import com.applytrack.model.enums.AppTheme;
import com.applytrack.model.enums.DateRangeFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for UserPreferences entity and AppTheme enum.
 *
 * Demonstrates:
 * - Domain model testing & Encapsulation verification (Unit 3)
 * - Safe default values preventing NullPointerExceptions (Unit 1 & Unit 3)
 * - Enum parser behavior with case-insensitive and fallback matching (Unit 2)
 */
public class UserPreferencesTest {

    @Test
    @DisplayName("UserPreferences.defaultForUser provides sensible default configuration")
    void testDefaultForUser() {
        UserPreferences prefs = UserPreferences.defaultForUser(42);

        assertEquals(42, prefs.getUserId());
        assertEquals(AppTheme.LIGHT, prefs.getTheme());
        assertEquals("DASHBOARD", prefs.getDefaultLandingPage());
        assertEquals(DateRangeFilter.ALL_TIME, prefs.getPreferredAnalyticsRange());
        assertTrue(prefs.isReminderUpcomingInterviews());
        assertTrue(prefs.isReminderApplicationDeadlines());
    }

    @Test
    @DisplayName("UserPreferences setters validate and sanitize inputs")
    void testSettersAndGetters() {
        UserPreferences prefs = new UserPreferences(1);
        prefs.setPreferenceId(10);
        prefs.setTheme(AppTheme.DARK);
        prefs.setDefaultLandingPage("applications");
        prefs.setPreferredAnalyticsRange(DateRangeFilter.LAST_30_DAYS);
        prefs.setReminderUpcomingInterviews(false);
        prefs.setReminderApplicationDeadlines(false);
        LocalDateTime now = LocalDateTime.now();
        prefs.setCreatedAt(now);
        prefs.setUpdatedAt(now);

        assertEquals(10, prefs.getPreferenceId());
        assertEquals(1, prefs.getUserId());
        assertEquals(AppTheme.DARK, prefs.getTheme());
        assertEquals("APPLICATIONS", prefs.getDefaultLandingPage());
        assertEquals(DateRangeFilter.LAST_30_DAYS, prefs.getPreferredAnalyticsRange());
        assertFalse(prefs.isReminderUpcomingInterviews());
        assertFalse(prefs.isReminderApplicationDeadlines());
        assertEquals(now, prefs.getCreatedAt());
        assertEquals(now, prefs.getUpdatedAt());
    }

    @Test
    @DisplayName("UserPreferences handles null values gracefully with fallback defaults")
    void testNullSafetyInSetters() {
        UserPreferences prefs = new UserPreferences();
        prefs.setTheme(null);
        prefs.setDefaultLandingPage(null);
        prefs.setPreferredAnalyticsRange(null);

        assertEquals(AppTheme.LIGHT, prefs.getTheme());
        assertEquals("DASHBOARD", prefs.getDefaultLandingPage());
        assertEquals(DateRangeFilter.ALL_TIME, prefs.getPreferredAnalyticsRange());
    }

    @Test
    @DisplayName("AppTheme enum parses case-insensitively and falls back to LIGHT on unknown string")
    void testAppThemeParser() {
        assertEquals(AppTheme.LIGHT, AppTheme.fromString("light"));
        assertEquals(AppTheme.LIGHT, AppTheme.fromString("Light Theme"));
        assertEquals(AppTheme.DARK, AppTheme.fromString("dark"));
        assertEquals(AppTheme.DARK, AppTheme.fromString("Dark Theme"));
        assertEquals(AppTheme.LIGHT, AppTheme.fromString(null));
        assertEquals(AppTheme.LIGHT, AppTheme.fromString("unknown_theme"));
    }
}

