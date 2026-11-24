# Viva Questions and Answers

## General / architecture
**Q1. Why a layered design (menu -> service -> DAO -> JDBC)?**
Each layer has one job. Menus handle input/output, services hold business rules, DAOs hold SQL. A change in one layer (for example the database) does not force changes in the others, and each member can work on a layer independently.

**Q2. Why is a DAO class used?** It keeps all SQL in one place per entity, so services stay free of JDBC code and are easier to read and test.

**Q3. Why only one external library?** JDBC always needs a vendor driver (`mysql-connector-j`). Everything else uses the standard library: `java.net.http.HttpClient`, `java.security.MessageDigest`, regex for parsing JSON.

## OOP
**Q4. Where is inheritance used?** `User` -> `Customer`, `Admin`; `Experience` -> `Movie`, `Event`, `Restaurant`, `Activity`.

**Q5. Show runtime polymorphism.** `ExperienceDAO` returns a `List<Experience>`; `BookingService` calls `exp.calculatePrice(qty)`, `exp.getServiceFee(...)` and `exp.isSeatBased()`. The JVM picks the Movie/Event/... version at run time. Also `User u = new Admin(...); u.getDashboardTitle()`.

**Q6. Overloading vs overriding with examples?** Overloading (same name, different parameters, decided at compile time): `SearchService.search(String)`, `search(String, String)`, `search(double, double)`. Overriding (subclass replaces parent behaviour, decided at run time): `Event.getServiceFee()` overrides `Experience.getServiceFee()`.

**Q7. Abstract class vs interface here?** `Experience` is abstract because it shares state (title, city, price) and some behaviour. `Bookable`, `Searchable`, `PaymentProcessor` are interfaces because they only define a contract that unrelated classes can implement.

**Q8. Constructor chaining example?** `Movie(title, city, ...)` calls `this(0, title, ..., true, ...)`, which calls `super(id, title, city, venue, price, "MOVIE", active)`.

**Q9. Why are fields private?** Encapsulation: outside code must use getters/setters, so validation or changes can be added in one place.

**Q10. Use of `static`?** `UserSession.currentUser`, `UserValidator` methods, `FileManager`, `AuthService.hashPassword`, `SeatManager.getLock`, constants like `Event.SERVICE_FEE_RATE`.

## Collections
**Q11. Which collections are used and why?** `ArrayList` for query results and seat lists; `LinkedHashSet` to remove duplicate seat labels while keeping order; `HashSet` for taken seats; `LinkedHashMap` for seat lookup/price legend; `ConcurrentHashMap` for per-slot locks (thread safe); `LinkedBlockingQueue` for notifications and logs; `Collections.sort` with `Comparator` for sorting.

## JDBC and MySQL
**Q12. Why `PreparedStatement`?** It prevents SQL injection and handles quoting/types. Every query with user data uses it.

**Q13. What is try-with-resources doing in the DAOs?** It closes `Connection`, `PreparedStatement` and `ResultSet` automatically even if an exception occurs.

**Q14. Explain the booking transaction.** `setAutoCommit(false)` -> lock seat rows with `SELECT ... FOR UPDATE` -> check `booking_seats` -> reduce capacity -> insert booking, booking_seats, payment -> update loyalty points -> `commit()`. Any failure -> `rollback()`, so nothing is half-saved.

**Q15. What are ACID properties and where do you see them?** Atomicity (all-or-nothing booking), Consistency (CHECK/UNIQUE/FK constraints), Isolation (row locks, READ COMMITTED), Durability (committed data survives restart).

**Q16. How do you prevent double booking?** Three layers: Java `synchronized` per slot (reduces contention), `SELECT ... FOR UPDATE` row locks inside the transaction, and `UNIQUE(seat_id)` in `booking_seats` as the last line of defence. The database has the final say, even if several JVMs ran the program.

**Q17. Why not rely only on `synchronized`?** It protects only threads inside one JVM. Two app instances, or a bug, would bypass it. A database constraint cannot be bypassed. The demo with the lock switched OFF proves this.

**Q18. Why is a cancelled booking's row removed from `booking_seats`?** Because of `UNIQUE(seat_id)`: deleting the row frees the seat for others. The labels remain in `bookings.seat_labels` for history.

**Q19. Why soft delete an experience?** Bookings reference its slots; physically deleting it would destroy history (and is blocked by foreign keys). So it is only deactivated.

**Q20. What is a JOIN and where do you use it?** Combining rows of related tables. `ExperienceDAO` LEFT JOINs the four child tables; the booking list joins bookings, users, time_slots, experiences and payments; reports use JOIN + GROUP BY.

## Multithreading
**Q21. What is a race condition? Show it here.** Two threads read a seat as free and both book it. In the demo several `BookingTask` threads start together (`CountDownLatch`) for the same seat; without protection both could succeed.

**Q22. Why `ExecutorService` instead of creating `Thread`s by hand?** A pool manages thread creation and shutdown, and `awaitTermination` lets us wait for all tasks.

**Q23. What does `CountDownLatch` do?** All threads wait at `await()`; `countDown()` releases them at the same instant, creating real contention.

**Q24. Which background threads exist?** `notification-thread` (producer/consumer with `BlockingQueue`) and `logger-thread`. The menu only enqueues a message, so the user never waits for I/O.

**Q25. Why `synchronized` in `FileManager.appendToFile`?** Several threads may log at once; the lock stops lines mixing.

## Exceptions
**Q26. List your custom exceptions and when they are thrown.** `InvalidLoginException` (bad credentials / wrong role), `InvalidInputException` (validation), `SeatAlreadyBookedException` (seat/capacity taken), `BookingNotFoundException` (unknown/cancelled/too-late booking), `PaymentFailedException` (declined/invalid), `DatabaseOperationException` (any SQL problem, wraps `SQLException`).

**Q27. Checked vs unchecked?** All custom exceptions are checked, so the compiler forces callers to handle or declare them (`throws`).

**Q28. Where do you use multi-catch, finally, throw, throws?** Multi-catch: `catch (SeatAlreadyBookedException | DatabaseOperationException e)` in `BookingService`. `finally`: `DatabaseConnection.closeQuietly(con)` and `Main`'s shutdown. `throw` inside services; `throws` on methods that can fail.

**Q29. Why wrap `SQLException`?** Upper layers should not depend on JDBC; the wrapper carries a readable message and the original cause for logs.

## File handling and API
**Q30. Which file classes are used?** `File`, `FileReader`, `FileWriter`, `BufferedReader`, `BufferedWriter`, `Files`, `Path/Paths` - for receipts, logs, CSV exports, reports and `config.properties`.

**Q31. How is the API key protected?** Read from the environment variable `URBANVIBE_WEATHER_KEY` or `config/config.properties` (git-ignored); never in source code.

**Q32. What if the API or internet fails?** `ApiService.getWeather` catches `IOException`, checks HTTP status, returns a friendly message and logs the error. Booking never depends on it.

**Q33. Why parse JSON with regex?** To avoid a JSON library outside the syllabus. A library such as Gson/Jackson would be the better choice in a production project.

## Security
**Q34. How are passwords stored?** As Base64(SHA-256(salt + password)) with a random 16-byte salt per user. Verification recomputes and compares with `MessageDigest.isEqual`. Production systems should use BCrypt or Argon2.

**Q35. Why the same error for wrong email and wrong password?** So attackers cannot find out which emails are registered.

## Features / design decisions
**Q36. Refund policy?** 24h or more before show: 100%; 2-24h: 50%; under 2h: not cancellable.

**Q37. Why is payment taken before the DB transaction, and what if the DB rejects the booking?** The simulated payment runs first; if the transaction fails (e.g. seat taken) `BookingService` immediately calls `refund(...)`. In the 20-thread stress test, 19 losing payments were refunded.

**Q38. How do restaurants differ from movies?** No seat rows. The slot holds `total_capacity` and `available_capacity`; booking reduces capacity with `UPDATE ... WHERE available_capacity >= ?`.

**Q39. What would you add next?** Seat-hold timer, waitlist, reviews, BCrypt, JUnit tests, a REST layer.
