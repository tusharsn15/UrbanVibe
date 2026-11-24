# Testing and Sample Outputs

All scenarios below were executed by running the real application (scripted console input) against a real database.

**Test environment (honest note):** JDK 21 (compiled with `--release 17`), **MariaDB 10.11** (MySQL-compatible) and the
MariaDB JDBC driver, because MySQL itself was not available where the project was built. The same `schema.sql`
and `jdbc:mysql://` URL were used. Please repeat the run once on your own MySQL 8 + `mysql-connector-j`;
the project has not been run through Maven (`pom.xml` is provided but unverified).
The full transcript of the main session is in `docs/sample-output.txt`.

## Results table

| # | Scenario | Result |
|---|---|---|
| 1 | Successful registration | PASS - "Registration successful! Your customer ID is 6." |
| 2 | Duplicate email | PASS - "This email is already registered. Please log in instead." |
| 3 | Weak password | PASS - password-rule message, nothing saved |
| 4 | Wrong password | PASS - "Login failed: Invalid email or password." |
| 5 | Correct login | PASS - "Login successful. Welcome, Ravi Kumar!" |
| 6 | Customer tries admin login | PASS - rejected with a role message |
| 7 | Search by keyword, by category+city, by price range, sorting; invalid price range | PASS - correct filtered lists; "Price range is invalid" message for min > max |
| 8 | Successful booking with promo | PASS - A1,A2 at Rs. 450 each = 900, WELCOME10 -90, total 810; receipt created |
| 9 | Booking an occupied seat | PASS - "Booking not completed: Seat A1 is already booked." |
| 10 | Two concurrent users, same seat (JVM lock ON) | PASS - one SUCCESS, one `SeatAlreadyBookedException` |
| 11 | Three users, same seat, JVM lock OFF (database decides) | PASS - exactly one winner |
| 12 | 20 threads, same seat, lock OFF (stress test) | PASS - 1 success, 19 rejected, 19 payments refunded, 1 row in `booking_seats` |
| 13 | Three users, different seats at once | PASS - all three succeed |
| 14 | Payment failure (card ending 0000) | PASS - "Payment failed: Card declined ... You have not been charged." No booking saved |
| 15 | Payment success (wallet, UPI, card) | PASS |
| 16 | Cancellation, 100% tier (3+ days ahead) | PASS - full refund, seats freed, capacity restored |
| 17 | Cancellation, 50% tier (slot 10 h away) | PASS - Rs. 250 booking refunded Rs. 125; payment status REFUNDED |
| 18 | Cancellation under 2 hours before show | PASS - rejected: too close to show time |
| 19 | Database down at start-up | PASS - friendly 3-step message, no stack trace |
| 20 | Database stopped mid-session | PASS - "Database problem: Cannot connect ... Is MySQL running?"; menu stays alive; error logged |
| 21 | Wrong database password | PASS - same friendly start-up message |
| 22 | Invalid console input (letters, out of range, empty) | PASS - message and re-prompt, no crash |
| 23 | API unavailable (fake key, no network) | PASS - "Weather unavailable right now (network problem)."; booking unaffected; error logged |
| 24 | API response parsing | PASS - sample JSON -> `Weather in Chandigarh: 24.6 C, clear sky (feels like 24.1 C), humidity 41%`; bad JSON -> graceful message |
| 25 | Receipt generation | PASS - `receipts/BOOKING_UV2026000001.txt` |
| 26 | History export | PASS - CSV in `exports/` |
| 27 | Admin: add/update/delete experience, add/delete slot, capacity change | PASS; deleting an experience with bookings deactivates it instead |
| 28 | Admin reports | PASS - revenue, per-experience, occupancy; full report file in `reports/` |
| 29 | Database constraint on its own | PASS - manually inserting a seat twice into `booking_seats` is rejected by `UNIQUE(seat_id)` |

## Sample output - double booking prevented

```
  ---- Results ----
  Demo User A: REJECTED - SeatAlreadyBookedException: Seat(s) [A1] already booked. Please choose different seats.
  Demo User B: SUCCESS - booking UV2026000008 for seat(s) A1

  1 of 2 succeeded. Exactly one winner = no double booking.
  The database (row locks + UNIQUE seat constraint) made the final decision.
```
(Which user wins varies from run to run - that is what a race is.)

## Sample receipt

```
==========================================
              URBANVIBE RECEIPT
     Entertainment & Experience Booking
==========================================
Booking Ref    : UV2026000001
Customer       : Ravi Kumar
------------------------------------------
Experience     : Stellar Odyssey (MOVIE)
Seats          : A1,A2
Quantity       : 2
------------------------------------------
Subtotal       : Rs. 900.00
Discount       : -Rs. 90.00 (WELCOME10)
TOTAL PAID     : Rs. 810.00
------------------------------------------
Payment method : WALLET
Payment status : SUCCESS
==========================================
```
(Shortened here; the real file also lists date, time, venue and transaction ID.)

## Known limitations found while testing

* Dates in sample data are relative to the day `schema.sql` is run; re-run it before a demo that is days later.
* No automated JUnit tests are included; testing was done by scripted CLI sessions and a Java stress harness.
* Console passwords are visible while typing.
