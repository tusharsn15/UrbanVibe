package com.urbanvibe.dao;

import com.urbanvibe.database.DatabaseConnection;
import com.urbanvibe.exception.DatabaseOperationException;
import com.urbanvibe.model.Activity;
import com.urbanvibe.model.Event;
import com.urbanvibe.model.Experience;
import com.urbanvibe.model.Movie;
import com.urbanvibe.model.Restaurant;
import com.urbanvibe.model.TimeSlot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ExperienceDAO {

    private static final String BASE_SELECT =
            "SELECT e.experience_id, e.category, e.title, e.city, e.venue, e.base_price, e.active, "
                    + "m.language, m.genre, m.duration_mins, m.rating, "
                    + "ev.artist, ev.event_type, ev.outdoor, "
                    + "r.cuisine, r.total_capacity, "
                    + "a.activity_type, a.min_age "
                    + "FROM experiences e "
                    + "LEFT JOIN movies m ON m.experience_id = e.experience_id "
                    + "LEFT JOIN events ev ON ev.experience_id = e.experience_id "
                    + "LEFT JOIN restaurants r ON r.experience_id = e.experience_id "
                    + "LEFT JOIN activities a ON a.experience_id = e.experience_id ";

    public List<Experience> findAll(boolean onlyActive) throws DatabaseOperationException {
        String sql = BASE_SELECT + (onlyActive ? "WHERE e.active = 1 " : "") + "ORDER BY e.category, e.experience_id";
        return runQuery(sql, null);
    }

    public List<Experience> findByCategory(String category, boolean onlyActive) throws DatabaseOperationException {
        String sql = BASE_SELECT + "WHERE e.category = ? " + (onlyActive ? "AND e.active = 1 " : "")
                + "ORDER BY e.experience_id";
        return runQuery(sql, category);
    }

    public Experience findById(int experienceId) throws DatabaseOperationException {
        String sql = BASE_SELECT + "WHERE e.experience_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, experienceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapExperience(rs) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Could not read experience: " + e.getMessage(), e);
        }
    }

    private List<Experience> runQuery(String sql, String parameter) throws DatabaseOperationException {
        List<Experience> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (parameter != null) {
                ps.setString(1, parameter);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapExperience(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Could not list experiences: " + e.getMessage(), e);
        }
        return list;
    }

    private Experience mapExperience(ResultSet rs) throws SQLException {
        int id = rs.getInt("experience_id");
        String title = rs.getString("title");
        String city = rs.getString("city");
        String venue = rs.getString("venue");
        double price = rs.getDouble("base_price");
        boolean active = rs.getBoolean("active");
        switch (rs.getString("category")) {
            case "MOVIE":
                return new Movie(id, title, city, venue, price, active, rs.getString("language"),
                        rs.getString("genre"), rs.getInt("duration_mins"), rs.getDouble("rating"));
            case "EVENT":
                return new Event(id, title, city, venue, price, active, rs.getString("artist"),
                        rs.getString("event_type"), rs.getBoolean("outdoor"));
            case "RESTAURANT":
                return new Restaurant(id, title, city, venue, price, active, rs.getString("cuisine"),
                        rs.getInt("total_capacity"));
            default:
                return new Activity(id, title, city, venue, price, active, rs.getString("activity_type"),
                        rs.getInt("min_age"));
        }
    }

    public int insertExperience(Experience exp) throws DatabaseOperationException {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);
            int id;
            String sql = "INSERT INTO experiences (category, title, city, venue, base_price, active) VALUES (?,?,?,?,?,?)";
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, exp.getCategory());
                ps.setString(2, exp.getTitle());
                ps.setString(3, exp.getCity());
                ps.setString(4, exp.getVenue());
                ps.setDouble(5, exp.getBasePrice());
                ps.setBoolean(6, exp.isActive());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    id = keys.getInt(1);
                }
            }
            insertChild(con, id, exp);
            con.commit();
            exp.setExperienceId(id);
            return id;
        } catch (SQLException e) {
            DatabaseConnection.rollbackQuietly(con);
            throw new DatabaseOperationException("Could not add experience: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(con);
        }
    }

    private void insertChild(Connection con, int id, Experience exp) throws SQLException {
        if (exp instanceof Movie) {
            Movie m = (Movie) exp;
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO movies (experience_id, language, genre, duration_mins, rating) VALUES (?,?,?,?,?)")) {
                ps.setInt(1, id);
                ps.setString(2, m.getLanguage());
                ps.setString(3, m.getGenre());
                ps.setInt(4, m.getDurationMins());
                ps.setDouble(5, m.getRating());
                ps.executeUpdate();
            }
        } else if (exp instanceof Event) {
            Event ev = (Event) exp;
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO events (experience_id, artist, event_type, outdoor) VALUES (?,?,?,?)")) {
                ps.setInt(1, id);
                ps.setString(2, ev.getArtist());
                ps.setString(3, ev.getEventType());
                ps.setBoolean(4, ev.isOutdoor());
                ps.executeUpdate();
            }
        } else if (exp instanceof Restaurant) {
            Restaurant r = (Restaurant) exp;
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO restaurants (experience_id, cuisine, total_capacity) VALUES (?,?,?)")) {
                ps.setInt(1, id);
                ps.setString(2, r.getCuisine());
                ps.setInt(3, r.getTotalCapacity());
                ps.executeUpdate();
            }
        } else if (exp instanceof Activity) {
            Activity a = (Activity) exp;
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO activities (experience_id, activity_type, min_age) VALUES (?,?,?)")) {
                ps.setInt(1, id);
                ps.setString(2, a.getActivityType());
                ps.setInt(3, a.getMinAge());
                ps.executeUpdate();
            }
        }
    }

    public void updateExperience(Experience exp) throws DatabaseOperationException {
        String sql = "UPDATE experiences SET title = ?, city = ?, venue = ?, base_price = ?, active = ? "
                + "WHERE experience_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, exp.getTitle());
            ps.setString(2, exp.getCity());
            ps.setString(3, exp.getVenue());
            ps.setDouble(4, exp.getBasePrice());
            ps.setBoolean(5, exp.isActive());
            ps.setInt(6, exp.getExperienceId());
            if (ps.executeUpdate() == 0) {
                throw new DatabaseOperationException("Experience not found.");
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Could not update experience: " + e.getMessage(), e);
        }
    }

    public boolean deleteExperience(int experienceId) throws DatabaseOperationException {
        String countSql = "SELECT COUNT(*) FROM bookings b JOIN time_slots ts ON ts.slot_id = b.slot_id "
                + "WHERE ts.experience_id = ?";
        try (Connection con = DatabaseConnection.getConnection()) {
            int bookings;
            try (PreparedStatement ps = con.prepareStatement(countSql)) {
                ps.setInt(1, experienceId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    bookings = rs.getInt(1);
                }
            }
            if (bookings > 0) {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE experiences SET active = 0 WHERE experience_id = ?")) {
                    ps.setInt(1, experienceId);
                    ps.executeUpdate();
                }
                return false;
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM experiences WHERE experience_id = ?")) {
                ps.setInt(1, experienceId);
                if (ps.executeUpdate() == 0) {
                    throw new DatabaseOperationException("Experience not found.");
                }
            }
            return true;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Could not delete experience: " + e.getMessage(), e);
        }
    }

    public List<TimeSlot> findSlots(int experienceId, boolean futureOnly) throws DatabaseOperationException {
        String sql = "SELECT slot_id, experience_id, slot_date, slot_time, total_capacity, available_capacity "
                + "FROM time_slots WHERE experience_id = ? "
                + (futureOnly ? "AND TIMESTAMP(slot_date, slot_time) > NOW() " : "")
                + "ORDER BY slot_date, slot_time";
        List<TimeSlot> slots = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, experienceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    slots.add(mapSlot(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Could not list slots: " + e.getMessage(), e);
        }
        return slots;
    }

    public TimeSlot findSlotById(int slotId) throws DatabaseOperationException {
        String sql = "SELECT slot_id, experience_id, slot_date, slot_time, total_capacity, available_capacity "
                + "FROM time_slots WHERE slot_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, slotId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapSlot(rs) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Could not read slot: " + e.getMessage(), e);
        }
    }

    private TimeSlot mapSlot(ResultSet rs) throws SQLException {
        return new TimeSlot(rs.getInt("slot_id"), rs.getInt("experience_id"),
                rs.getDate("slot_date").toLocalDate(), rs.getTime("slot_time").toLocalTime(),
                rs.getInt("total_capacity"), rs.getInt("available_capacity"));
    }

    public int insertSlot(TimeSlot slot, double basePrice, int rows, int seatsPerRow)
            throws DatabaseOperationException {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);
            int slotId;
            String sql = "INSERT INTO time_slots (experience_id, slot_date, slot_time, total_capacity, available_capacity) "
                    + "VALUES (?,?,?,?,?)";
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, slot.getExperienceId());
                ps.setDate(2, java.sql.Date.valueOf(slot.getSlotDate()));
                ps.setTime(3, java.sql.Time.valueOf(slot.getSlotTime()));
                ps.setInt(4, slot.getTotalCapacity());
                ps.setInt(5, slot.getTotalCapacity());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    slotId = keys.getInt(1);
                }
            }
            if (rows > 0) {
                String seatSql = "INSERT INTO seats (slot_id, seat_label, seat_type, price) VALUES (?,?,?,?)";
                try (PreparedStatement ps = con.prepareStatement(seatSql)) {
                    for (int r = 0; r < rows; r++) {
                        char rowLetter = (char) ('A' + r);
                        String type = r < 2 ? "VIP" : (r < 4 ? "PREMIUM" : "REGULAR");
                        double multiplier = r < 2 ? 1.8 : (r < 4 ? 1.4 : 1.0);
                        double price = Math.round(basePrice * multiplier * 100.0) / 100.0;
                        for (int n = 1; n <= seatsPerRow; n++) {
                            ps.setInt(1, slotId);
                            ps.setString(2, "" + rowLetter + n);
                            ps.setString(3, type);
                            ps.setDouble(4, price);
                            ps.addBatch();
                        }
                    }
                    ps.executeBatch();
                }
            }
            con.commit();
            return slotId;
        } catch (SQLException e) {
            DatabaseConnection.rollbackQuietly(con);
            if (DatabaseConnection.isConstraintViolation(e)) {
                throw new DatabaseOperationException("A slot already exists for that experience at that date and time.", e);
            }
            throw new DatabaseOperationException("Could not add slot: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(con);
        }
    }

    public void deleteSlot(int slotId) throws DatabaseOperationException {
        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM bookings WHERE slot_id = ?")) {
                ps.setInt(1, slotId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        throw new DatabaseOperationException("This slot has bookings and cannot be deleted.");
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM time_slots WHERE slot_id = ?")) {
                ps.setInt(1, slotId);
                if (ps.executeUpdate() == 0) {
                    throw new DatabaseOperationException("Slot not found.");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Could not delete slot: " + e.getMessage(), e);
        }
    }

    public void updateSlotCapacity(int slotId, int newTotal) throws DatabaseOperationException {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);
            int total;
            int available;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT total_capacity, available_capacity FROM time_slots WHERE slot_id = ? FOR UPDATE")) {
                ps.setInt(1, slotId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new DatabaseOperationException("Slot not found.");
                    }
                    total = rs.getInt(1);
                    available = rs.getInt(2);
                }
            }
            int booked = total - available;
            if (newTotal < booked) {
                throw new DatabaseOperationException(
                        "Cannot reduce capacity below the " + booked + " places already booked.");
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE time_slots SET total_capacity = ?, available_capacity = ? WHERE slot_id = ?")) {
                ps.setInt(1, newTotal);
                ps.setInt(2, newTotal - booked);
                ps.setInt(3, slotId);
                ps.executeUpdate();
            }
            con.commit();
        } catch (SQLException e) {
            DatabaseConnection.rollbackQuietly(con);
            throw new DatabaseOperationException("Could not update capacity: " + e.getMessage(), e);
        } catch (DatabaseOperationException e) {
            DatabaseConnection.rollbackQuietly(con);
            throw e;
        } finally {
            DatabaseConnection.closeQuietly(con);
        }
    }
}
