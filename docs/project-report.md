# UrbanVibe - Entertainment & Experience Booking Management System
### College Project Report (Core Java, JDBC, MySQL)

**Team:** 4 members - see `team-contributions.md`.  **Course:** Object-Oriented Programming with Java.

## 1. Project title
UrbanVibe - Entertainment & Experience Booking Management System. "Urban" = city life, "Vibe" = the energy of going out: one place for everything to do in the city.

## 2. Abstract
UrbanVibe is a console-based Java application that lets customers discover and book movies, concerts/events, restaurant tables and activities, and lets administrators manage inventory, slots, customers, bookings and reports. It uses object-oriented design, the Collections framework, multithreading, custom exception handling, file handling, JDBC with MySQL and one external weather API. Its central feature is a transaction-based booking engine that guarantees a seat can never be sold twice, even when many users book at the same instant.

## 3. Introduction
Booking platforms such as BookMyShow and District combine many kinds of entertainment under one login. This project reproduces their core logic in a terminal, so that the focus stays on Java fundamentals and database design instead of user-interface frameworks.

## 4. Problem statement
Entertainment options are spread across separate apps with separate logins, searches and payments. Booking systems must also stay correct under concurrency: two people selecting the same seat must never both succeed, and a failed payment or crash must never leave half-saved data.

## 5. Objectives
1. Apply OOP, collections, multithreading, exceptions, file handling and JDBC in one working system.
2. Provide one booking flow for seat-based (movies, events) and capacity-based (restaurants, activities) experiences.
3. Guarantee data integrity with transactions and constraints.
4. Produce receipts, logs, exports and reports as files.
5. Keep the core usable without internet access.

## 6. Proposed system
A layered CLI application: Menu -> Service -> DAO -> JDBC -> MySQL. Customers register, search, book, pay (simulated), cancel and export history. Admins perform CRUD on experiences and slots, view customers and bookings, and generate reports. Background threads send notifications and write logs. Extra features: promo codes, tiered refunds, loyalty points.

## 7. Technology stack
Java 17+, MySQL 8 with `mysql-connector-j`, optional Maven, OpenWeatherMap API through `java.net.http.HttpClient`, `java.util.concurrent` for threads, `java.nio.file` / `java.io` for files.

## 8. System architecture
```
CLI Menu (menu/) -> InputHelper -> Services (service/) -> DAOs (dao/) -> DatabaseConnection -> MySQL
                                       |-> PaymentService (simulated)
                                       |-> ReceiptGenerator / FileManager -> receipts/ logs/ reports/ exports/
                                       |-> NotificationService, AppLogger (background threads)
                                       |-> ApiService (optional weather)
```
Details: `data-flow.md`.

## 9. Class diagram
See `class-diagram.md` (Mermaid diagram, dependency diagram and class table).

## 10. ER diagram
See `database-design.md`.

## 11. Database design
Eleven tables: users, experiences (+ movies, events, restaurants, activities), time_slots, seats, bookings, booking_seats, payments, promo_codes. Primary keys, foreign keys with cascade rules, UNIQUE and CHECK constraints. `UNIQUE(seat_id)` on `booking_seats` prevents double booking.

## 12. Data flow
See `data-flow.md` for registration/login, browsing, availability, booking, payment, cancellation and admin flows.

## 13. Module descriptions
1. **User management** - registration, salted SHA-256 passwords, login/logout, roles, profile, validation.
2. **Experience management** - four experience types, search/filter/sort, admin CRUD, slots and seat maps.
3. **Booking and payment** - seat selection, price breakdown, promo codes, simulated payment, transactional booking, cancellation with refund tiers, history.
4. **Integration and utilities** - `Main`, JDBC connection, file handling, receipts, notifications, thread demo, API, reports.

## 14. Class responsibilities
Each of the 32 core classes, its owner, attributes, methods, OOP concepts and database interaction is listed in `class-diagram.md`.

## 15. Implementation details
* **Pricing:** seat-based subtotal = sum of seat prices (VIP x1.8, Premium x1.4, Regular x1.0); capacity-based = `calculatePrice(quantity)` (activities get 10% off for 4+). Events add a 5% service fee. Promo discount applies to the subtotal.
* **Booking ID:** `UV` + year + six-digit database id (e.g. `UV2026000001`).
* **Loyalty:** 1 point per Rs. 100 spent; adjusted on cancellation.
* **Configuration:** environment variable > `config/config.properties` > default.
* **Security:** `PreparedStatement` everywhere, salted hashes, generic login errors, admin-only service methods.

## 16. Exception handling
Six checked custom exceptions (`InvalidLoginException`, `SeatAlreadyBookedException`, `BookingNotFoundException`, `PaymentFailedException`, `InvalidInputException`, `DatabaseOperationException`). Demonstrated: `try/catch/finally`, `throw`, `throws`, multiple and multi-catch blocks, custom exceptions, try-with-resources (DAOs, `FileManager`, `ReceiptGenerator`). Menus catch every exception and re-prompt, so the application does not crash on invalid input, lost database connection, declined payment or API failure.

## 17. Multithreading
* `BookingTask implements Runnable`, run by an `ExecutorService`; a `CountDownLatch` releases all threads together.
* **Race condition:** two threads read the same seat as free and both try to write it. Prevention: `synchronized` on a per-slot lock (single JVM), `SELECT ... FOR UPDATE` and `UNIQUE(seat_id)` (database). The demo can switch the JVM lock off to show that the database alone still allows only one winner.
* `NotificationService` (producer/consumer with a `BlockingQueue`) and `AppLogger` run in background threads.
* Verified: 2-thread and 3-thread races, 3 parallel bookings of different seats, and a 20-thread stress run (1 success, 19 rejected and refunded).

## 18. File handling
`FileManager` uses `File`, `FileReader`, `FileWriter`, `BufferedReader`, `BufferedWriter`, `Files`, `Path`. Outputs: `receipts/*.txt`, `logs/application.log`, `logs/error.log`, `logs/notifications.log`, `exports/history_*.csv`, `reports/report_*.txt`; input: `config/config.properties`.

## 19. API integration
OpenWeatherMap current-weather endpoint, called through `HttpClient` with timeouts for outdoor events. Key from `URBANVIBE_WEATHER_KEY` or the git-ignored config file. Response fields (temperature, feels-like, humidity, description) are extracted with regular expressions. Every failure (no key, no network, 401, 404, bad JSON) returns a friendly message; the booking system never depends on it. Sample response: `api-sample-response.json`.

## 20. Testing and sample outputs
See `testing.md` (29 scenarios, all passing in the test environment) and `sample-output.txt`.

## 21. Team member contributions
See `team-contributions.md`.

## 22. Conclusion
UrbanVibe delivers a complete, console-only booking platform that applies the whole Java syllabus in one coherent project. Its database-first approach to concurrency produces correct behaviour under heavy contention, which was confirmed by thread demos and a stress test.

## 23. Future scope
Seat-hold timer with automatic release, waitlist with notifications, reviews and ratings, BCrypt/Argon2 passwords, a real payment gateway, JUnit test suite, REST API with a web or mobile front end, QR-code tickets, dynamic pricing, recommendation engine.
