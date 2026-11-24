-- =====================================================================
--  UrbanVibe - Entertainment & Experience Booking Management System
--  Database schema (MySQL 8 / MariaDB 10.5+)
--  Run:  mysql -u root -p < database/schema.sql
--  WARNING: this script drops and recreates all UrbanVibe tables.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS urbanvibe_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE urbanvibe_db;
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS booking_seats;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS promo_codes;
DROP TABLE IF EXISTS seats;
DROP TABLE IF EXISTS time_slots;
DROP TABLE IF EXISTS activities;
DROP TABLE IF EXISTS restaurants;
DROP TABLE IF EXISTS events;
DROP TABLE IF EXISTS movies;
DROP TABLE IF EXISTS experiences;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- USERS (customers and admins)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id         INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(50)  NOT NULL,
    email           VARCHAR(100) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    salt            VARCHAR(50)  NOT NULL,
    phone           VARCHAR(15)  NOT NULL,
    role            ENUM('CUSTOMER','ADMIN') NOT NULL DEFAULT 'CUSTOMER',
    city            VARCHAR(50)  NOT NULL DEFAULT 'Chandigarh',
    loyalty_points  INT          NOT NULL DEFAULT 0,
    employee_code   VARCHAR(20)  NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_points CHECK (loyalty_points >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- EXPERIENCES (parent) + one child table per category
-- ---------------------------------------------------------------------
CREATE TABLE experiences (
    experience_id INT AUTO_INCREMENT PRIMARY KEY,
    category      ENUM('MOVIE','EVENT','RESTAURANT','ACTIVITY') NOT NULL,
    title         VARCHAR(100) NOT NULL,
    city          VARCHAR(50)  NOT NULL,
    venue         VARCHAR(100) NOT NULL,
    base_price    DECIMAL(10,2) NOT NULL,
    active        TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_exp_price CHECK (base_price >= 0),
    INDEX idx_exp_category (category),
    INDEX idx_exp_city (city)
) ENGINE=InnoDB;

CREATE TABLE movies (
    experience_id INT PRIMARY KEY,
    language      VARCHAR(30) NOT NULL,
    genre         VARCHAR(40) NOT NULL,
    duration_mins INT         NOT NULL,
    rating        DECIMAL(3,1) NOT NULL DEFAULT 0.0,
    CONSTRAINT fk_movie_exp FOREIGN KEY (experience_id) REFERENCES experiences(experience_id) ON DELETE CASCADE,
    CONSTRAINT chk_movie_duration CHECK (duration_mins > 0),
    CONSTRAINT chk_movie_rating CHECK (rating BETWEEN 0 AND 10)
) ENGINE=InnoDB;

CREATE TABLE events (
    experience_id INT PRIMARY KEY,
    artist        VARCHAR(80) NOT NULL,
    event_type    VARCHAR(40) NOT NULL,
    outdoor       TINYINT(1)  NOT NULL DEFAULT 0,
    CONSTRAINT fk_event_exp FOREIGN KEY (experience_id) REFERENCES experiences(experience_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE restaurants (
    experience_id  INT PRIMARY KEY,
    cuisine        VARCHAR(40) NOT NULL,
    total_capacity INT         NOT NULL,
    CONSTRAINT fk_rest_exp FOREIGN KEY (experience_id) REFERENCES experiences(experience_id) ON DELETE CASCADE,
    CONSTRAINT chk_rest_cap CHECK (total_capacity > 0)
) ENGINE=InnoDB;

CREATE TABLE activities (
    experience_id INT PRIMARY KEY,
    activity_type VARCHAR(40) NOT NULL,
    min_age       INT         NOT NULL DEFAULT 0,
    CONSTRAINT fk_act_exp FOREIGN KEY (experience_id) REFERENCES experiences(experience_id) ON DELETE CASCADE,
    CONSTRAINT chk_act_age CHECK (min_age >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- TIME SLOTS (a showing / session) and SEATS (for seat-based slots)
-- ---------------------------------------------------------------------
CREATE TABLE time_slots (
    slot_id            INT AUTO_INCREMENT PRIMARY KEY,
    experience_id      INT  NOT NULL,
    slot_date          DATE NOT NULL,
    slot_time          TIME NOT NULL,
    total_capacity     INT  NOT NULL,
    available_capacity INT  NOT NULL,
    CONSTRAINT fk_slot_exp FOREIGN KEY (experience_id) REFERENCES experiences(experience_id) ON DELETE CASCADE,
    CONSTRAINT uq_slot UNIQUE (experience_id, slot_date, slot_time),
    CONSTRAINT chk_slot_capacity CHECK (available_capacity >= 0 AND available_capacity <= total_capacity),
    INDEX idx_slot_date (slot_date)
) ENGINE=InnoDB;

CREATE TABLE seats (
    seat_id    INT AUTO_INCREMENT PRIMARY KEY,
    slot_id    INT NOT NULL,
    seat_label VARCHAR(10) NOT NULL,
    seat_type  ENUM('REGULAR','PREMIUM','VIP') NOT NULL DEFAULT 'REGULAR',
    price      DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_seat_slot FOREIGN KEY (slot_id) REFERENCES time_slots(slot_id) ON DELETE CASCADE,
    CONSTRAINT uq_seat_label UNIQUE (slot_id, seat_label),
    CONSTRAINT chk_seat_price CHECK (price >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- PROMO CODES
-- ---------------------------------------------------------------------
CREATE TABLE promo_codes (
    code        VARCHAR(20) PRIMARY KEY,
    percent_off INT  NOT NULL,
    expiry_date DATE NOT NULL,
    max_uses    INT  NOT NULL,
    used_count  INT  NOT NULL DEFAULT 0,
    CONSTRAINT chk_promo_percent CHECK (percent_off BETWEEN 1 AND 90),
    CONSTRAINT chk_promo_uses CHECK (used_count >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- BOOKINGS, BOOKING_SEATS, PAYMENTS
-- ---------------------------------------------------------------------
CREATE TABLE bookings (
    booking_id   INT AUTO_INCREMENT PRIMARY KEY,
    booking_ref  VARCHAR(40) NOT NULL,
    user_id      INT NOT NULL,
    slot_id      INT NOT NULL,
    quantity     INT NOT NULL,
    subtotal     DECIMAL(10,2) NOT NULL,
    service_fee  DECIMAL(10,2) NOT NULL DEFAULT 0,
    discount     DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(10,2) NOT NULL,
    promo_code   VARCHAR(20) NULL,
    status       ENUM('CONFIRMED','CANCELLED') NOT NULL DEFAULT 'CONFIRMED',
    seat_labels  VARCHAR(200) NOT NULL DEFAULT '',
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_booking_ref UNIQUE (booking_ref),
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_booking_slot FOREIGN KEY (slot_id) REFERENCES time_slots(slot_id),
    CONSTRAINT chk_booking_qty CHECK (quantity > 0),
    CONSTRAINT chk_booking_total CHECK (total_amount >= 0),
    INDEX idx_booking_user (user_id)
) ENGINE=InnoDB;

-- UNIQUE(seat_id) is the last line of defence against double booking:
-- a seat can appear in booking_seats only once. Cancelling deletes the row.
CREATE TABLE booking_seats (
    booking_id INT NOT NULL,
    seat_id    INT NOT NULL,
    PRIMARY KEY (booking_id, seat_id),
    CONSTRAINT uq_seat_once UNIQUE (seat_id),
    CONSTRAINT fk_bs_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
    CONSTRAINT fk_bs_seat FOREIGN KEY (seat_id) REFERENCES seats(seat_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE payments (
    payment_id      INT AUTO_INCREMENT PRIMARY KEY,
    booking_id      INT NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    method          ENUM('CARD','UPI','WALLET') NOT NULL,
    status          ENUM('SUCCESS','FAILED','REFUNDED') NOT NULL,
    transaction_ref VARCHAR(30) NOT NULL,
    refund_amount   DECIMAL(10,2) NOT NULL DEFAULT 0,
    paid_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pay_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
    CONSTRAINT chk_pay_amount CHECK (amount >= 0)
) ENGINE=InnoDB;

-- =====================================================================
--  UrbanVibe sample data
--  (Users are NOT inserted here: the application creates the default
--   admin and demo customers on first start-up with salted password hashes.)
--  Slot dates are relative to CURDATE() so the demo is always in the future.
-- =====================================================================

-- ----- Experiences: ids 1-4 movies, 5-8 events, 9-11 restaurants, 12-14 activities (Chandigarh) -----
INSERT INTO experiences (experience_id, category, title, city, venue, base_price) VALUES
 (1,'MOVIE','Stellar Odyssey','Chandigarh','PVR Elante',250.00),
 (2,'MOVIE','Laugh Riot','Chandigarh','INOX Sector 17',200.00),
 (3,'MOVIE','The Last Monsoon','Chandigarh','Cinepolis Bestech Square',180.00),
 (4,'MOVIE','Dark Harbor','Chandigarh','PVR Centra Mall',300.00),
 (5,'EVENT','Neon Nights Live','Chandigarh','Leisure Valley Grounds',800.00),
 (6,'EVENT','Stand-Up Saturday','Chandigarh','The Laugh Club, Sector 26',500.00),
 (7,'EVENT','Raga Evening','Chandigarh','Tagore Theatre',400.00),
 (8,'EVENT','Rock the Ridge Festival','Chandigarh','Sukhna Lake Club Grounds',1200.00),
 (9,'RESTAURANT','Spice Route','Chandigarh','Sector 26 Market',350.00),
 (10,'RESTAURANT','Bella Napoli','Chandigarh','Sector 35 Inner Market',600.00),
 (11,'RESTAURANT','Tokyo Table','Chandigarh','Elante Mall Courtyard',700.00),
 (12,'ACTIVITY','Go-Karting Grand Prix','Chandigarh','Speed Arena, Sector 18',450.00),
 (13,'ACTIVITY','Escape Room: The Vault','Chandigarh','Mystery Rooms, Sector 26',550.00),
 (14,'ACTIVITY','Sunrise Trekking Trail','Chandigarh','Morni Hills Base Camp',300.00);

INSERT INTO movies (experience_id, language, genre, duration_mins, rating) VALUES
 (1,'Hindi','Sci-Fi',148,8.4),
 (2,'Hindi','Comedy',126,7.6),
 (3,'Punjabi','Drama',139,8.1),
 (4,'English','Thriller',117,7.9);

INSERT INTO events (experience_id, artist, event_type, outdoor) VALUES
 (5,'DJ Aurora','EDM Concert',1),
 (6,'Vikas Verma','Stand-up Comedy',0),
 (7,'Ustad Rahim','Classical Music',0),
 (8,'The Ridge Band','Music Festival',1);

INSERT INTO restaurants (experience_id, cuisine, total_capacity) VALUES
 (9,'North Indian',60),
 (10,'Italian',50),
 (11,'Japanese',40);

INSERT INTO activities (experience_id, activity_type, min_age) VALUES
 (12,'Racing',12),
 (13,'Puzzle Game',10),
 (14,'Adventure',14);

-- ----- Time slots (next 3 days) -----
-- Movies: 14:00 and 19:30, 40 seats each (5 rows x 8)
INSERT INTO time_slots (experience_id, slot_date, slot_time, total_capacity, available_capacity)
SELECT e.experience_id, DATE_ADD(CURDATE(), INTERVAL d.n DAY), t.slot_time, 40, 40
FROM experiences e
CROSS JOIN (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3) d
CROSS JOIN (SELECT '14:00:00' AS slot_time UNION ALL SELECT '19:30:00') t
WHERE e.category = 'MOVIE';

-- Events: one evening show per day, 40 seats
INSERT INTO time_slots (experience_id, slot_date, slot_time, total_capacity, available_capacity)
SELECT e.experience_id, DATE_ADD(CURDATE(), INTERVAL d.n DAY), '18:30:00', 40, 40
FROM experiences e
CROSS JOIN (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3) d
WHERE e.category = 'EVENT';

-- Restaurants: lunch and dinner, capacity = guests per sitting
INSERT INTO time_slots (experience_id, slot_date, slot_time, total_capacity, available_capacity)
SELECT e.experience_id, DATE_ADD(CURDATE(), INTERVAL d.n DAY), t.slot_time, 30, 30
FROM experiences e
CROSS JOIN (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3) d
CROSS JOIN (SELECT '13:00:00' AS slot_time UNION ALL SELECT '20:00:00') t
WHERE e.category = 'RESTAURANT';

-- Activities: morning and afternoon sessions, 12 participants
INSERT INTO time_slots (experience_id, slot_date, slot_time, total_capacity, available_capacity)
SELECT e.experience_id, DATE_ADD(CURDATE(), INTERVAL d.n DAY), t.slot_time, 12, 12
FROM experiences e
CROSS JOIN (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3) d
CROSS JOIN (SELECT '09:00:00' AS slot_time UNION ALL SELECT '16:00:00') t
WHERE e.category = 'ACTIVITY';

-- ----- Seats for every seat-based slot: rows A-E, seats 1-8 -----
-- Rows A,B = VIP (x1.8), C,D = PREMIUM (x1.4), E = REGULAR (x1.0)
INSERT INTO seats (slot_id, seat_label, seat_type, price)
SELECT s.slot_id,
       CONCAT(r.row_label, n.num),
       CASE WHEN r.row_label IN ('A','B') THEN 'VIP'
            WHEN r.row_label IN ('C','D') THEN 'PREMIUM'
            ELSE 'REGULAR' END,
       ROUND(e.base_price * CASE WHEN r.row_label IN ('A','B') THEN 1.8
                                 WHEN r.row_label IN ('C','D') THEN 1.4
                                 ELSE 1.0 END, 2)
FROM time_slots s
JOIN experiences e ON e.experience_id = s.experience_id
CROSS JOIN (SELECT 'A' AS row_label UNION ALL SELECT 'B' UNION ALL SELECT 'C'
            UNION ALL SELECT 'D' UNION ALL SELECT 'E') r
CROSS JOIN (SELECT 1 AS num UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
            UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8) n
WHERE e.category IN ('MOVIE','EVENT');

-- ----- Promo codes -----
INSERT INTO promo_codes (code, percent_off, expiry_date, max_uses, used_count) VALUES
 ('WELCOME10', 10, DATE_ADD(CURDATE(), INTERVAL 365 DAY), 1000, 0),
 ('FEST20',    20, DATE_ADD(CURDATE(), INTERVAL 30 DAY),  50,   0),
 ('OLD5',       5, DATE_SUB(CURDATE(), INTERVAL 1 DAY),   100,  0);
