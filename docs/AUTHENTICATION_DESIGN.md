# ApplyTrack – Authentication & Security Architecture

> **Designed for College Hackathons & Second-Year Computer Science Curriculum.**  
> Explains the secure authentication flow, layered responsibilities, BCrypt password hashing, session tracking, and security best practices.

---

## 1. Authentication Architecture & Sequence Diagram

The following Mermaid sequence diagram illustrates the complete user lifecycle from registration to authenticated dashboard access and logout:

```mermaid
sequenceDiagram
    autonumber
    actor Candidate as Candidate / User
    participant UI as Login / Register View (JavaFX)
    participant Auth as AuthService
    participant Repo as UserRepository (JDBC)
    participant DB as MariaDB (applytrack_db)
    participant Session as UserSession (Singleton)
    participant Dashboard as MainLayoutController

    %% REGISTRATION FLOW
    rect rgb(240, 249, 255)
    note right of Candidate: Registration Flow
    Candidate->>UI: Enters Name, Email, Password, Confirm Password
    UI->>Auth: register(fullName, email, password, confirmPassword)
    Auth->>Auth: Validate fields & minimum length (>= 8 chars)
    Auth->>Repo: existsByEmail(email)
    Repo->>DB: SELECT 1 FROM users WHERE email = ?
    DB-->>Repo: false (Email available)
    Auth->>Auth: Hash password with BCrypt (Salt factor 10)
    Auth->>Repo: save(User with hash)
    Repo->>DB: INSERT INTO users VALUES (?, ?, ?)
    DB-->>Repo: Returns generated user_id
    Repo-->>Auth: Returns persisted User
    Auth-->>UI: Registration Successful
    UI-->>Candidate: Redirect to Login with pre-filled email
    end

    %% LOGIN FLOW
    rect rgb(245, 243, 255)
    note right of Candidate: Login Authentication Flow
    Candidate->>UI: Enters Email and Password
    UI->>Auth: login(email, password)
    Auth->>Repo: findByEmail(email)
    Repo->>DB: SELECT * FROM users WHERE email = ?
    DB-->>Repo: User record with password_hash
    Auth->>Auth: BCrypt.checkpw(enteredPassword, storedHash)
    alt Credentials Valid
        Auth->>Session: setCurrentUser(user)
        Auth-->>UI: Authentication Success
        UI->>Dashboard: SceneNavigator.showMainDashboard(stage)
        Dashboard->>Session: getCurrentUser()
        Dashboard-->>Candidate: Dashboard rendered with candidate profile
    else Invalid Credentials
        Auth-->>UI: throws AuthenticationException("Invalid email or password.")
        UI-->>Candidate: Displays safe error message (no user enumeration)
    end
    end

    %% LOGOUT FLOW
    rect rgb(254, 242, 242)
    note right of Candidate: Logout Flow
    Candidate->>Dashboard: Clicks "Sign Out"
    Dashboard->>Session: clearSession()
    Dashboard->>UI: SceneNavigator.showLoginScreen(stage)
    UI-->>Candidate: Displays "You have been logged out successfully."
    end
```

---

## 2. Layered Responsibilities

| Layer | Component | Core Responsibility |
| :--- | :--- | :--- |
| **Presentation (View / Controller)** | `LoginController`, `RegisterController` | Captures UI inputs, provides show/hide password toggle, displays error/success feedback, and triggers scene changes. |
| **Business Logic (Service)** | `AuthService` | Enforces domain rules (minimum 8 characters, matching confirmation), hashes passwords via BCrypt, and verifies credentials. |
| **Persistence (Repository)** | `UserRepository`, `JdbcUserRepository` | Prepares parameterized SQL statements (`PreparedStatement`), handles `SQLException`, and retrieves auto-generated keys. |
| **Session Management** | `UserSession` | Thread-safe Singleton holding the authenticated `User` object for downstream modules. |
| **Domain Model** | `User` | Encapsulates candidate identity without exposing the password hash in logs (`[PROTECTED]`). |

---

## 3. BCrypt Password Hashing Explained

In basic student projects, passwords are often saved in plain text or using outdated algorithms like MD5 or SHA-1, which are vulnerable to lookup tables and rainbow table attacks.

ApplyTrack uses **BCrypt** (`org.mindrot:jbcrypt`):
1. **Adaptive Work Factor (Salt Factor):** BCrypt incorporates a random 128-bit salt and an exponential cost parameter (set to factor 10, performing $2^{10} = 1024$ key expansion rounds).
2. **One-Way Salting:** Each hash has a unique salt embedded:
   ```
   $2a$10$n3HO5YCqxZO.xV/6h6bOW.fWUBql4Qdn0bJv9nR2z5b0LtyrMEtbK
   \__/ \/ \____________________/\_____________________________/
    |    |            |                            |
  Prefix Cost     22-char Salt              31-char Hash
   ```
3. **Verification Without Decryption:** `BCrypt.checkpw(plainText, storedHash)` extracts the salt from the stored hash, hashes the candidate's plain-text input using that exact salt, and compares the resulting hashes.

---

## 4. Key Security Practices Implemented

1. **Defense Against SQL Injection:** All database operations strictly use `java.sql.PreparedStatement` with bind parameters (`?`). No string concatenation is used in queries.
2. **Prevention of Username Enumeration:** During login failure, `AuthService` throws a uniform message: `"Invalid email or password."`. It never reveals whether the email exists.
3. **Password Masking in Logs:** `User.toString()` and `DatabaseConfig.toString()` conceal sensitive strings as `[PROTECTED]` and `******`.
4. **Session Cleansing on Logout:** Calling `UserSession.clearSession()` zeroes out user references, preventing session lingering.
5. **No Password Stored in Memory:** `UserSession` maintains the entity reference with the hash, never storing plain-text passwords.

