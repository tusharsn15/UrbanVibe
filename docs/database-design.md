# Database Design (`urbanvibe_db`)

MySQL 8 / InnoDB. Full script: `database/schema.sql`.

## ER diagram

```mermaid
erDiagram
    USERS ||--o{ BOOKINGS : makes
    EXPERIENCES ||--o| MOVIES : "is a"
    EXPERIENCES ||--o| EVENTS : "is a"
    EXPERIENCES ||--o| RESTAURANTS : "is a"
    EXPERIENCES ||--o| ACTIVITIES : "is a"
    EXPERIENCES ||--o{ TIME_SLOTS : "has"
    TIME_SLOTS ||--o{ SEATS : "has (seat based)"
    TIME_SLOTS ||--o{ BOOKINGS : "booked for"
    BOOKINGS ||--o{ BOOKING_SEATS : reserves
    SEATS ||--o| BOOKING_SEATS : "reserved by (max 1)"
    BOOKINGS ||--|| PAYMENTS : "paid by"
    PROMO_CODES ||--o{ BOOKINGS : "applied to"

    USERS { int user_id PK  varchar email UK  varchar password_hash  varchar salt  enum role  int loyalty_points }
    EXPERIENCES { int experience_id PK  enum category  varchar title  varchar city  decimal base_price  bool active }
    TIME_SLOTS { int slot_id PK  int experience_id FK  date slot_date  time slot_time  int total_capacity  int available_capacity }
    SEATS { int seat_id PK  int slot_id FK  varchar seat_label  enum seat_type  decimal price }
    BOOKINGS { int booking_id PK  varchar booking_ref UK  int user_id FK  int slot_id FK  decimal total_amount  enum status }
    BOOKING_SEATS { int booking_id PK_FK  int seat_id PK_FK_UK }
    PAYMENTS { int payment_id PK  int booking_id FK  decimal amount  enum method  enum status  decimal refund_amount }
    PROMO_CODES { varchar code PK  int percent_off  date expiry_date  int max_uses  int used_count }
```

## Tables

| Table | Purpose | Notable constraints |
|---|---|---|
| users | customers and admins | `UNIQUE(email)`, `CHECK(loyalty_points >= 0)`, role ENUM |
| experiences | parent row for every bookable thing | `CHECK(base_price >= 0)`, category ENUM |
| movies / events / restaurants / activities | category details (1-to-1 with experiences) | PK = FK to experiences, `ON DELETE CASCADE`, CHECKs |
| time_slots | a showing/session with capacity | `UNIQUE(experience_id, slot_date, slot_time)`, `CHECK(0 <= available_capacity <= total_capacity)` |
| seats | seat map for seat-based slots | `UNIQUE(slot_id, seat_label)` |
| bookings | one row per booking | `UNIQUE(booking_ref)`, FKs to users and time_slots, `CHECK(quantity > 0)` |
| booking_seats | which seats a booking holds | **`UNIQUE(seat_id)`** - a seat can be in only one booking |
| payments | payment and refund record | FK to bookings, status SUCCESS/FAILED/REFUNDED |
| promo_codes | discount codes | `CHECK(percent_off BETWEEN 1 AND 90)` |

## How double booking is prevented

1. `BookingDAO.createBooking` runs in a transaction (`READ COMMITTED`).
2. It executes `SELECT seat_id FROM seats WHERE slot_id=? AND seat_label IN (...) FOR UPDATE`. A second transaction asking for the same seats **waits** here.
3. It checks `booking_seats` (now showing the winner's committed row) and throws `SeatAlreadyBookedException`.
4. Even if steps 2-3 were skipped, `UNIQUE(seat_id)` makes the second `INSERT` fail, which the DAO converts to the same exception.
5. Capacity uses `UPDATE ... SET available_capacity = available_capacity - ? WHERE available_capacity >= ?` and a `CHECK`, so capacity can never go negative.

## SQL features used

`INSERT`, `SELECT`, `UPDATE`, `DELETE`, `JOIN` / `LEFT JOIN`, `GROUP BY` + aggregates (reports), sub-selects in sample data, `FOR UPDATE`, transactions with `commit`/`rollback`, batch inserts, generated keys.
