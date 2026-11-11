# ApplyTrack – Object-Oriented Architecture (OOP) Documentation

> **Designed for College Hackathons & Second-Year Computer Science Curriculum.**  
> Demonstrates core object-oriented principles: **Encapsulation**, **Inheritance**, **Polymorphism**, **Abstract Classes**, **Interfaces**, **Generics**, and the **Java Collections Framework**.

---

## 1. Class Diagram (Mermaid UML)

```mermaid
classDiagram
    class BaseEntity {
        <<abstract>>
        #int id
        #LocalDateTime createdAt
        #LocalDateTime updatedAt
        +getId() int
        +setId(int) void
        +getCreatedAt() LocalDateTime
        +getUpdatedAt() LocalDateTime
        +getDisplayTitle()* String
        +equals(Object) boolean
        +hashCode() int
    }

    class Searchable {
        <<interface>>
        +matches(String query) boolean
    }

    class User {
        -String fullName
        -String email
        -String passwordHash
        +getFullName() String
        +setFullName(String) void
        +getEmail() String
        +setEmail(String) void
        +getPasswordHash() String
        +setPasswordHash(String) void
        +getDisplayTitle() String
        +toString() String
    }

    class Company {
        -String companyName
        -String website
        -String industry
        -String location
        +getCompanyName() String
        +setCompanyName(String) void
        +getWebsite() String
        +getIndustry() String
        +getLocation() String
        +matches(String) boolean
        +getDisplayTitle() String
    }

    class Recruiter {
        -Integer companyId
        -Company company
        -String recruiterName
        -String email
        -String phone
        -String linkedinUrl
        -String notes
        +getRecruiterName() String
        +setCompany(Company) void
        +matches(String) boolean
        +getDisplayTitle() String
    }

    class Application {
        -int userId
        -User user
        -int companyId
        -Company company
        -Integer recruiterId
        -Recruiter recruiter
        -String jobRole
        -JobType jobType
        -LocalDate applicationDate
        -LocalDate deadline
        -String jobDescription
        -String jobUrl
        -BigDecimal expectedSalary
        -ApplicationStatus status
        -String notes
        -List~Interview~ interviews
        +addInterview(Interview) void
        +removeInterview(Interview) void
        +getInterviews() List~Interview~
        +matches(String) boolean
        +getDisplayTitle() String
    }

    class Interview {
        -int applicationId
        -Application application
        -String roundName
        -LocalDateTime interviewDate
        -InterviewMode interviewMode
        -String interviewLocation
        -String meetingLink
        -String interviewerName
        -String feedback
        -InterviewOutcome outcome
        +getRoundName() String
        +getInterviewDate() LocalDateTime
        +matches(String) boolean
        +getDisplayTitle() String
    }

    class DataContainer~T~ {
        -List~T~ items
        +add(T) void
        +addAll(Collection) void
        +remove(T) boolean
        +get(int) T
        +getAll() List~T~
        +filter(Predicate) List~T~
        +findFirst(Predicate) Optional~T~
    }

    BaseEntity <|-- User : Inheritance
    BaseEntity <|-- Company : Inheritance
    BaseEntity <|-- Recruiter : Inheritance
    BaseEntity <|-- Application : Inheritance
    BaseEntity <|-- Interview : Inheritance

    Searchable <|.. Company : Realization
    Searchable <|.. Recruiter : Realization
    Searchable <|.. Application : Realization
    Searchable <|.. Interview : Realization

    Company "1" <-- "0..*" Recruiter : Associated
    Company "1" <-- "0..*" Application : Offered By
    User "1" <-- "0..*" Application : Submitted By
    Recruiter "0..1" <-- "0..*" Application : Coordinated By
    Application "1" *-- "0..*" Interview : Composition (1:N)
```

---

## 2. Core OOP Principles Implemented

### A. Inheritance (`extends BaseEntity`)
All domain entities (`User`, `Company`, `Recruiter`, `Application`, `Interview`) extend the abstract base class `BaseEntity`:
- **Shared Attributes:** Primary identity `id`, audit timestamps `createdAt`, and `updatedAt`.
- **Constructor Delegation (`super`):** Subclasses invoke `super(id, createdAt, updatedAt)` to initialize the persistent identity cleanly.
- **Code Reuse:** Equality checks (`equals`), hashing (`hashCode`), and ID accessors are defined once in `BaseEntity`.

### B. Encapsulation & Data Validation
1. **Private Attributes:** Every instance field is declared `private` or `protected`.
2. **Controlled Mutators:** Setters enforce business constraints before modifying state:
   - `User.setEmail()` rejects emails missing `@` or domain dots.
   - `Application.setDeadline()` prevents deadlines from being scheduled before the application submission date.
   - `Application.setExpectedSalary()` rejects negative salaries.
3. **Security Encapsulation:** `User.toString()` and `DatabaseConfig.toString()` hide raw password hashes (`[PROTECTED]`), preventing credentials from leaking into terminal outputs or application log files.
4. **Defensive Copies:** `Application.getInterviews()` and `DataContainer.getAll()` return unmodifiable lists (`Collections.unmodifiableList()`), preventing external code from mutating internal lists without using designated domain methods (`addInterview()`, `removeInterview()`).

### C. Polymorphism & Interfaces (`implements Searchable`)
- The `Searchable` interface declares:
  ```java
  public interface Searchable {
      boolean matches(String query);
  }
  ```
- **Polymorphic Filtering:** `Company`, `Recruiter`, `Application`, and `Interview` implement `matches(query)` with their own domain logic. A global search bar can process any collection of `Searchable` objects without needing to know their specific concrete types.
- **Polymorphic Title Contract:** `BaseEntity.getDisplayTitle()` is an abstract method implemented distinctly by each model.

### D. Enums as Strongly-Typed Domain Categories
Rather than using arbitrary magic strings that cause typos and database bugs, ApplyTrack uses custom Java enums with custom attributes:
1. `ApplicationStatus`: `APPLIED`, `UNDER_REVIEW`, `SHORTLISTED`, `INTERVIEW_SCHEDULED`, `SELECTED`, `REJECTED`, `WITHDRAWN`, `NO_RESPONSE`.
   - Includes display names, hex badge colors (`#10b981`), and terminal state indicators (`isTerminal()`).
2. `JobType`: `FULL_TIME`, `INTERNSHIP`, `PART_TIME`.
3. `InterviewMode`: `ONLINE`, `OFFLINE`.
4. `InterviewOutcome`: `SCHEDULED`, `PENDING`, `PASSED`, `FAILED`, `ON_HOLD`, `CANCELLED`.

All enums provide a null-safe `fromString(String text)` method to reliably translate database values.

---

## 3. Generics Implementation (`DataContainer<T>`)

The `DataContainer<T>` class in `com.applytrack.util` illustrates:
1. **Generic Type Parameter (`<T>`):** Can store any model type (`DataContainer<Application>`, `DataContainer<Company>`, etc.) with full compile-time type safety.
2. **Bounded Wildcard (`addAll(Collection<? extends T> collection)`):** Accepts collections containing subtypes of `T` (covariance).
3. **Higher-Order Filtering:** Accepts a functional `Predicate<T>` to filter in-memory collections without writing loops repeatedly.
4. **Null-Safety with `Optional<T>`:** `findFirst(Predicate<T>)` returns an `Optional<T>` to prevent `NullPointerException`s.

---

## 4. Java Collections Framework Integration

In `ApplicationCollectionUtil`, the 4 core collections of Unit 3 are utilized:

| Collection | Class | Concrete Real-World Purpose in ApplyTrack |
| :--- | :--- | :--- |
| **`ArrayList`** | `List<Application>` | Stores ordered sequences of applications and interview rounds with $O(1)$ indexed retrieval. |
| **`HashMap`** | `Map<ApplicationStatus, List<Application>>` | Groups applications by status category for instant pipeline rendering and funnel calculations. |
| **`HashSet`** | `Set<String>` | Collects deduplicated company names with $O(1)$ lookup time. |
| **`PriorityQueue`** | `PriorityQueue<Application>` | Min-heap data structure sorting upcoming application deadlines chronologically using a custom comparator. |

---

## 5. Prevention of Circular References

In bidirectional domain models (e.g. `Application` references `Company`, and `Company` references `Application`s), auto-generated `toString()`, `equals()`, or `hashCode()` methods can trigger an infinite recursive loop resulting in a `StackOverflowError`.

In ApplyTrack:
- `Application.toString()` prints `companyId` and basic attributes, never calling `company.toString()`.
- `Recruiter.toString()` prints `companyId`, avoiding deep traversal.
- `BaseEntity.equals()` and `hashCode()` use the database primary key `id` and class type rather than traversing object references.

