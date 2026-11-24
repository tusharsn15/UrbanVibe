# UrbanVibe - Entertainment & Experience Booking Management System

> **Chitkara University — Continuous Evaluation 3: Project Milestone**  
> **Course:** Programming in Java (`25CSE0202`) | **Notice Ref:** `CUIET/CSE/ACAD/2026/358`  
> Complete rubric compliance details: [docs/continuous-evaluation-3.md](docs/continuous-evaluation-3.md)

A console (CLI) Java application, inspired by BookMyShow and District, for booking **movies, concerts/events,
restaurant tables and activities**. Built with Core Java, JDBC and MySQL. No GUI, no web, no frameworks.

* 48 Java source files (32 core classes + 3 interfaces + 6 custom exceptions + 7 helpers)
* OOP, Collections, Multithreading, Exception handling, File handling, JDBC transactions
* Concurrency protection via row locks + a UNIQUE database constraint (zero double-booking)

## 1. Requirements

| Tool | Version |
|---|---|
| JDK | 17 or newer (developed on 21) |
| MySQL | 8.x (MariaDB 10.5+ also works) |
| MySQL JDBC driver | `mysql-connector-j` 8.x (the only external jar) |
| Maven | optional |

## 2. Set-up (first time)

```bash
# 1. Create the database, tables and sample data (DROPS and recreates urbanvibe_db)
mysql -u root -p < database/schema.sql

# 2. Create your private config file (it is git-ignored)
cp config/config.example.properties config/config.properties
#    edit it: db.user, db.password (and optionally weather.api.key)

# 3a. Build and run WITH Maven
mvn compile exec:java

# 3b. ...or WITHOUT Maven
#     download mysql-connector-j-8.x.jar (https://dev.mysql.com/downloads/connector/j/ - "Platform Independent")
#     and copy the jar into lib/
./build.sh && ./run.sh            # Windows: build.bat then run.bat
```

`schema.sql` already contains the sample data (the same inserts are in `database/sample_data.sql` if you only want to reload data).
Slot dates are relative to the day you run the script, so sample shows are always in the future. **If you run the demo
days later, re-run `schema.sql`** (slots in the past cannot be booked).

Settings can also come from environment variables, which override the file:
`URBANVIBE_DB_URL`, `URBANVIBE_DB_USER`, `URBANVIBE_DB_PASSWORD`, `URBANVIBE_WEATHER_KEY`, `URBANVIBE_PAYMENT_FAILURE_RATE`.

## 3. Default accounts (created automatically on first start)

| Role | Email | Password |
|---|---|---|
| Admin | admin@urbanvibe.com | Admin@123 |
| Customer | customer@urbanvibe.com | Customer@123 |
| Demo customers (used by the thread demo) | demo1@ / demo2@ / demo3@urbanvibe.com | Customer@123 |

Or choose **1. Register** to create your own customer.

## 4. Sample data and codes to try

* Promo codes: `WELCOME10` (10%), `FEST20` (20%), `OLD5` (expired - shows the error)
* Payment: card number ending `0000` is declined; UPI id starting `fail` is declined; any other 16-digit card, UPI id like `name@bank`, or Wallet succeeds
* Movies/events use a seat map (rows A-E x 8 seats; A-B VIP, C-D Premium, E Regular). Restaurants and activities use a guest count per time slot.

## 5. Weather API (optional)

For **outdoor events** the app shows the current weather using OpenWeatherMap.

1. Sign up free at https://openweathermap.org and copy your key from *My API keys* (new keys can take a little while to activate)
2. Put it in `config/config.properties` as `weather.api.key=...` **or** set the environment variable `URBANVIBE_WEATHER_KEY`
3. The key is never stored in source code, and `config/config.properties` is in `.gitignore`

Without a key or without internet the app prints "Weather unavailable" and everything else works normally.
Sample response: `docs/api-sample-response.json`.

## 6. Folder structure

```
UrbanVibe/
  pom.xml  README.md  build.sh run.sh build.bat run.bat  .gitignore
  database/   schema.sql  sample_data.sql
  config/     config.example.properties      (copy to config.properties)
  src/main/java/com/urbanvibe/
      Main.java
      model/ dao/ service/ menu/ database/ exception/ utility/ api/ threading/
  receipts/ logs/ reports/ exports/          (generated files)
  docs/       project-report.md class-diagram.md database-design.md data-flow.md
              team-contributions.md testing.md viva-questions.md api-sample-response.json
```

## 7. Five-minute evaluation demo

1. **Register**, then register the same email again -> duplicate error. Try a weak password -> validation message.
2. Wrong password, then correct login.
3. **Customer > Explore Events > 5** (outdoor event shows weather line).
4. **Book Experience > Movie > 1**, pick a future slot, seats `A1,A2`, promo `WELCOME10`, pay with wallet -> receipt in `receipts/`.
5. Book seat `A1` again -> *seat already booked*. Pay with card `4111111111110000` -> *payment declined*.
6. **View Booking History** -> export CSV to `exports/`. **Cancel Booking** -> refund per policy.
7. Admin login > **Manage Seats and Slots > Run concurrent booking demo** -> several threads, exactly one winner.
8. Admin > **Generate Reports** -> file in `reports/`. Show `logs/application.log`.
9. Stop MySQL and perform any action -> friendly database error, no crash.

## 8. Cancellation / refund policy

24 hours or more before the show: 100% refund - 2 to 24 hours: 50% - under 2 hours: cannot be cancelled.

## 9. Honest notes

* Passwords are hashed with salted SHA-256 (syllabus-friendly). Production systems should use BCrypt/Argon2.
* Payments are simulated; no real money is involved. Console input is not masked.
* Promo-code usage is not given back when a booking is cancelled (documented design choice).
