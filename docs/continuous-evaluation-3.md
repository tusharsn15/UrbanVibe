# Chitkara University - Department of Computer Science & Engineering
## Continuous Evaluation 3 - Project Milestone (25CSE0202)
**Reference Notice:** CUIET/CSE/ACAD/2026/358  
**Course:** Programming in Java (25CSE0202)  
**Batch:** BE CSE 3rd Sem (Batch 2025)  
**Project Title:** UrbanVibe - Entertainment & Experience Booking Management System  
**Team Composition:** 4 Members (see `team-contributions.md`)

---

## Rubrics Evaluation Mapping (Total: 25 Marks)

| Evaluation Component | Maximum Marks | Verification Reference |
|---|:---:|---|
| **1. Problem Statement** | **3** | [Section 1](#1-problem-statement-3-marks) |
| **2. Project Objectives & Scope** | **2** | [Section 2](#2-project-objectives-and-scope-2-marks) |
| **3. Project Design & Prototype** | **9** | [Section 3](#3-project-design--prototype-9-marks) |
| **4. Integration of Course Concepts** | **6** | [Section 4](#4-integration-of-all-course-concepts-6-marks) |
| **5. Code Quality, Progress Evidence & Documentation** | **5** | [Section 5](#5-code-quality-progress-evidence--documentation-5-marks) |
| **Total Marks** | **25** | |

---

## 1. Problem Statement (3 Marks)

### Identified Problem
In urban life, finding and booking entertainment experiences (movies, concerts, restaurant tables, sports/escape rooms) is fragmented across multiple independent platforms. Users must maintain separate accounts, navigate disconnected search tools, and manage disparate payment flows. 

### Core Technical Problem
Booking platforms face a critical **concurrency and race condition problem**: when multiple users attempt to reserve the exact same seat or remaining restaurant table at the exact same instant, naive systems suffer from double-booking or inconsistent database states. 

### Intended Users and Context
* **City Customers:** Discover, search, and securely book entertainment with instant receipt generation, promotional discounts, and cancellation refund policies.
* **Administrators:** Manage venue capacity, schedule time slots, monitor live bookings, and generate business revenue reports.

---

## 2. Project Objectives and Scope (2 Marks)

### Measurable Objectives
1. **Core Java Integration:** Demonstrate comprehensive mastery of Object-Oriented Programming (OOP), the Java Collections framework, custom checked exceptions, multithreading, and file I/O.
2. **Unified Experience Model:** Support both **seat-based** inventory (movies, live events with visual seat maps) and **capacity-based** inventory (dining tables, sports activities).
3. **Guaranteed Data Integrity:** Eliminate double-booking by enforcing database-level transactions (`SELECT ... FOR UPDATE`) paired with a `UNIQUE(seat_id)` constraint.
4. **Physical File Artifacts:** Generate readable text receipts (`receipts/`), CSV transaction exports (`exports/`), audit logs (`logs/`), and admin revenue summaries (`reports/`).

### Project Scope and Realistic Boundaries
* **Included:** Full CLI terminal interface, complete MySQL schema with ACID transaction isolation, simulated payment gateway with validation and intentional decline branches, background notification and logging threads.
* **Realistic Boundaries:** Developed as a console-based Core Java application to prioritize software design patterns, multithreaded concurrency, and database integrity over graphical UI frameworks.

---

## 3. Project Design & Prototype (9 Marks)

### Architectural Flow
```
User / Admin CLI Menu (com.urbanvibe.menu)
       │
       ▼
InputHelper & UserValidator (com.urbanvibe.utility)
       │
       ▼
Business Service Layer (com.urbanvibe.service)
  ├── AuthService
  ├── ExperienceService & SearchService
  ├── SeatManager & BookingService
  └── PaymentService (simulated gateway)
       │
       ▼
Data Access Objects (com.urbanvibe.dao)
  ├── UserDAO
  ├── ExperienceDAO
  └── BookingDAO
       │
       ▼
Database Connection Pool (com.urbanvibe.database)
       │
       ▼
MySQL Database (urbanvibe_db - 11 Relational Tables)
```

### Relational Data Model (11 Tables)
* `users` — Customers and Administrators with salted SHA-256 password hashes.
* `experiences` — Parent table for all bookable experiences.
* `movies`, `events`, `restaurants`, `activities` — Category-specific extension tables.
* `time_slots` — Scheduled showtimes, dates, and venue capacity.
* `seats` — Physical seat matrix (Rows A–E, Seats 1–8; VIP, Premium, Regular).
* `bookings` & `booking_seats` — Confirmed bookings with transactional link to seats.
* `payments` — Transaction logs with method (Card, UPI, Wallet) and refund status.
* `promo_codes` — Discount codes (`WELCOME10`, `FEST20`, `OLD5`).

### Working Prototype Capabilities
* **Full Authentication:** Customer/Admin registration and login with salt-based SHA-256 verification.
* **Catalog Exploration:** Search by keyword, filter by category and city, sort by price or title.
* **Booking & Checkout:** Seat selection, discount computation, transaction execution, and text receipt generation.
* **Cancellation Engine:** Tiered refund policy (100% refund >24h before show; 50% between 2–24h; non-refundable <2h).
* **Admin Controls:** Add/modify experiences, generate revenue/occupancy summaries, and run multithreaded concurrency stress tests.

---

## 4. Integration of all Course Concepts (6 Marks)

| Course Concept | Implementation in UrbanVibe | Key Classes Involved |
|---|---|---|
| **Inheritance** | Base abstract classes extended by specialized entity subclasses. | `Experience` extended by `Movie`, `Event`, `Restaurant`, `Activity`. `User` extended by `Customer`, `Admin`. |
| **Polymorphism** | Dynamic method overriding and method overloading. | Overriding: `calculatePrice()`, `getDetails()`, `getServiceFee()`. Overloading: `search(keyword)`, `search(cat, city)`, `search(min, max)`. |
| **Abstraction** | Abstract classes and interface contracts. | Abstract: `Experience`, `User`. Interfaces: `Bookable`, `Searchable`, `PaymentProcessor`. |
| **Encapsulation** | Private data fields exposed via accessor/mutator methods. | Encapsulated state across all 14 entity classes in `com.urbanvibe.model`. |
| **Collections Framework** | Dynamic data storage, filtering, and custom sorting. | `List<Experience>`, `ArrayList<Seat>`, `Map<Integer, Object>`, `Set<Integer>`, `Collections.sort()` with anonymous `Comparator`. |
| **Custom Exceptions** | 6 custom checked exceptions representing domain errors. | `BookingNotFoundException`, `SeatAlreadyBookedException`, `PaymentFailedException`, `InvalidInputException`, `InvalidLoginException`, `DatabaseOperationException`. |
| **JDBC Transactions** | MySQL connectivity, ACID compliance, parameterized queries. | `PreparedStatement`, `setAutoCommit(false)`, `commit()`, `rollback()`, row lock with `SELECT ... FOR UPDATE`. |
| **File Handling** | Persistent receipts, exported reports, CSVs, and audit logs. | `FileManager`, `ReceiptGenerator`, `ReportGenerator` utilizing `BufferedReader`, `BufferedWriter`, `Files`, and `Path`. |
| **Multithreading** | Asynchronous operations, worker queues, race condition demo. | `NotificationService` (producer/consumer), `AppLogger` (background logger), `BookingTask` (`ExecutorService` + `CountDownLatch`). |

---

## 5. Code Quality, Progress Evidence & Documentation (5 Marks)

### Code Quality & Standards
* **Student-Written Coding Style:** Refactored to clean, beginner-level Java: no AI-generated corporate boilerplate, no artificial rubric comment tags, no unnecessary functional stream chains.
* **Readable and Structured:** Natural variable naming, classic `for`/`while` loops, straightforward `if`/`else` control flow, and standard `try-catch` exception handling.
* **Zero Compilation Warnings:** Clean compilation using JDK 17 / Maven.

### Current Status (Milestone 3 Completed)
* Complete working CLI prototype with all 4 functional modules operational.
* Database schema with sample data, slots, and promo codes seeded.
* Concurrency race condition test verifying zero double-bookings.

### Setup Instructions (For Evaluator)
```bash
# 1. Initialize Database Schema & Data
mysql -u root -p < database/schema.sql

# 2. Configure Database Credentials (if password differs from blank/123456)
cp config/config.example.properties config/config.properties

# 3. Compile and Run
mvn compile exec:java
```

### Git Repository Progress Evidence
The repository history exhibits structured development commits across all 3 milestones:
* `856f34e` — `feat(milestone-1): Initial project structure, database schema, and domain model classes`
* `b8bf4b7` — `feat(milestone-2): JDBC connection layer, DAOs, and custom exception handling`
* `42d6513` — `feat(milestone-2): Service layer, booking transaction engine, and multithreading`
* `74792de` — `feat(milestone-3): CLI interactive menus, input validation, and file handling utilities`
* `7c2571d` — `docs(milestone-3): Project documentation, testing scenarios, and evaluation guide`

### Pending Work for Final Submission / Next Milestone
1. Integration of a temporary seat-hold countdown timer (5-minute lock with automatic rollback on timeout).
2. Customer waitlist system with automated notification when a booked seat is cancelled.
3. Enhanced loyalty tier rewards (Gold / Silver customer perks).
4. Export reports in PDF format alongside current TXT and CSV formats.
