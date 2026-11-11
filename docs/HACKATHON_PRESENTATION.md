# ApplyTrack — Job Application Intelligence System
## Hackathon Pitch, Architectural Defense & Viva Presentation Deck

---

## Slide 1: Title & Vision
### **ApplyTrack: Job Application Intelligence System**
> *Transforming Job Search Chaos into Data-Driven Career Success.*

- **Category:** Desktop Productivity, Data Engineering & Career Intelligence
- **Platform:** Native Java 21 LTS Desktop Application with JavaFX & MariaDB
- **Presenter:** ApplyTrack Engineering Team

---

## Slide 2: The Problem Statement
### **The Chaotic Reality of Student & Graduate Job Hunting**
1. **Spreadsheet Entropy**: Job applicants track 30–100+ applications across fragmented spreadsheets without data integrity, automatic reminders, or status synchronization.
2. **Missed Deadlines & Forgotten Interviews**: Campus placement deadlines and multi-round interviews are missed due to disjointed calendars and lack of cutoff countdowns.
3. **Black-Hole Feedback**: Applicants submit dozens of resumes without visibility into where they are dropping out of the hiring funnel (e.g. resume screening vs technical rounds).
4. **Data Vulnerability**: Personal job notes, contact coordinates, and salary negotiations stored in plaintext files risk data loss and privacy exposure.

---

## Slide 3: The Proposed Solution
### **An Enterprise-Grade, Offline-Capable Desktop Intelligence System**
ApplyTrack combines the responsiveness of a native desktop application with the relational integrity of a modern MariaDB backend:
- **Centralized Application Hub**: Full lifecycle tracking from submission to final offer.
- **Multi-Round Pipeline Tracking**: Dedicated multi-stage tracking for coding tests, technical interviews, and HR rounds.
- **Real-Time Funnel Analytics**: Dynamic calculation of interview conversion rates and offer probabilities.
- **Executive Document Exporters**: Instant compilation of vector PDF and multi-tab Excel reports.
- **Cryptographic Security**: Salted BCrypt password protection and multi-tenant data isolation.

---

## Slide 4: Target Users
- **College Students & New Graduates**: Navigating high-volume campus placement drives and internship seasons.
- **Career Switchers & Software Engineers**: Managing multiple technical assessment pipelines and interview rounds simultaneously.
- **Academic Mentors & Placement Cells**: Reviewing student application histories and interview conversion rates via standardized PDF/Excel reports.

---

## Slide 5: Core System Capabilities & Modules
1. **Dynamic Intelligence Dashboard**: 5 live KPI cards, status distribution PieChart, monthly trend LineChart, deadline countdowns, and scheduled interview appointments.
2. **Job Application Management (CRUD)**: Multi-attribute tracking (role, mode, salary, status, dates) with instant search, status filtering, and chronological sorting.
3. **Multi-Round Interview Pipeline**: Track interview rounds per application with date-times, video meeting links, interviewer identities, and round outcomes (*Passed*, *Scheduled*, *Failed*).
4. **Recruiter Network Directory**: Corporate contact registry linked to hiring organizations with foreign key safe unlinking (`ON DELETE SET NULL`).
5. **Career Analytics Engine**: Real-time conversion funnel ratios (`distinct / total`), offer rates, company-wise volume distribution, and narrative takeaways.
6. **Reports & Data Export Center**: Native document compilation into vector **PDF** (Apache PDFBox 3.0) and styled **Excel** (Apache POI 5.2.5).
7. **Settings & Dynamic Theming**: Candidate profile management, BCrypt password changes, and runtime Light/Dark mode switching without stage reloads.

---

## Slide 6: Technology Stack
- **Language**: Java 21 LTS (Amazon Corretto)
- **GUI Framework**: JavaFX 21 (FXML + CSS + Hardware-Accelerated Charts)
- **Database**: MariaDB Server 12.3.2 (InnoDB Engine, utf8mb4)
- **Data Access**: JDBC Type 4 (MySQL Connector/J 8.3.0) with Parameterized PreparedStatements
- **Build & Dependency Automation**: Apache Maven 3.9+
- **Security & Cryptography**: BCrypt 0.4 (`org.mindrot:jbcrypt`)
- **Document Engines**: Apache PDFBox 3.0.2 (Vector PDF) & Apache POI 5.2.5 (OpenXML Spreadsheet)
- **Testing**: JUnit 5 (`org.junit.jupiter`) — **285 automated tests across 37 test classes**

---

## Slide 7: System Architecture
ApplyTrack adheres to a decoupled **Model-View-Controller (MVC)** and layered service pattern:

```
┌────────────────────────────────────────────────────────┐
│                   Presentation Layer                   │
│   JavaFX FXML Views (9 Views) + Custom CSS Themes     │
│   FXML Controllers (11 Controllers)                    │
└───────────────────────────┬────────────────────────────┘
                            │  Background Worker (javafx.concurrent.Task<T>)
                            ▼
┌────────────────────────────────────────────────────────┐
│                      Service Layer                     │
│   AuthService • ApplicationService • InterviewService  │
│   RecruiterService • DashboardService • Analytics      │
│   ReportService • SettingsService • PDF & Excel Engines│
└───────────────────────────┬────────────────────────────┘
                            │  Domain Entities & DTOs
                            ▼
┌────────────────────────────────────────────────────────┐
│                   Data Access Layer                    │
│   Generic CrudRepository<T, ID>                        │
│   8 Repository Interfaces + 8 JDBC Implementations     │
└───────────────────────────┬────────────────────────────┘
                            │  Parameterized SQL (PreparedStatement)
                            ▼
┌────────────────────────────────────────────────────────┐
│                 Database Storage Layer                 │
│   MariaDB Database (6 Tables, 6 Foreign Keys, Indexes) │
└────────────────────────────────────────────────────────┘
```

---

## Slide 8: Relational Database Design (3NF Normalization)
```
               ┌───────────────┐
               │     users     │ (1)
               └───────┬───────┘
                       │
       ┌───────────────┼───────────────┐
   1:1 │               │ 1:N           │ 1:1
       ▼               ▼               ▼
┌──────────────┐┌──────────────┐┌──────────────┐
│user_preferenc││ applications ││  companies   │ (1)
└──────────────┘└──────┬───────┘└──────┬───────┘
                       │               │
                   1:N │           1:N │
                       ▼               ▼
                ┌──────────────┐┌──────────────┐
                │  interviews  ││  recruiters  │ (N)
                └──────────────┘└──────────────┘
```

### Relational Integrity Highlights:
- **Cascade Deletes**: Deleting a candidate cascades to remove their applications, interviews, and preferences (`ON DELETE CASCADE`).
- **Pipeline Integrity**: Deleting an application automatically purges associated interview rounds.
- **Safe Recruiter Unlinking**: Deleting a recruiter sets `recruiter_id = NULL` (`ON DELETE SET NULL`) on linked applications, preserving application records intact.
- **Restricted Company Deletion**: A company with active applications cannot be accidentally deleted (`ON DELETE RESTRICT`).

---

## Slide 9: Java Syllabus Mastery (Academic Units 1, 2, and 3)

| Academic Unit | Core Concepts | Concrete Implementation in ApplyTrack |
| :--- | :--- | :--- |
| **Unit 1: Fundamentals** | OOP Paradigms, Primitive/Reference Types, Operators, Control Flow, Enhanced Switch, Date/Time API | `ChronoUnit.DAYS` deadline countdowns, division-by-zero safeguards, `switch` pattern matching for date filters and landing pages. |
| **Unit 2: Methods & Objects** | Encapsulation, Method Overloading, Static Factories, `StringBuilder`, Arrays & Collections, `this` Chaining | Overloaded constructors for test injection, `Comparable<T>` sorting on timelines, static `ThemeManager.getInstance()`. |
| **Unit 3: Advanced Java** | Inheritance (`BaseEntity`), Polymorphism (`CrudRepository`), Interfaces (`Initializable`, `Searchable`), Generics (`DataContainer<T>`), Exception Handling (`ValidationException`), Multithreading (`Task<T>`), JDBC (`PreparedStatement`) | Parameterized queries with `try-with-resources`, 10-round salted BCrypt hashing, background worker threads ensuring 60 FPS UI responsiveness. |

---

## Slide 10: User Experience & Application Workflow
1. **Secure Onboarding**: Registration with instant RFC 5322 email syntax validation and BCrypt password encryption.
2. **Cockpit Dashboard**: Dynamic KPI review, countdown warnings, and upcoming interview cards.
3. **Pipeline Management**: Search, filter, and schedule multi-round interview stages seamlessly.
4. **Data Exporting**: Export reports directly to PDF and Excel via native `FileChooser` dialogs.
5. **Runtime Theming**: Instant one-click toggle between Light Mode and Dark Mode.

---

## Slide 11: Security & Multi-Tenant Assurance
- **Zero Plaintext Storage**: Passwords hashed with 10 salt rounds; plaintext inputs zeroed from memory.
- **Constant-Time Verification**: `BCrypt.checkpw` guards against timing analysis.
- **Strict Tenant Scoping**: Every database operation queries `WHERE user_id = ?` using the verified `UserSession` singleton.
- **Zero SQL Injection**: 100% parameterized JDBC bindings prevent code injection.
- **Live Automated Verification**: Comprehensive integration tests verify that Candidate B can never view, alter, or delete Candidate A's records.

---

## Slide 12: Future Roadmap & Extensibility
While Phase 12 certifies the core system as 100% complete and self-contained, the decoupled architecture supports future enhancements:
1. **Calendar Synchronization**: Export upcoming interviews directly to `.ics` iCalendar format.
2. **Resume Attachment Vault**: Store and associate tailored PDF resumes per application.
3. **Offer Comparison Matrix**: Multi-attribute comparison table evaluating base pay, stock grants, benefits, and location costs.

---

## Slide 13: Conclusion
> **ApplyTrack represents a complete, professional, highly secure, and rigorously tested Java application.**

- **285 Tests Passing (100% Pass Rate)**
- **Fully Integrated MVC & Service Architecture**
- **Production-Ready Document Exporting & Cryptographic Security**
- **100% Aligned with Academic Computer Science Syllabus**

*Thank you. We are ready to demonstrate the live application and address questions!*

