# ApplyTrack — Reports & Data Export Module Design Document
## Module 6 Architecture, Export Engines, Data Pipelines & Security

---

## 1. Executive Summary

The **Reports & Data Export Module** (Module 6) provides ApplyTrack users with an enterprise-grade reporting and document generation facility. It transforms normalized MariaDB relational records into executive-ready **Portable Document Format (PDF)** summaries and **Microsoft Excel (.xlsx)** workbooks with professional formatting, typography, summary KPI cards, and multi-tenant security guarantees.

All export operations execute asynchronously on background threads via `javafx.concurrent.Task`, ensuring smooth, non-blocking UI interactions, live progress indication, and user-directed destination file saving via standard JavaFX `FileChooser` dialogs.

---

## 2. Layered Architecture & Data Flow

ApplyTrack follows a strict layered architecture adhering to the **Separation of Concerns** principle:

```
┌─────────────────────────────────────────────────────────────┐
│                       Presentation Layer                    │
│      Reports.fxml  <───>  ReportsController.java            │
│  (UI Controls, Live KPI Cards, DatePickers, FileChooser)    │
└──────────────────────────────┬──────────────────────────────┘
                               │  Background Thread (Task<T>)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                         Service Layer                       │
│                     ReportService.java                      │
│   (Report Payload Compilation, Date Validation, Routing)    │
└──────────────┬──────────────────────────────┬───────────────┘
               │                              │
               ▼                              ▼
┌──────────────────────────────┐ ┌────────────────────────────┐
│      Data Access Layer       │ │       Export Engines       │
│    ReportRepository.java     │ │  PdfExportService.java     │
│   JdbcReportRepository.java  │ │    (Apache PDFBox 3.0)     │
│ (Multi-Table JOINs, user_id) │ │  ExcelExportService.java   │
│                              │ │     (Apache POI 5.2.5)     │
└──────────────┬───────────────┘ └────────────┬───────────────┘
               │                              │
               ▼                              ▼
    MariaDB Database                 Generated Documents
   (applytrack_db)                  (.pdf and .xlsx files)
```

### Component Responsibilities:
1. **`ReportsController`**: Manages UI state, handles user event triggers, executes date range validation, controls the loading overlay, opens `FileChooser` dialogs, and coordinates background worker threads.
2. **`ReportService`**: Acts as the central coordinator. Prepares and aggregates the `ReportPayload`, synthesizes high-level summary KPIs, evaluates active status distributions, and routes requests to either `PdfExportService` or `ExcelExportService`.
3. **`JdbcReportRepository`**: Executes parameterized JDBC SQL queries strictly filtered by `user_id = ?` and optional `startDate` / `endDate` boundaries across 4 core entity domains.
4. **`PdfExportService`**: Low-level vector PDF document renderer built on **Apache PDFBox 3.0.2**. Produces paginated, branded, vector-quality PDF documents with running headers, alternating row fills, and confidential footers.
5. **`ExcelExportService`**: Native spreadsheet generation engine built on **Apache POI 5.2.5**. Produces multi-tab `.xlsx` workbooks with styled header rows (`#0F172A`), frozen top panes, explicit cell formatting (`$#,##0.00`, `0.0%`, `yyyy-mm-dd`), and column auto-sizing.

---

## 3. Supported Report Types

| ID | Enum Constant | Display Name | Content & Purpose | Default Filename Pattern |
|---|---|---|---|---|
| 1 | `JOB_APPLICATIONS` | Job Application Report | Detailed inventory of all applications, companies, roles, work modes, applied dates, deadlines, statuses, salaries, and recruiter contacts. | `ApplyTrack_Job_Applications_YYYY-MM-DD` |
| 2 | `INTERVIEW_HISTORY` | Interview History Report | Comprehensive log of all interview rounds, company names, schedules, interview modes, outcomes (Passed/Scheduled/Failed), and interviewers. | `ApplyTrack_Interview_History_YYYY-MM-DD` |
| 3 | `RECRUITER_DIRECTORY` | Recruiter Directory Report | Talent acquisition network directory including recruiter names, corporate affiliations, contact coordinates, LinkedIn profiles, and linked application counts. | `ApplyTrack_Recruiter_Directory_YYYY-MM-DD` |
| 4 | `APPLICATION_STATUS` | Application Status Report | Distribution and lifecycle breakdown of applications across all active and terminal statuses with percentage shares. | `ApplyTrack_Status_Report_YYYY-MM-DD` |
| 5 | `ANALYTICS_SUMMARY` | Analytics Summary Report | Executive intelligence overview synthesizing conversion rates, interview velocity, offer rates, status distributions, and recent applications. | `ApplyTrack_Analytics_Summary_YYYY-MM-DD` |
| 6 | `DATE_WISE_ACTIVITY` | Date-wise Activity Report | Chronological audit trail combining application submissions, interview appointments, and round outcomes in descending order. | `ApplyTrack_Activity_Log_YYYY-MM-DD` |

---

## 4. Date Filtering & Custom Range Logic

The system provides flexible date scoping via `ReportFilter`:
- **Predefined Scopes**:
  - `ALL_TIME`: No lower bound (`startDate = null`), encompasses candidate history.
  - `LAST_30_DAYS`: Lower bound set to `LocalDate.now().minusDays(30)`.
  - `LAST_3_MONTHS`: Lower bound set to `LocalDate.now().minusMonths(3)`.
  - `LAST_6_MONTHS`: Lower bound set to `LocalDate.now().minusMonths(6)`.
  - `LAST_12_MONTHS`: Lower bound set to `LocalDate.now().minusYears(1)`.
- **Custom Date Range**:
  - Bound by `customStartDate` and `customEndDate` picked via JavaFX `DatePicker`.
  - Enforces domain rule: `customStartDate <= customEndDate`.
  - Rejection with `ValidationException` if `customStartDate.isAfter(customEndDate)`.

---

## 5. PDF Generation Architecture (Apache PDFBox 3.0)

### Design & Layout Features:
- **Geometry**: ISO A4 Page (`595.27 x 841.89` pt) with 36 pt (0.5 inch) outer margins for maximum usable printable width.
- **Color Palette (AWT Color Integration)**:
  - Header Background: Deep Slate `#0F172A` (`new Color(15, 23, 42)`)
  - Accent / Primary: Brand Indigo `#4F46E5` (`new Color(79, 70, 229)`)
  - KPI Box Fill: Cool Gray `#F1F5F9` (`new Color(241, 245, 249)`)
  - Zebra Row Striping: Pure White `#FFFFFF` and Subtle Gray `#F8FAFC` (`new Color(248, 250, 252)`)
  - Border Lines: Slate Border `#CBD5E1` (`new Color(203, 213, 225)`)
- **Typography**: Built-in PDF Type 1 Helvetica and Helvetica-Bold via `Standard14Fonts.FontName`.
- **Dynamic Pagination**:
  - `PdfContext` maintains `currentY` coordinate state.
  - When `currentY <= 65` pt, `newPage()` is invoked automatically.
  - Table column headers are re-rendered at the top of subsequent pages for uninterrupted reading.
- **Content Stream Lifecycle**:
  - Implements `AutoCloseable` to cleanly finalize open COSStream writers before document serialization (`document.save()`).
  - Running footer pass appends confidential disclaimer and total page count (`"Page X of Y"`) across all generated pages.

---

## 6. Excel Generation Architecture (Apache POI 5.2.5)

### Design & Layout Features:
- **File Format**: Modern OpenXML Spreadsheet (`.xlsx`) via `XSSFWorkbook`.
- **Executive Overview Tab**: Every generated workbook includes an initial "Executive Overview" tab displaying the candidate identity, date range, generation timestamp, and top 4 KPI cards.
- **Deep Slate Styling**: Header rows formatted with `#0F172A` fill, white bold typography, center/left alignment, and thin bottom borders.
- **Frozen Panes**: `sheet.createFreezePane(0, 1)` freezes the top header row so column names remain visible during vertical scrolling.
- **Explicit Cell Formats (`DataFormat`)**:
  - Currency: `$#,##0.00`
  - Dates: `yyyy-mm-dd`
  - Percentages: `0.0%`
  - Integers: `#,##0`
- **Dynamic Auto-Sizing**: Iterates columns and invokes `sheet.autoSizeColumn(i)` with an 11-character minimum floor to prevent squished columns.

---

## 7. Multi-Tenant Security & User Isolation

Multi-tenant data isolation is enforced through architectural constraints:
1. **Zero Client Trust**: All user identifiers originate exclusively from the securely authenticated `UserSession.getInstance().getUserId()`. The client UI cannot override or request another user's records.
2. **Parameterized JDBC Bindings**: Every query in `JdbcReportRepository` contains `WHERE applications.user_id = ?` or equivalent user boundary bindings.
3. **No Cross-User Leakage**: Foreign keys and JOINs (e.g. `interviews` joining `applications`, `recruiters` joining `applications`) join on `applications.user_id = ?`, preventing access to other users' recruiters, interviews, or company notes.
4. **Live Verification**: Automated integration tests confirm that queries for non-existent users return exactly 0 records.

---

## 8. Automated Testing Strategy

The test suite validates the module across 35 dedicated test cases:

```
Phase 10 Test Suite (35 Tests, 0 Failures, 0 Errors)
├── ReportServiceTest (12 Tests)
│   ├── Enum metadata verification
│   ├── Predefined and custom date filter boundaries
│   ├── Start date after end date validation exceptions
│   ├── Payload assembly for all 6 report types
│   ├── KPI calculations and conversion rate aggregations
│   └── Destination file extension routing (PDF vs Excel fallback)
├── PdfExportServiceTest (9 Tests)
│   ├── Real PDF generation for all 6 report types
│   ├── PDFBox Loader verification & page count assertion
│   ├── Empty dataset resilience
│   ├── Multi-page pagination (>60 items creates multiple pages)
│   └── Null payload defensive exception handling
├── ExcelExportServiceTest (8 Tests)
│   ├── Real Excel generation for all 6 report types
│   ├── Workbook sheet names & Executive Overview presence
│   ├── Column header titles & cell value inspections
│   ├── Currency & numeric cell format assertions
│   └── Empty dataset resilience
└── JdbcReportRepositoryLiveTest (6 Tests)
    ├── Live MariaDB query execution for demo candidate
    ├── Multi-table SQL JOIN integrity
    ├── Date range boundary filtering
    ├── Activity timeline chronological sorting
    └── Multi-tenant isolation & non-existent user scoping
```

