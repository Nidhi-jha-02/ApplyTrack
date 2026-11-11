# ApplyTrack — Live Demonstration Guide
## 3-to-5 Minute Hackathon & Viva Presentation Walkthrough

This guide provides a structured, high-impact demonstration script for presenting **ApplyTrack** to hackathon judges, technical evaluators, or academic faculty.

---

## 1. Quick Launch & Setup

### Option A: 1-Click Launch (Recommended)
Double-click **`run.bat`** in the project root directory.

### Option B: Terminal Launch
```bash
mvn javafx:run
```

---

## 2. Authentication & Instant Demo Account

When the application opens, the modern **Sign In** card is displayed:

```
┌────────────────────────────────────────────────────────┐
│  ApplyTrack                                            │
│  Job Application Intelligence System                   │
│                                                        │
│  Email:       [ alex.mercer@applytrack.io            ] │
│  Password:    [ ••••••••••••             ] [ 👁 Show ] │
│                                                        │
│  [ Sign In to ApplyTrack ]                             │
│                                                        │
│  [ ⚡ Fill Demo Account ]   [ Create New Account ]     │
└────────────────────────────────────────────────────────┘
```

### For the Evaluator:
- Point out the **"⚡ Fill Demo Account"** button: Click it to automatically populate the pre-configured candidate credentials (`alex.mercer@applytrack.io` / `password123`).
- Click the **"👁 Show"** button next to the password input: Highlight that ApplyTrack uses bidirectional binding to allow candidates to safely inspect their password before logging in.
- Click **"Sign In to ApplyTrack"**.

---

## 3. Recommended 3–5 Minute Demonstration Flow

### Stop 1: Dynamic Dashboard (30 Seconds)
*“Welcome to the candidate intelligence dashboard.”*
- **Highlight 5 KPI Cards**: Point to Total Applications (6), In Progress (3), Scheduled Interviews (1), Offers (1), and Rejected (1). Explain that these metrics are not hardcoded—they are computed via live SQL aggregate queries for the authenticated candidate.
- **Showcase Status Distribution & Trends**: Point to the interactive **PieChart** showing status breakdown and the **LineChart** displaying monthly submission volume over time.
- **Point to Upcoming Action Panels**: Show the **Upcoming Deadlines** panel (with color-coded urgency badges: *Due in 4 Days*, *Due in 9 Days*) and the **Scheduled Interviews** panel with direct video meeting links.

---

### Stop 2: Job Applications Management (45 Seconds)
*“Let's look at the central application management table.”*
- Click **Job Applications** in the persistent sidebar.
- **Search & Filter in Real-Time**:
  - Type `Google` or `Intern` into the instant search box; observe the `TableView` filtering without lag.
  - Reset filters with **"Reset Filters"**.
- **Inspect Application Details**:
  - Select an application (e.g., *Google — Software Engineer*) and click **"View Details"**.
  - Show the clean modal displaying job role, salary ($135,000.00), application deadline, posting URL, and recruiter notes.
- **Cross-Module Action ("Schedule Interview")**:
  - Point to the **"Schedule Interview"** button right on the action bar.
  - Explain that selecting any application allows the candidate to schedule a follow-up interview round without losing context.

---

### Stop 3: Multi-Round Interview Pipeline (45 Seconds)
*“Now let's inspect the interview pipeline.”*
- Click **Interview Pipeline** in the sidebar.
- **Multi-Round Architecture**:
  - Show that Amazon has multiple interview rounds (*Online Assessment*, *Technical Round 1*, *Final HR & Bar Raiser*).
  - Point to the outcome badges: *Passed* (green pill) and *Scheduled* (blue pill).
- **Schedule an Interview Round**:
  - Click **"Schedule Interview"**.
  - Select an application from the dropdown, enter a round name (e.g., *Final Executive Round*), select interview mode (*Online*), paste a Google Meet link, and click **"Save Interview"**.
  - The new round appears immediately in the table and updates the upcoming interview count.

---

### Stop 4: Recruiter Directory & Safe FK Unlinking (30 Seconds)
*“Candidates build relationships with recruiters across hiring cycles.”*
- Click **Recruiter Directory** in the sidebar.
- Show the talent acquisition partner directory with corporate affiliations, direct email coordinates, phone numbers, and LinkedIn links.
- **Explain Database Relational Safety (3NF)**:
  - Explain that recruiters are linked to applications via `recruiter_id`.
  - Highlight the foreign key constraint rule: If a recruiter contact is removed, the database automatically sets `recruiter_id = NULL` (`ON DELETE SET NULL`), preserving the candidate's historical job applications and interview logs intact.

---

### Stop 5: Advanced Career Analytics & Funnels (45 Seconds)
*“ApplyTrack empowers candidates with data-driven job search insights.”*
- Click **Career Analytics** in the sidebar.
- **Showcase Conversion Rates**:
  - Point to the **Interview Conversion Rate** (e.g., *50.0%*) and **Offer Rate** (e.g., *16.7%*).
  - Explain the mathematical safeguard: division-by-zero protection ensures zero applications displays `0.0%` cleanly without runtime exceptions.
- **Temporal Filtering**:
  - Switch the date range from **All-Time** to **Last 30 Days**; show the charts dynamically re-aggregating on background worker threads.
- **Key Narrative Insights Banner**: Point out the automated takeaway summary identifying the candidate's peak application month and most frequent hiring partner.

---

### Stop 6: Reports Center & Document Generation (45 Seconds)
*“Candidates frequently need to export their data for campus placement cells or mentorship reviews.”*
- Click **Reports & Data Export** in the sidebar.
- **PDF Generation (Apache PDFBox 3.0)**:
  - Select **Job Application Report** from the dropdown.
  - Click **"Export as PDF (.pdf)"**.
  - The native JavaFX `FileChooser` dialog opens. Choose a location and click **Save**.
  - Open the generated PDF: Point out the branded deep-slate header, vector typography, candidate identity metadata, summary KPI cards, and alternating row striping.
- **Excel Generation (Apache POI 5.2.5)**:
  - Click **"Export as Excel (.xlsx)"**.
  - Open the generated `.xlsx` file: Point out the **Executive Overview** tab, frozen top header rows, and formatted currency and date columns.

---

### Stop 7: Settings & Instant Runtime Theming (30 Seconds)
*“Finally, let's explore candidate personalization and security.”*
- Click **Settings & Preferences** in the sidebar.
- **Personal Profile**: Change the candidate's name to *Alex Mercer (Senior SDE)* and click **Save Changes**; show that the sidebar name updates instantly without a page reload.
- **Instant Dark Mode**:
  - Switch to the **Preferences** tab.
  - Change Theme from **Light Mode** to **Dark Mode**.
  - The entire JavaFX interface restyles immediately to a deep-slate dark palette without restarting the application!
- **Cryptographic Security & Logout**:
  - Switch to the **Security** tab to highlight 10-round salted BCrypt hashing and password change verification rules.
  - Switch to the **Account** tab, click **"Sign Out"**, and confirm the dialog.
  - The session memory is purged completely, returning the evaluator to the secure Login screen.

---

## 4. Key Talking Points for Evaluators

| Evaluator Question | Recommended Student Answer |
| :--- | :--- |
| **Why not just use an Excel spreadsheet?** | *“Spreadsheets lack relational integrity, audit tracking, multi-round interview pipelines, and automated analytics. In ApplyTrack, foreign keys ensure that deleting a company or recruiter never corrupts application history, and the system computes conversion rates and upcoming deadline alerts in real time.”* |
| **How is candidate data secured?** | *“ApplyTrack enforces zero plaintext password storage. All credentials use salted BCrypt hashing (`org.mindrot:jbcrypt`) with work factor 10. Every database query strictly uses parameterized `PreparedStatement` scoped to `user_id = ?`, ensuring complete multi-tenant isolation.”* |
| **Why use JavaFX rather than a web framework?** | *“JavaFX delivers a native desktop experience with 60 FPS hardware-accelerated rendering, instant file system dialogs (`FileChooser`), local offline capability, and multithreading via `javafx.concurrent.Task` to ensure zero GUI freezing during heavy database reads or PDF exports.”* |
| **How does ApplyTrack align with our Java syllabus?** | *“ApplyTrack demonstrates all core syllabus concepts: OOP inheritance (`BaseEntity`), Polymorphism (`CrudRepository`), Collections (`HashMap`, `ArrayList`, `PriorityQueue`), Multithreading (`Task<T>`), JDBC transaction handling, Generics (`DataContainer<T>`), and modern Date/Time calculations (`java.time.LocalDate`).”* |

