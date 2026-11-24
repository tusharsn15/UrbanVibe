# Team Contributions

Four members, one module each. Every member can demo their module on its own and owns the SQL and documentation for it.

| Member | Module | Core classes | Extra files owned |
|---|---|---|---|
| **1** | User Management and Authentication | User, Customer, Admin, AuthService, UserDAO, UserSession, UserValidator, UserMenu (8) | InvalidLoginException, InvalidInputException |
| **2** | Entertainment and Experience Management | Experience, Movie, Event, Restaurant, Activity, ExperienceDAO, ExperienceService, SearchService (8) | Bookable, Searchable, TimeSlot, DatabaseOperationException, AdminMenu |
| **3** | Booking and Payment | Booking, Seat, SeatManager, BookingDAO, BookingService, Payment, PaymentService, BookingHistory (8) | PaymentProcessor, PromoCode, SeatAlreadyBookedException, BookingNotFoundException, PaymentFailedException, CustomerMenu |
| **4** | System Integration and Utilities | Main, DatabaseConnection, FileManager, ReceiptGenerator, NotificationService, BookingTask, ApiService, ReportGenerator (8) | InputHelper, ConsoleView, AppLogger, schema.sql, README, build scripts |

## What each member should be able to explain (viva)

**Member 1** - password hashing with salt, why login errors are generic, abstract `User` and polymorphic `getDashboardTitle()`,
regex validation, how `UserSession` enforces roles, `UserDAO` with `PreparedStatement` (SQL injection safety).

**Member 2** - abstract `Experience`, overriding `calculatePrice/getDetails/getServiceFee`, overloaded `search`,
LEFT JOIN + `mapExperience`, parent/child insert in one transaction, soft delete when bookings exist, `Searchable` interface.

**Member 3** - the booking transaction step by step, `FOR UPDATE`, UNIQUE(seat_id), rollback, refund tiers, price breakdown,
`PaymentProcessor` and the simulated gateway, why payment is refunded if the DB rejects the booking.

**Member 4** - `DatabaseConnection` and configuration order (env > file > default), file handling classes, producer/consumer
`NotificationService`, `AppLogger` thread, `ExecutorService` + `CountDownLatch` in `BookingTask`, `HttpClient` and graceful API failure, GROUP BY reports, how `Main` wires the layers.

## Suggested collaboration

1. Agree on the model getters and the three interfaces first (they are the contracts).
2. One Git branch per member; merge into `main` through Member 4, who owns `Main`.
3. Integrate in this order: database -> users -> experiences -> booking -> threads/API/reports.
4. Each member writes the matching section of `docs/project-report.md` and adds viva answers for their classes.
