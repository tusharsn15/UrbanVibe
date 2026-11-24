-- =====================================================================
--  UrbanVibe sample data
--  (Users are NOT inserted here: the application creates the default
--   admin and demo customers on first start-up with salted password hashes.)
--  Slot dates are relative to CURDATE() so the demo is always in the future.
-- =====================================================================
USE urbanvibe_db;

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
