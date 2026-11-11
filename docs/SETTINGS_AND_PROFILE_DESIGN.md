# ApplyTrack — Settings & User Profile Management Module Design Document
## Module 7 Architecture, BCrypt Security, Theming Engine, Preferences Persistence & Session Lifecycle

---

## 1. Executive Summary

The **Settings & User Profile Management Module** (Module 7 / Phase 11) provides ApplyTrack with a secure, centralized hub for personal candidate profile management, credential updates, customizable interface preferences, runtime dynamic theming, and session lifecycle control.

By encapsulating sensitive operations behind dedicated service boundaries and leveraging salted BCrypt cryptographic hashing (`org.mindrot:jbcrypt`), the module guarantees that user credentials remain protected against unauthorized alterations, rainbow table attacks, and cross-tenant data leakage. The module also introduces an isolated persistence layer for candidate preferences (`user_preferences` table) and a global `ThemeManager` capable of swapping JavaFX stylesheet states at runtime without requiring application reboots.

All settings operations strictly respect multi-tenant boundaries governed by `UserSession.getInstance()`.

---

## 2. Layered Architecture & Component Interaction

The Settings module follows ApplyTrack's strict Model-View-Controller (MVC) and layered service architecture:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             Presentation Layer                              │
│         Settings.fxml   <───────>   SettingsController.java                 │
│   (4 TabPanes: Profile, Security, Preferences, Account Info, Alerts Banner) │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │  Background Worker (Task<T>)
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                               Service Layer                                 │
│                           SettingsService.java                              │
│   (Profile Validation, BCrypt Verification, Password Rules, Preferences)    │
└──────────────────────┬───────────────────────────────┬──────────────────────┘
                       │                               │
                       ▼                               ▼
┌────────────────────────────────────────┐ ┌──────────────────────────────────┐
│           Data Access Layer            │ │      Global Utility & State      │
│        SettingsRepository.java         │ │        UserSession.java          │
│      JdbcSettingsRepository.java       │ │        ThemeManager.java         │
│  (Profile UPDATE, Pass UPDATE, Upsert) │ │      SceneNavigator.java         │
└──────────────────────┬─────────────────┘ └──────────────────────────────────┘
                       │
                       ▼
            MariaDB Database (applytrack_db)
       - users (full_name, email, password_hash)
       - user_preferences (theme, landing_page, analytics_range, reminders)
```

### Component Responsibilities:
1. **`SettingsController`**:
   - Coordinates the 4-tab user interface (`👤 My Profile`, `🔒 Security & Password`, `🎨 Preferences`, `ℹ️ Account Information`).
   - Executes asynchronous background loading via `javafx.concurrent.Task` to ensure zero GUI stutter.
   - Manages interactive password show/hide visibility toggles with bidirectional text field synchronization.
   - Dispatches validation alerts via dynamic inline banner notifications (`#bannerBox`).
   - Delegates theme adjustments in real-time to `ThemeManager`.
   - Triggers secure session logout and returns candidates to `Login.fxml`.
2. **`SettingsService`**:
   - Enforces business rules for profile editing (mandatory fields, RFC 5322 email syntax, duplicate email rejection).
   - Validates password changes: verifies current password with `BCrypt.checkpw`, requires $\ge 8$ character length, validates new/confirmation matching, and rejects reuse of the current password.
   - Hashes new credentials using `BCrypt.hashpw(plain, BCrypt.gensalt(10))`.
   - Coordinates default fallback preferences and synchronizes active user data with `UserSession`.
3. **`SettingsRepository` & `JdbcSettingsRepository`**:
   - Contract interface and JDBC implementation handling atomic MariaDB persistence.
   - Automatically ensures table existence (`CREATE TABLE IF NOT EXISTS user_preferences`).
   - Implements SQL `ON DUPLICATE KEY UPDATE` upsert logic for candidate preferences.
   - Guarantees strict multi-tenant scoping via parameterized SQL queries bound to `user_id = ?`.
4. **`ThemeManager`**:
   - Manages global UI appearance by dynamically injecting or removing `dark-theme.css` across JavaFX `Scene` instances.
   - Synchronizes visual themes across view transitions in `SceneNavigator`.
5. **`UserSession`**:
   - Thread-safe singleton holding the active authenticated `User` context and candidate identity.
   - Updated immediately when candidate profile information changes, preventing stale UI state.
   - Emptied completely upon candidate sign-out via `clearSession()`.

---

## 3. Database Schema & Migration Architecture

### 3.1 Migration Script (`database/migration_phase11_user_preferences.sql`)
Phase 11 introduces a dedicated, normalized relational table for candidate preferences, eliminating the need to clutter the primary `users` authentication table.

```sql
CREATE TABLE IF NOT EXISTS user_preferences (
    preference_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    theme VARCHAR(20) NOT NULL DEFAULT 'LIGHT',
    default_landing_page VARCHAR(30) NOT NULL DEFAULT 'DASHBOARD',
    preferred_analytics_range VARCHAR(30) NOT NULL DEFAULT 'ALL_TIME',
    reminder_upcoming_interviews BOOLEAN NOT NULL DEFAULT TRUE,
    reminder_application_deadlines BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_preferences_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 3.2 Relational Design Highlights
- **1-to-1 Association (`UNIQUE` constraint on `user_id`)**: Guarantees each candidate has at most one preferences profile.
- **Foreign Key Cascade (`ON DELETE CASCADE`)**: Automatically removes candidate preferences if the parent user record is removed, preventing orphaned records.
- **Automatic Timestamps**: Leverages MariaDB's `DEFAULT CURRENT_TIMESTAMP` and `ON UPDATE CURRENT_TIMESTAMP` for audit traceability.
- **Idempotent Upsert Logic**:
  `JdbcSettingsRepository` uses standard SQL upsert syntax:
  ```sql
  INSERT INTO user_preferences (
      user_id, theme, default_landing_page, preferred_analytics_range,
      reminder_upcoming_interviews, reminder_application_deadlines
  ) VALUES (?, ?, ?, ?, ?, ?)
  ON DUPLICATE KEY UPDATE
      theme = VALUES(theme),
      default_landing_page = VALUES(default_landing_page),
      preferred_analytics_range = VALUES(preferred_analytics_range),
      reminder_upcoming_interviews = VALUES(reminder_upcoming_interviews),
      reminder_application_deadlines = VALUES(reminder_application_deadlines),
      updated_at = CURRENT_TIMESTAMP
  ```

---

## 4. Security & Cryptographic Password Management

### 4.1 BCrypt Hashing Standard
ApplyTrack relies on `org.mindrot:jbcrypt` for salted password protection:
- **Salt Generation**: Cryptographically secure random salt generated with work factor $10$ (`BCrypt.gensalt(10)`).
- **One-Way Protection**: Passwords are never stored, logged, or serialized in plaintext.
- **Constant-Time Verification**: Uses `BCrypt.checkpw(plainPassword, storedHash)` which avoids timing attack vulnerabilities.

### 4.2 Password Change Verification Workflow
```
[User submits change request]
          │
          ▼
Is current, new, or confirm empty? ──(Yes)──> Throw ValidationException
          │ (No)
          ▼
Does BCrypt.checkpw(current, user.passwordHash) match? ──(No)──> Throw ValidationException ("Current password incorrect")
          │ (Yes)
          ▼
Does newPassword length >= 8? ──(No)──> Throw ValidationException ("Must be at least 8 characters")
          │ (Yes)
          ▼
Does newPassword.equals(confirmPassword)? ──(No)──> Throw ValidationException ("Passwords do not match")
          │ (Yes)
          ▼
Does BCrypt.checkpw(newPassword, user.passwordHash) match? ──(Yes)──> Throw ValidationException ("New password cannot be same as current")
          │ (No)
          ▼
Generate new BCrypt hash: BCrypt.hashpw(newPassword, BCrypt.gensalt(10))
          │
          ▼
Persist new hash to database: UPDATE users SET password_hash = ? WHERE user_id = ?
          │
          ▼
Update User instance in UserSession & clear password form fields
```

### 4.3 Password Visibility Toggles
The user interface features toggle buttons (`👁 Show` / `🔒 Hide`) for the Current Password, New Password, and Confirm Password inputs.
- Dual-field architecture pairs a `PasswordField` with a parallel `TextField`.
- When revealed, `TextField.setText(passwordField.getText())`, `textField.setVisible(true)`, and `passwordField.setVisible(false)`.
- When obscured, `passwordField.setText(textField.getText())`, `passwordField.setVisible(true)`, and `textField.setVisible(false)`.
- Input values remain strictly synchronized regardless of whether the user types while fields are hidden or shown.

---

## 5. Runtime Dynamic Theming Engine

ApplyTrack implements an instant, non-destructive runtime theming system without requiring window recreation or view reloads.

```
┌────────────────────────────────┐
│      AppTheme (Enum)           │
│  - LIGHT ("Light Mode")        │
│  - DARK  ("Dark Mode")         │
└───────────────┬────────────────┘
                │
                ▼
┌────────────────────────────────┐
│     ThemeManager (Singleton)   │
│  - activeTheme: AppTheme       │
│  - applyTheme(Scene, Theme)    │
│  - applyCurrentTheme(Scene)    │
└───────────────┬────────────────┘
                │  Dynamically manipulates scene.getStylesheets()
                ▼
┌────────────────────────────────┐
│   JavaFX Scene Graph           │
│   ├── base.css (Light default) │
│   └── dark-theme.css (Dark)    │
└────────────────────────────────┘
```

### 5.1 Theming Mechanics:
1. `base.css` contains all default dimensions, typography, layout rules, and light-theme color tokens (`#FFFFFF` backgrounds, `#0F172A` text, `#4F46E5` primary brand).
2. `dark-theme.css` overrides the CSS variables and control selectors when dark mode is enabled:
   - Root background: `#0F172A`
   - Content container & card fills: `#1E293B`
   - Border accents: `#334155`
   - Primary foreground text: `#F8FAFC`
   - Secondary muted text: `#94A3B8`
   - Inputs and combo boxes: `#1E293B` with `#475569` borders
   - TableView alternating rows: `#1E293B` / `#0F172A`
3. When the user changes the Theme ComboBox, `ThemeManager.applyTheme(scene, newTheme)` runs immediately.
4. If `DARK`, `dark-theme.css` is added to `scene.getStylesheets()`.
5. If `LIGHT`, `dark-theme.css` is removed from `scene.getStylesheets()`.
6. The preference is automatically persisted to `user_preferences` in MariaDB, ensuring that future logins automatically restore the candidate's chosen appearance.

---

## 6. User Profile & Live Session Synchronization

When a candidate updates their Full Name or Email:
1. **Validation**:
   - `fullName` must not be blank and must contain at least 2 characters.
   - `email` must conform to RFC 5322 regex: `^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$`.
2. **Uniqueness Verification**:
   - If the candidate alters their email address, `SettingsRepository.isEmailTakenByOtherUser(newEmail, userId)` verifies that no other candidate possesses that email.
3. **Database Write**:
   - `SettingsRepository.updateUserProfile(userId, fullName, email)` executes an atomic SQL update.
4. **Session Synchronization**:
   - `User updatedUser = new User(...)` is constructed.
   - `UserSession.getInstance().setCurrentUser(updatedUser)` is refreshed in-memory.
   - `MainLayoutController.getInstance().updateUserProfileUI(updatedUser)` updates the candidate's display name, email, and user badge in the persistent sidebar immediately without requiring a page refresh or application restart.

---

## 7. Session Lifecycle & Secure Logout

ApplyTrack enforces a strict zero-trust session lifecycle:
1. When the user clicks **Sign Out** from the Account tab:
   - A confirmation dialog verifies user intent (`Sign Out`, `Are you sure you want to log out?`).
   - `SettingsService.logout()` calls `UserSession.getInstance().clearSession()`.
   - All candidate identity and cached state are purged from JVM memory.
2. **Navigation Reset**:
   - `MainLayoutController.handleLogout()` redirects the stage directly to `Login.fxml`.
   - The root scene is switched, destroying the `MainLayout` container.
3. **Access Prevention**:
   - Any attempt to access protected controllers without an active session triggers an immediate redirect or exception.

---

## 8. Multi-Tenant Security & Tenant Isolation

Tenant isolation is built into the foundation of the settings module:
1. **Zero Client Trust**: All profile updates, password changes, and preference reads originate strictly from `UserSession.getInstance().getUserId()`. The client cannot manipulate another candidate's settings.
2. **Strict SQL Parameterization**:
   ```sql
   UPDATE users SET full_name = ?, email = ? WHERE user_id = ?;
   UPDATE users SET password_hash = ? WHERE user_id = ?;
   SELECT * FROM user_preferences WHERE user_id = ?;
   ```
3. **Database Isolation Verification**: Automated integration tests confirm that operations performed for Candidate A (e.g. `user_id = 1`) do not mutate, overwrite, or expose the preferences or password hash of Candidate B (e.g. `user_id = 2` or `999999`).

---

## 9. Automated Testing & Verification Strategy

The Settings & User Profile Management module is validated across **33 automated test cases**:

```
Phase 11 Test Suite (33 Tests, 0 Failures, 0 Errors)
├── UserPreferencesTest (4 Tests)
│   ├── Default values instantiation (Light theme, Dashboard, All-Time, true alerts)
│   ├── Custom setters and getters verification
│   ├── AppTheme string parsing and fallback resilience
│   └── Null safety on preference assignments
├── SettingsServiceTest (23 Tests)
│   ├── User profile fetching & session extraction
│   ├── Successful profile update with valid name & email
│   ├── Empty / blank name rejection with ValidationException
│   ├── Invalid email syntax rejection (RFC 5322 validation)
│   ├── Duplicate email rejection against existing database users
│   ├── Profile persistence & UserSession live synchronization
│   ├── Successful password change with valid current & new password
│   ├── Incorrect current password rejection
│   ├── Short new password (< 8 chars) rejection
│   ├── New password and confirmation mismatch rejection
│   ├── Password reuse rejection (new password identical to current)
│   ├── BCrypt 10-round salt verification on new password hash
│   ├── Successful authentication with new password via AuthService
│   ├── Rejection of old password authentication post-change
│   ├── Default preferences creation when record absent
│   ├── Successful preferences update & database upsert
│   ├── Theme switching persistence & ThemeManager sync
│   ├── Default landing page and analytics range configuration
│   ├── Notification reminder toggles persistence
│   ├── Multi-tenant isolation: user cannot alter another candidate's preferences
│   ├── Session logout and memory purging
│   ├── Account information retrieval and formatting
│   └── Null / unauthenticated session defensive rejection
└── JdbcSettingsRepositoryLiveTest (6 Tests)
    ├── Live MariaDB user profile retrieval for demo candidate
    ├── Atomic profile update with rollback verification
    ├── Live password hash mutation & BCrypt verification with rollback
    ├── Duplicate email collision detection across distinct user IDs
    ├── User preferences ON DUPLICATE KEY UPDATE upsert verification
    └── Multi-tenant isolation: non-existent user returns empty preferences
```

---

## 10. Conclusion

The Settings & User Profile Management Module provides ApplyTrack with an elegant, responsive, and cryptographically secure settings center. By combining BCrypt salted hashing, real-time JavaFX CSS theming, idempotent MariaDB preference upserts, and instant session synchronization, the module guarantees a frictionless, customizable, and enterprise-grade user experience.

