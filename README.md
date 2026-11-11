# ApplyTrack – Job Application Intelligence System

> **A professional, desktop-based career management platform built with Core Java, JavaFX, and MySQL/MariaDB.**
> Designed for college hackathons, academic viva defenses, and real-world student job seekers to streamline application tracking, deadline monitoring, recruiter relationships, career analytics, and executive document exports.

---

## 1. Project Overview

**ApplyTrack** transforms the traditional chaotic spreadsheet into an intelligent, centralized desktop application. It empowers students and job applicants to:
- Securely register and sign in to their personal candidate workspace.
- Manage multi-stage job and internship applications.
- Track upcoming application deadlines and scheduled interview rounds.
- Maintain a directory of recruiters and corporate talent partners.
- Analyze interview and offer conversion funnels with real-time statistics.
- Export application records and executive summaries to vector PDF and multi-tab Excel (.xlsx) workbooks.
- Customize application preferences, runtime UI themes (Light/Dark), and profile credentials.

---

## 2. Technology Stack

- **Language:** Java 21 LTS (OpenJDK / Amazon Corretto)
- **GUI Framework:** JavaFX 21 (FXML + CSS + JavaFX Controls & Charts)
- **Database:** MySQL / MariaDB (Relational schema with InnoDB, Foreign Keys, and Indexes)
- **Database Connectivity:** JDBC Type 4 with `PreparedStatement` & Connection Factory pattern
- **Build & Dependency Tool:** Apache Maven 3.9+
- **Security & Cryptography:** BCrypt for salted password hashing (`org.mindrot:jbcrypt`)
- **Document Export Engines:** Apache PDFBox 3.0.2 (Vector PDF) & Apache POI 5.2.5 (OpenXML Excel)
- **Testing:** JUnit 5 (`org.junit.jupiter`) — **285 automated unit, live integration, and E2E tests (37 test classes, 100% pass rate)**

---

## 3. Project Architecture

ApplyTrack follows a decoupled MVC (Model-View-Controller) and layered service architecture:

```
ApplyTrack/
├── pom.xml                                  # Maven build and dependency descriptor
├── README.md                                # Master project documentation
├── JAVA_SYLLABUS_MAPPING.md                 # Complete Unit 1, Unit 2, Unit 3 syllabus mapping
├── .gitignore                               # Git ignore rules (protects credentials)
├── .env.example                             # Environment variables template
├── db.properties.example                    # Local database configuration template
├── db.properties                            # (Local) Active credentials (git-ignored)
├── run.bat                                  # 1-Click launcher for JavaFX UI
├── build.bat                                # 1-Click build & test script
├── database/
│   ├── schema.sql                           # Complete relational schema (6 tables + FK constraints)
│   ├── sample_data.sql                      # Realistic seed data for testing
│   └── migration_phase11_user_preferences.sql # User preferences table migration (Phase 11)
├── docs/
│   ├── DATABASE_DESIGN.md                   # Full ER diagram & table specifications (Phase 2)
│   ├── OOP_ARCHITECTURE.md                 # UML class diagram & OOP design guide (Phase 3)
│   ├── AUTHENTICATION_DESIGN.md             # Sequence diagram & security guide (Phase 4)
│   ├── DASHBOARD_DESIGN.md                  # Dynamic Dashboard architecture & analytics queries (Phase 5)
│   ├── APPLICATION_MANAGEMENT.md            # Job Application CRUD, search, filter & sorting guide (Phase 6)
│   ├── INTERVIEW_MANAGEMENT.md              # Interview Pipeline & multi-round evaluation guide (Phase 7)
│   ├── RECRUITER_MANAGEMENT.md              # Recruiter Directory & relationship management guide (Phase 8)
│   ├── ANALYTICS_DESIGN.md                  # Advanced Career Analytics, funnels & trends guide (Phase 9)
│   ├── REPORTS_AND_EXPORT_DESIGN.md         # PDFBox & POI document export engines guide (Phase 10)
│   ├── SETTINGS_AND_PROFILE_DESIGN.md       # Settings, BCrypt security & Theming Engine guide (Phase 11)
│   ├── FINAL_PROJECT_AUDIT.md               # Complete architectural & module integration audit (Phase 12)
│   ├── FINAL_TEST_REPORT.md                 # 285 automated tests report & multi-tenant security verification (Phase 12)
│   ├── DEMO_GUIDE.md                        # 3-to-5 minute live demonstration walkthrough script (Phase 12)
│   └── HACKATHON_PRESENTATION.md            # Presentation slides deck & architectural defense (Phase 12)
└── src/
    ├── main/
    │   ├── java/com/applytrack/
    │   │   ├── Launcher.java                # Standard JVM entry point
    │   │   ├── MainApp.java                 # JavaFX Application lifecycle (starts at Login)
    │   │   ├── controller/                  # FXML UI Controllers
    │   │   │   ├── LoginController.java     # Authentication form & show/hide password (Phase 4)
    │   │   │   ├── RegisterController.java  # Candidate registration form (Phase 4)
    │   │   │   ├── MainLayoutController.java# Sidebar navigation, preference routing, live profile & logout
    │   │   │   ├── DashboardController.java # Dynamic 5 KPI cards, PieChart, LineChart & async refresh (Phase 5)
    │   │   │   ├── ApplicationsController.java # Application TableView, search, filter, sort & delete (Phase 6)
    │   │   │   ├── ApplicationDialogController.java # Modal dialog for Add & Edit application with recruiter selector
    │   │   │   ├── InterviewsController.java# Interview pipeline TableView, search, filter & delete (Phase 7)
    │   │   │   ├── InterviewDialogController.java # Modal dialog for Schedule & Edit interview round (Phase 7)
    │   │   │   ├── RecruitersController.java# Recruiter directory TableView, search, filter & details (Phase 8)
    │   │   │   ├── RecruiterDialogController.java # Modal dialog for Add & Edit recruiter contact (Phase 8)
    │   │   │   ├── AnalyticsController.java # Career Analytics, PieCharts, LineCharts, BarChart & insights (Phase 9)
    │   │   │   ├── ReportsController.java   # Reports Center, PDF/Excel exporters & preview cards (Phase 10)
    │   │   │   └── SettingsController.java  # Tabbed Profile, BCrypt Password, Theme & Account Center (Phase 11)
    │   │   ├── database/                    # JDBC Connectivity & Configuration (Phase 2)
    │   │   │   ├── DatabaseConfig.java      # Credential loader with password masking
    │   │   │   ├── DatabaseConnection.java  # Connection factory & error diagnostics
    │   │   │   ├── DatabaseHelper.java      # Metadata inspector & table validator
    │   │   │   └── DatabaseConnectionTest.java # Standalone terminal connectivity verification
    │   │   ├── exception/                   # Custom domain exceptions (Phase 4 & 6)
    │   │   │   ├── AuthenticationException.java
    │   │   │   └── ValidationException.java
    │   │   ├── model/                       # Domain Entities & OOP Hierarchy (Phase 3, 5, 9 & 11)
    │   │   │   ├── base/
    │   │   │   │   └── BaseEntity.java      # Abstract base class (id, audit timestamps)
    │   │   │   ├── dto/                     # Data Transfer Objects (Phase 5, 9 & 10)
    │   │   │   │   ├── DashboardSummary.java# Aggregated metrics, charts & activity container
    │   │   │   │   ├── DeadlineSummaryItem.java # Cutoff urgency & remaining days calculator
    │   │   │   │   ├── InterviewSummaryItem.java# Scheduled rounds & proctor link DTO
    │   │   │   │   ├── AnalyticsSummary.java# Master career analytics & conversion rates DTO (Phase 9)
    │   │   │   │   ├── MonthlyApplicationData.java # Chronological multi-year submission volume (Phase 9)
    │   │   │   │   ├── CompanyApplicationData.java # Company-wise application distribution (Phase 9)
    │   │   │   │   ├── StatusDistribution.java # Status percentage & color pill breakdown (Phase 9)
    │   │   │   │   ├── InterviewAnalytics.java # Interview outcome & multi-round analytics (Phase 9)
    │   │   │   │   ├── KeyInsights.java     # Narrative takeaways, top company & peak month (Phase 9)
    │   │   │   │   ├── ReportFilter.java    # Date range and custom boundaries filter DTO (Phase 10)
    │   │   │   │   ├── ReportPayload.java   # Master multi-entity report data container (Phase 10)
    │   │   │   │   ├── ApplicationReportItem.java # Application tabular DTO with formatted currency (Phase 10)
    │   │   │   │   ├── InterviewReportItem.java # Interview rounds report DTO (Phase 10)
    │   │   │   │   ├── RecruiterReportItem.java # Recruiter directory report DTO (Phase 10)
    │   │   │   │   └── DateWiseActivityItem.java # Activity timeline DTO with Comparable sorting (Phase 10)
    │   │   │   ├── enums/
    │   │   │   │   ├── ApplicationStatus.java
    │   │   │   │   ├── JobType.java
    │   │   │   │   ├── InterviewMode.java
    │   │   │   │   ├── InterviewOutcome.java
    │   │   │   │   ├── DateRangeFilter.java # Predefined temporal filters (Phase 9)
    │   │   │   │   ├── ReportType.java      # 6 supported reporting types & filename prefixes (Phase 10)
    │   │   │   │   └── AppTheme.java        # Light and Dark UI theme options (Phase 11)
    │   │   │   ├── interfaces/
    │   │   │   │   └── Searchable.java
    │   │   │   ├── User.java                # User candidate entity (password protected)
    │   │   │   ├── Company.java
    │   │   │   ├── Recruiter.java
    │   │   │   ├── Application.java
    │   │   │   ├── Interview.java
    │   │   │   └── UserPreferences.java     # User UI preferences & reminders entity (Phase 11)
    │   │   ├── repository/                  # Data Access Layer (Phase 4-11)
    │   │   │   ├── CrudRepository.java      # Generic repository interface <T, ID>
    │   │   │   ├── UserRepository.java      # User queries contract
    │   │   │   ├── DashboardRepository.java # Aggregated user analytics contract (Phase 5)
    │   │   │   ├── ApplicationRepository.java # Application CRUD & filtering contract (Phase 6)
    │   │   │   ├── CompanyRepository.java   # Company profiles & lookups contract (Phase 6)
    │   │   │   ├── InterviewRepository.java # Interview rounds & pipeline contract (Phase 7)
    │   │   │   ├── RecruiterRepository.java # Recruiter directory & relationships contract (Phase 8)
    │   │   │   ├── AnalyticsRepository.java # Advanced career analytics & conversion contract (Phase 9)
    │   │   │   ├── ReportRepository.java    # Multi-attribute reporting contract (Phase 10)
    │   │   │   ├── SettingsRepository.java  # User profile & preferences contract (Phase 11)
    │   │   │   └── impl/
    │   │   │       ├── JdbcUserRepository.java # Parameterized JDBC user repository
    │   │   │       ├── JdbcDashboardRepository.java # Parameterized JDBC dashboard queries (Phase 5)
    │   │   │       ├── JdbcApplicationRepository.java # Parameterized JDBC application CRUD (user_id = ?) (Phase 6)
    │   │   │       ├── JdbcCompanyRepository.java # Parameterized JDBC company queries (Phase 6)
    │   │   │       ├── JdbcInterviewRepository.java # Parameterized JDBC interview queries (user_id = ?) (Phase 7)
    │   │   │       ├── JdbcRecruiterRepository.java # Parameterized JDBC recruiter queries (Phase 8)
    │   │   │       ├── JdbcAnalyticsRepository.java # Parameterized SQL aggregation analytics queries (Phase 9)
    │   │   │       ├── JdbcReportRepository.java # Parameterized reporting queries with multi-table JOINs (Phase 10)
    │   │   │       └── JdbcSettingsRepository.java # Parameterized preferences upsert & profile update (Phase 11)
    │   │   ├── service/                     # Business Logic & Document Generators (Phase 4-11)
    │   │   │   ├── AuthService.java         # BCrypt hashing, login/register logic
    │   │   │   ├── DashboardService.java    # Metrics calculation, month formatting & aggregation (Phase 5)
    │   │   │   ├── ApplicationService.java  # CRUD validations, ownership checks & company resolution (Phase 6)
    │   │   │   ├── InterviewService.java    # Scheduling, multi-round logic & ownership security (Phase 7)
    │   │   │   ├── RecruiterService.java    # Recruiter directory, validations & contact management (Phase 8)
    │   │   │   ├── AnalyticsService.java    # Conversion rates, date range filtering & insights (Phase 9)
    │   │   │   ├── ReportService.java       # Report compilation, KPI synthesis & format routing (Phase 10)
    │   │   │   ├── SettingsService.java     # Profile validation, BCrypt password change & preferences (Phase 11)
    │   │   │   └── export/
    │   │   │       ├── PdfExportService.java # Apache PDFBox 3.0 vector PDF export engine (Phase 10)
    │   │   │       └── ExcelExportService.java # Apache POI 5.2.5 multi-sheet Excel engine (Phase 10)
    │   │   ├── session/                     # Session Management (Phase 4)
    │   │   │   └── UserSession.java         # Singleton holding authenticated user
    │   │   └── util/                        # Reusable Generics, SceneNavigator, Theming & Helpers
    │   │       ├── SceneNavigator.java      # Stage scene switcher with theme preservation
    │   │       ├── ThemeManager.java        # Runtime CSS theme switcher (Light / Dark) (Phase 11)
    │   │       ├── DataContainer.java       # Generic container class
    │   │       ├── ApplicationCollectionUtil.java # ArrayList, HashMap, HashSet, PriorityQueue
    │   │       └── ValidationUtil.java      # Email, URL, phone, salary, deadline validations
    │   └── resources/
    │       ├── com/applytrack/
    │       │   ├── view/                    # FXML view templates
    │       │   │   ├── Login.fxml           # Modern SaaS sign-in card (Phase 4)
    │       │   │   ├── Register.fxml        # Candidate registration card (Phase 4)
    │       │   │   ├── MainLayout.fxml      # Dashboard layout (Sidebar with profile & logout)
    │       │   │   ├── Dashboard.fxml       # Dynamic dashboard with PieChart, LineChart & lists (Phase 5)
    │       │   │   ├── Applications.fxml    # TableView, instant search, status filter & action bar (Phase 6)
    │       │   │   ├── ApplicationDialog.fxml # Modal dialog form for Add & Edit application (Phase 6 & 8)
    │       │   │   ├── Interviews.fxml      # Interview pipeline TableView, search, filter & outcomes (Phase 7)
    │       │   │   ├── InterviewDialog.fxml # Modal dialog for Schedule & Edit interview round (Phase 7)
    │       │   │   ├── Recruiters.fxml      # Recruiter TableView, search, company filter & badges (Phase 8)
    │       │   │   ├── RecruiterDialog.fxml # Modal dialog form for Add & Edit recruiter (Phase 8)
    │       │   │   ├── Analytics.fxml       # Advanced Analytics, PieCharts, LineCharts, BarChart & KPI cards (Phase 9)
    │       │   │   ├── Reports.fxml         # Reports Center, PDF/Excel export buttons & live preview cards (Phase 10)
    │       │   │   └── Settings.fxml        # Tabbed Settings & Profile center (Phase 11)
    │       │   └── css/
    │       │       ├── styles.css           # Modern SaaS CSS with TableView & badge styling
    │       │       └── dark-theme.css       # Complete dark mode stylesheet for cards, tables & forms (Phase 11)
    │       └── config/
    │           └── db.properties.example
    └── test/java/com/applytrack/
        ├── MainAppTest.java                 # FXML, CSS, and Stage inheritance tests
        ├── database/                        # Database unit & integration tests (Phase 2)
        │   ├── DatabaseConfigTest.java
        │   ├── DatabaseSchemaValidationTest.java
        │   └── DatabaseConnectionLiveTest.java
        ├── e2e/                             # End-to-End & Multi-Tenant Lifecycle tests (Phase 12)
        │   └── EndToEndUserJourneyLiveTest.java # 20-step new-user lifecycle & multi-tenant isolation tests
        ├── model/                           # Model & OOP architecture tests (Phase 3, 5 & 11)
        │   ├── UserTest.java
        │   ├── CompanyTest.java
        │   ├── RecruiterTest.java
        │   ├── ApplicationTest.java
        │   ├── InterviewTest.java
        │   ├── UserPreferencesTest.java     # UserPreferences defaults & AppTheme parsing tests (Phase 11)
        │   ├── EnumsTest.java
        │   └── dto/
        │       └── DashboardSummaryTest.java# DTO, Urgency & Comparable sorting tests (Phase 5)
        ├── repository/                      # Repository integration tests (Phase 4-11)
        │   ├── JdbcUserRepositoryLiveTest.java
        │   ├── JdbcDashboardRepositoryLiveTest.java # Live MariaDB user-specific queries (Phase 5)
        │   ├── JdbcApplicationRepositoryLiveTest.java # Live MariaDB application CRUD tests (Phase 6)
        │   ├── JdbcCompanyRepositoryLiveTest.java # Live MariaDB company tests (Phase 6)
        │   ├── JdbcInterviewRepositoryLiveTest.java # Live MariaDB interview pipeline & multi-round tests (Phase 7)
        │   ├── JdbcRecruiterRepositoryLiveTest.java # Live MariaDB recruiter tests & SET NULL cascade (Phase 8)
        │   ├── JdbcAnalyticsRepositoryLiveTest.java # Live MariaDB analytics aggregation & isolation tests (Phase 9)
        │   ├── JdbcReportRepositoryLiveTest.java # Live MariaDB reporting queries & isolation tests (Phase 10)
        │   └── JdbcSettingsRepositoryLiveTest.java # Live MariaDB profile & preferences upsert tests (Phase 11)
        ├── service/                         # Service logic tests (Phase 4-11)
        │   ├── AuthServiceTest.java
        │   ├── DashboardServiceTest.java    # Calculation & formatting unit tests (Phase 5)
        │   ├── DashboardRefreshTest.java    # Dynamic refresh & cache invalidation tests (Phase 5)
        │   ├── ApplicationServiceTest.java  # CRUD validations & ownership isolation unit tests (Phase 6)
        │   ├── InterviewServiceTest.java    # Scheduling, multi-round validations & ownership unit tests (Phase 7)
        │   ├── RecruiterServiceTest.java    # Recruiter CRUD, validations & security unit tests (Phase 8)
        │   ├── AnalyticsServiceTest.java    # Conversion rates, date range filtering & insights unit tests (Phase 9)
        │   ├── ReportServiceTest.java       # Report compilation, KPI synthesis & routing tests (Phase 10)
        │   ├── SettingsServiceTest.java     # Profile validation, BCrypt password change & preferences tests (Phase 11)
        │   └── export/
        │       ├── PdfExportServiceTest.java # Apache PDFBox 3.0 vector PDF export tests (Phase 10)
        │       └── ExcelExportServiceTest.java # Apache POI 5.2.5 multi-sheet Excel export tests (Phase 10)
        ├── session/                         # Session tests (Phase 4)
        │   └── UserSessionTest.java
        └── util/                            # Utility & Collection tests (Phase 3)
            ├── DataContainerTest.java
            ├── ValidationUtilTest.java
            └── ApplicationCollectionUtilTest.java
```

---

## 4. Academic Syllabus Integration (College Syllabus: Units 1-3)

- **Unit 1 (Java Fundamentals):** Strong OOP paradigms, primitives vs references, type conversions, conditional validation trees, loops, date calculations (`ChronoUnit.DAYS`, `LocalDate`, `DateTimeFormatter`), and enhanced switch pattern matching.
- **Unit 2 (Methods, Strings, Arrays, Classes & Objects):** Method overloading, static utility methods (`ThemeManager.getInstance()`, `AppTheme.fromString()`), RFC 5322 regex validation, searching/sorting algorithms (`Comparable`), and explicit `this` chaining.
- **Unit 3 (OOP & Advanced Java):** Inheritance (`BaseEntity`, custom `Exception`s), Polymorphism with generic interfaces (`CrudRepository<T, ID>`, `SettingsRepository`, `ApplicationRepository`), custom exception handling (`ValidationException`), Multithreading (`javafx.concurrent.Task`), Dynamic GUI Theming (`Scene.getStylesheets()`), and Password Cryptography (10-round salted BCrypt).

*For an exhaustive breakdown of topics mapped to specific methods and classes, see [`JAVA_SYLLABUS_MAPPING.md`](JAVA_SYLLABUS_MAPPING.md).*

---

## 5. How to Build and Run the Application

### 1-Click Windows Launch:
- Double click **`run.bat`** to start the JavaFX desktop application.
- Double click **`build.bat`** to clean, compile, and run all 285 automated unit tests.

### Pre-Configured Demo Account:
For instant hackathon demonstration or testing, click **"⚡ Fill Demo Account"** on the Login screen, or enter:
- **Email:** `alex.mercer@applytrack.io`
- **Password:** `password123`

### Command Line:
```bash
# Run all 285 automated JUnit 5 tests
mvn clean test

# Package standalone application JAR
mvn clean package

# Launch the JavaFX Application
mvn javafx:run
```

---

## 6. Project Roadmap

- [x] **Phase 0 & 1:** Environment verification, Maven JavaFX architecture, modern UI layout, and initial documentation.
- [x] **Phase 2:** Database Design, Relational Schema, Seed Data, JDBC Connection Factory, Password Security, and Connectivity Test.
- [x] **Phase 3:** Java Domain Models, OOP Inheritance Hierarchy (`BaseEntity`), Enums, `Searchable` Interface, Generic `DataContainer<T>`, Validation Utility, and Collections (`ArrayList`, `HashMap`, `HashSet`, `PriorityQueue`).
- [x] **Phase 4:** Login and Registration, BCrypt password hashing, `UserSession` singleton, `UserRepository` (CRUD), and JavaFX authentication screens.
- [x] **Phase 5:** Dynamic Dashboard integration with live database metrics, JavaFX PieChart & LineChart, upcoming deadlines & interviews panels, and user-specific security (`user_id = ?`).
- [x] **Phase 6:** Job Application Management (Full CRUD, Search, Filter, Sort, TableView, Modal Dialogs & Ownership Security).
- [x] **Phase 7:** Interview Pipeline Management (Multi-round scheduling, video call links, interviewer tracking, outcome status, TableView & Ownership Isolation).
- [x] **Phase 8:** Recruiter Management Module (Recruiter Directory, Company Association, Safe SET NULL FK Unlinking, Multi-tenant Isolation & TableView).
- [x] **Phase 9:** Career Analytics & Conversion Rate Funnels (Interactive PieCharts, LineCharts, BarChart, Date Range Filters & Key Insights).
- [x] **Phase 10:** Reports & Data Export Module (Apache PDFBox 3.0 vector PDF reports, Apache POI 5.2.5 multi-sheet Excel workbooks, Date filtering, FileChooser & Multi-tenant security).
- [x] **Phase 11:** Settings & User Profile Management (User Profile updates, BCrypt password change, Application Preferences, Runtime Dynamic Theming, and Session Lifecycle).
- [x] **Phase 12:** Final Integration, Testing, Hackathon Presentation & Defense Deliverables (End-to-End User Journey test, 285 passing tests, Demo Guide, Final Audit & Slide Deck).
