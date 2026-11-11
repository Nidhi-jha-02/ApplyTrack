-- ===================================================================
-- ApplyTrack: Job Application Intelligence System
-- Database Migration: Phase 11 — User Preferences Table
-- Character Set: utf8mb4 | Storage Engine: InnoDB
-- ===================================================================

USE applytrack_db;

-- -------------------------------------------------------------
-- Table 6: user_preferences
-- Stores per-user customization, themes, and dashboard preferences
-- Rules:
-- - One preference row per user (user_id UNIQUE)
-- - Deleting a user cascades to remove their preferences
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_preferences (
    preference_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    theme VARCHAR(20) NOT NULL DEFAULT 'LIGHT',
    default_landing_page VARCHAR(50) NOT NULL DEFAULT 'DASHBOARD',
    preferred_analytics_range VARCHAR(50) NOT NULL DEFAULT 'ALL_TIME',
    reminder_upcoming_interviews BOOLEAN NOT NULL DEFAULT TRUE,
    reminder_application_deadlines BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_pref_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed default preferences for existing demo users if not present
INSERT IGNORE INTO user_preferences (user_id, theme, default_landing_page, preferred_analytics_range, reminder_upcoming_interviews, reminder_application_deadlines)
SELECT user_id, 'LIGHT', 'DASHBOARD', 'ALL_TIME', TRUE, TRUE FROM users;

