-- ===================================================================
-- ApplyTrack: Job Application Intelligence System
-- Database Schema Definition (MySQL / MariaDB Compatible)
-- Character Set: utf8mb4 | Storage Engine: InnoDB
-- ===================================================================

CREATE DATABASE IF NOT EXISTS applytrack_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE applytrack_db;

-- -------------------------------------------------------------
-- Table 1: users
-- Stores candidate accounts for secure login and profile mapping
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------
-- Table 2: companies
-- Stores enterprise profiles and hiring organizations
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS companies (
    company_id INT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(150) NOT NULL UNIQUE,
    website VARCHAR(255),
    industry VARCHAR(100),
    location VARCHAR(150),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------
-- Table 3: recruiters
-- Stores recruiter contacts linked to companies
-- Rule: Deleting a company sets company_id to NULL (does not delete recruiter)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS recruiters (
    recruiter_id INT AUTO_INCREMENT PRIMARY KEY,
    company_id INT NULL,
    recruiter_name VARCHAR(100) NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(25),
    linkedin_url VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recruiter_company FOREIGN KEY (company_id)
        REFERENCES companies(company_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------
-- Table 4: applications
-- Core application tracking table
-- Rules:
-- - Deleting user cascades to applications (user's data removed)
-- - Deleting company is RESTRICTED if applications exist (prevents accidental loss)
-- - Deleting recruiter sets recruiter_id to NULL (application preserved)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS applications (
    application_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    company_id INT NOT NULL,
    recruiter_id INT NULL,
    job_role VARCHAR(150) NOT NULL,
    job_type VARCHAR(50) NOT NULL DEFAULT 'Full-time',
    application_date DATE NOT NULL,
    deadline DATE NULL,
    job_description TEXT,
    job_url VARCHAR(500),
    expected_salary DECIMAL(12, 2) NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'Applied',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_app_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_app_company FOREIGN KEY (company_id)
        REFERENCES companies(company_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_app_recruiter FOREIGN KEY (recruiter_id)
        REFERENCES recruiters(recruiter_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------
-- Table 5: interviews
-- Multi-round interview pipeline tracking
-- Rule: Deleting an application cascades to its interview rounds
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS interviews (
    interview_id INT AUTO_INCREMENT PRIMARY KEY,
    application_id INT NOT NULL,
    round_name VARCHAR(100) NOT NULL,
    interview_date DATETIME NOT NULL,
    interview_mode VARCHAR(50) NOT NULL DEFAULT 'Online',
    interview_location VARCHAR(255) NULL,
    meeting_link VARCHAR(500) NULL,
    interviewer_name VARCHAR(100) NULL,
    feedback TEXT NULL,
    outcome VARCHAR(50) NOT NULL DEFAULT 'Scheduled',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_interview_app FOREIGN KEY (application_id)
        REFERENCES applications(application_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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

-- -------------------------------------------------------------
-- Performance Indexes for Search, Filtering & Analytics
-- -------------------------------------------------------------
CREATE INDEX idx_apps_status ON applications(status);
CREATE INDEX idx_apps_date ON applications(application_date);
CREATE INDEX idx_apps_deadline ON applications(deadline);
CREATE INDEX idx_apps_role ON applications(job_role);
CREATE INDEX idx_interviews_date ON interviews(interview_date);
CREATE INDEX idx_interviews_outcome ON interviews(outcome);
