# Data Flow and Architecture

```
CLI Menu -> Input (Scanner via InputHelper) -> Service layer -> DAO layer -> JDBC (DatabaseConnection) -> MySQL
                                                                                                          |
CLI Menu <- result object / exception <- Service <- model objects <- DAO <- ResultSet <-------------------+
```

Rules: menus never write SQL; DAOs never print; services hold business rules; every SQL problem becomes a
`DatabaseOperationException` that the menu shows in plain words.

## 1. Registration and login
`UserMenu.registerFlow` reads fields -> `AuthService.register` -> `UserValidator.validate` -> `UserDAO.emailExists`
-> salt + SHA-256 hash -> `UserDAO.insertUser` (INSERT) -> id returned -> message.
Login: `AuthService.login` -> `UserDAO.findByEmail` (SELECT) -> `verifyPassword` -> role check -> `UserSession.login`
-> `CustomerMenu` or `AdminMenu`. Failure -> `InvalidLoginException`.

## 2. Experience browsing
`CustomerMenu.explore` -> `ExperienceService.listByCategory` -> `ExperienceDAO.findByCategory` (LEFT JOIN across the
child tables) -> `List<Experience>` of Movie/Event/Restaurant/Activity -> menu prints `toString()`/`getDetails()`.
For an outdoor `Event`, `ApiService.getWeather` is called (never throws).

## 3. Seat availability
Slot chosen -> `SeatManager.getAllSeats` -> `BookingDAO.findSeats` (seats LEFT JOIN booking_seats) -> `List<Seat>`
-> `displaySeatMap` prints `[A1 ]` free / `[XX ]` booked. Restaurants/activities show `available/total` from `time_slots`.

## 4. Ticket booking (the main workflow)
1. `CustomerMenu.bookFlow` collects slot, seats or quantity, promo code.
2. `BookingService.createDraft`: show not started, not sold out, `SeatManager.resolveSeats` (fresh DB read),
   subtotal (`Seat` prices or `Experience.calculatePrice`), `Experience.getServiceFee`, promo lookup -> unsaved `Booking`.
3. Menu prints the summary; customer confirms and picks a payment method.
4. `BookingService.confirmBooking` -> `PaymentService.processPayment` (may throw `PaymentFailedException`; nothing saved).
5. `synchronized` on the slot lock -> `BookingDAO.createBooking` (single transaction, see database-design.md).
   On `SeatAlreadyBookedException` the payment is refunded.
6. After commit: `ReceiptGenerator` writes the receipt, `NotificationService.send` queues the message,
   `AppLogger` logs. Booking object returns to the menu for the confirmation screen. History is a plain SELECT, so it is up to date.

## 5. Payment
`BookingService` -> `PaymentProcessor.processPayment(amount, method, detail)` -> validation (16-digit card / UPI id) ->
simulated decline rules -> `Payment{SUCCESS, TXN...}` returned, stored inside the booking transaction.

## 6. Cancellation
`CustomerMenu.cancelBooking` -> `BookingHistory.getRefundPercent` (100 / 50 / 0) -> `BookingService.cancelBooking`
-> `BookingDAO.cancelBooking` (transaction: lock booking row, delete booking_seats, restore capacity, status CANCELLED,
payment REFUNDED + amount, loyalty points reduced) -> `PaymentService.refund` -> notification.

## 7. Admin operations
`AdminMenu` -> `ExperienceService` (`requireAdmin()` checks `UserSession`) -> `ExperienceDAO` INSERT/UPDATE/DELETE
(parent + child in one transaction, soft delete when bookings exist); slots and seat maps; `UserDAO.getAllCustomers`;
`BookingDAO.findAll` (4-table JOIN); `ReportGenerator` GROUP BY queries -> text file in `reports/`.

## 8. Threads at run time

| Thread | Role |
|---|---|
| main | menus and all user work |
| `notification-thread` | consumes the notification queue |
| `logger-thread` | writes log lines to `logs/` |
| pool threads (demo only) | `BookingTask`s started by `ExecutorService` |
