package com.urbanvibe.dao;

import com.urbanvibe.database.DatabaseConnection;
import com.urbanvibe.exception.BookingNotFoundException;
import com.urbanvibe.exception.DatabaseOperationException;
import com.urbanvibe.exception.SeatAlreadyBookedException;
import com.urbanvibe.model.Booking;
import com.urbanvibe.model.Payment;
import com.urbanvibe.model.PromoCode;
import com.urbanvibe.model.Seat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BookingDAO {

    public static int pointsFor(double totalAmount) {
        return (int) (totalAmount / 100);
    }

    private static final String BOOKING_SELECT =
            "SELECT b.booking_id, b.booking_ref, b.user_id, u.name AS user_name, b.slot_id, "
                    + "e.experience_id, e.title, e.category, e.venue, e.city, ts.slot_date, ts.slot_time, "
                    + "b.quantity, b.subtotal, b.service_fee, b.discount, b.total_amount, b.promo_code, "
                    + "b.status, b.seat_labels, b.created_at, p.method, p.transaction_ref, p.refund_amount "
                    + "FROM bookings b "
                    + "JOIN users u ON u.user_id = b.user_id "
                    + "JOIN time_slots ts ON ts.slot_id = b.slot_id "
                    + "JOIN experiences e ON e.experience_id = ts.experience_id "
                    + "LEFT JOIN payments p ON p.booking_id = b.booking_id ";

    public List<Seat> findSeats(int slotId) throws DatabaseOperationException {
        String sql = "SELECT s.seat_id, s.slot_id, s.seat_label, s.seat_type, s.price, "
                + "(bs.seat_id IS NOT NULL) AS booked "
                + "FROM seats s LEFT JOIN booking_seats bs ON bs.seat_id = s.seat_id "
                + "WHERE s.slot_id = ? "
                + "ORDER BY LEFT(s.seat_label, 1), CAST(SUBSTRING(s.seat_label, 2) AS UNSIGNED)";
        List<Seat> seats = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, slotId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    seats.add(new Seat(rs.getInt("seat_id"), rs.getInt("slot_id"), rs.getString("seat_label"),
                            rs.getString("seat_type"), rs.getDouble("price"), rs.getBoolean("booked")));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Could not read seats: " + e.getMessage(), e);
        }
        return seats;
    }

    public PromoCode findPromo(String code) throws DatabaseOperationException {
        String sql = "SELECT code, percent_off, expiry_date, max_uses, used_count FROM promo_codes WHERE code = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PromoCode(rs.getString("code"), rs.getInt("percent_off"),
                            rs.getDate("expiry_date").toLocalDate(), rs.getInt("max_uses"), rs.getInt("used_count"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Could not read promo code: " + e.getMessage(), e);
        }
    }

    public Booking createBooking(Booking booking, List<String> seatLabels, boolean seatBased, Payment payment)
            throws SeatAlreadyBookedException, DatabaseOperationException {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);
            con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            List<Integer> seatIds = new ArrayList<>();
            if (seatBased) {
                seatIds = lockAndCheckSeats(con, booking.getSlotId(), seatLabels);
            }
            reduceCapacity(con, booking.getSlotId(), booking.getQuantity());
            if (booking.getPromoCode() != null) {
                useVoucher(con, booking.getPromoCode());
            }

            int bookingId = insertBookingRow(con, booking);
            booking.setBookingId(bookingId);
            String ref = "UV" + LocalDate.now().getYear() + String.format("%06d", bookingId);
            try (PreparedStatement ps = con.prepareStatement("UPDATE bookings SET booking_ref = ? WHERE booking_id = ?")) {
                ps.setString(1, ref);
                ps.setInt(2, bookingId);
                ps.executeUpdate();
            }
            booking.setBookingRef(ref);

            if (seatBased) {
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO booking_seats (booking_id, seat_id) VALUES (?, ?)")) {
                    for (int seatId : seatIds) {
                        ps.setInt(1, bookingId);
                        ps.setInt(2, seatId);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO payments (booking_id, amount, method, status, transaction_ref) VALUES (?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, bookingId);
                ps.setDouble(2, payment.getAmount());
                ps.setString(3, payment.getMethod());
                ps.setString(4, payment.getStatus());
                ps.setString(5, payment.getTransactionRef());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        payment.setPaymentId(keys.getInt(1));
                    }
                }
            }
            payment.setBookingId(bookingId);

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE users SET loyalty_points = loyalty_points + ? WHERE user_id = ?")) {
                ps.setInt(1, pointsFor(booking.getTotalAmount()));
                ps.setInt(2, booking.getUserId());
                ps.executeUpdate();
            }

            con.commit();
            booking.setPaymentMethod(payment.getMethod());
            booking.setTransactionRef(payment.getTransactionRef());
            return booking;

        } catch (SeatAlreadyBookedException e) {
            DatabaseConnection.rollbackQuietly(con);
            throw e;
        } catch (DatabaseOperationException e) {
            DatabaseConnection.rollbackQuietly(con);
            throw e;
        } catch (SQLException e) {
            DatabaseConnection.rollbackQuietly(con);
            if (DatabaseConnection.isConstraintViolation(e) && seatBased) {
                throw new SeatAlreadyBookedException(
                        "One of the seats " + seatLabels + " was just booked by someone else.");
            }
            if ("40001".equals(e.getSQLState())) {
                throw new DatabaseOperationException("The system is busy. Please try again.", e);
            }
            throw new DatabaseOperationException("Booking failed and was rolled back: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(con);
        }
    }

    private List<Integer> lockAndCheckSeats(Connection con, int slotId, List<String> labels)
            throws SQLException, SeatAlreadyBookedException, DatabaseOperationException {
        String placeholders = placeholders(labels.size());
        List<Integer> ids = new ArrayList<>();
        String lockSql = "SELECT seat_id FROM seats WHERE slot_id = ? AND seat_label IN (" + placeholders
                + ") ORDER BY seat_id FOR UPDATE";
        try (PreparedStatement ps = con.prepareStatement(lockSql)) {
            ps.setInt(1, slotId);
            for (int i = 0; i < labels.size(); i++) {
                ps.setString(i + 2, labels.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        }
        if (ids.size() != labels.size()) {
            throw new DatabaseOperationException("One or more selected seats do not exist for this show.");
        }

        String checkSql = "SELECT s.seat_label FROM booking_seats bs JOIN seats s ON s.seat_id = bs.seat_id "
                + "WHERE s.slot_id = ? AND s.seat_label IN (" + placeholders + ")";
        Set<String> taken = new HashSet<>();
        try (PreparedStatement ps = con.prepareStatement(checkSql)) {
            ps.setInt(1, slotId);
            for (int i = 0; i < labels.size(); i++) {
                ps.setString(i + 2, labels.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    taken.add(rs.getString(1));
                }
            }
        }
        if (!taken.isEmpty()) {
            throw new SeatAlreadyBookedException("Seat(s) " + taken + " already booked. Please choose different seats.");
        }
        return ids;
    }

    private void reduceCapacity(Connection con, int slotId, int quantity)
            throws SQLException, SeatAlreadyBookedException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE time_slots SET available_capacity = available_capacity - ? "
                        + "WHERE slot_id = ? AND available_capacity >= ?")) {
            ps.setInt(1, quantity);
            ps.setInt(2, slotId);
            ps.setInt(3, quantity);
            if (ps.executeUpdate() == 0) {
                throw new SeatAlreadyBookedException("Not enough places left in this slot.");
            }
        }
    }

    private void useVoucher(Connection con, String code) throws SQLException, DatabaseOperationException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE promo_codes SET used_count = used_count + 1 "
                        + "WHERE code = ? AND used_count < max_uses AND expiry_date >= CURDATE()")) {
            ps.setString(1, code);
            if (ps.executeUpdate() == 0) {
                throw new DatabaseOperationException("Promo code " + code + " is no longer valid.");
            }
        }
    }

    private int insertBookingRow(Connection con, Booking b) throws SQLException {
        String sql = "INSERT INTO bookings (booking_ref, user_id, slot_id, quantity, subtotal, service_fee, discount, "
                + "total_amount, promo_code, status, seat_labels) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "TMP-" + java.util.UUID.randomUUID());
            ps.setInt(2, b.getUserId());
            ps.setInt(3, b.getSlotId());
            ps.setInt(4, b.getQuantity());
            ps.setDouble(5, b.getSubtotal());
            ps.setDouble(6, b.getServiceFee());
            ps.setDouble(7, b.getDiscount());
            ps.setDouble(8, b.getTotalAmount());
            ps.setString(9, b.getPromoCode());
            ps.setString(10, Booking.CONFIRMED);
            ps.setString(11, b.getSeatLabels());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private static String placeholders(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(i == 0 ? "?" : ",?");
        }
        return sb.toString();
    }

    public void cancelBooking(int bookingId, int userId, double refundAmount)
            throws BookingNotFoundException, DatabaseOperationException {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);
            con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            int slotId;
            int quantity;
            double total;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT slot_id, quantity, total_amount, status FROM bookings "
                            + "WHERE booking_id = ? AND user_id = ? FOR UPDATE")) {
                ps.setInt(1, bookingId);
                ps.setInt(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new BookingNotFoundException("Booking #" + bookingId + " was not found for your account.");
                    }
                    if (!Booking.CONFIRMED.equals(rs.getString("status"))) {
                        throw new BookingNotFoundException("Booking #" + bookingId + " is already cancelled.");
                    }
                    slotId = rs.getInt("slot_id");
                    quantity = rs.getInt("quantity");
                    total = rs.getDouble("total_amount");
                }
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM booking_seats WHERE booking_id = ?")) {
                ps.setInt(1, bookingId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE time_slots SET available_capacity = available_capacity + ? WHERE slot_id = ?")) {
                ps.setInt(1, quantity);
                ps.setInt(2, slotId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE bookings SET status = 'CANCELLED' WHERE booking_id = ?")) {
                ps.setInt(1, bookingId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE payments SET status = 'REFUNDED', refund_amount = ? WHERE booking_id = ?")) {
                ps.setDouble(1, refundAmount);
                ps.setInt(2, bookingId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE users SET loyalty_points = GREATEST(0, loyalty_points - ?) WHERE user_id = ?")) {
                ps.setInt(1, pointsFor(total));
                ps.setInt(2, userId);
                ps.executeUpdate();
            }
            con.commit();
        } catch (BookingNotFoundException e) {
            DatabaseConnection.rollbackQuietly(con);
            throw e;
        } catch (SQLException e) {
            DatabaseConnection.rollbackQuietly(con);
            throw new DatabaseOperationException("Cancellation failed and was rolled back: " + e.getMessage(), e);
        } finally {
            DatabaseConnection.closeQuietly(con);
        }
    }

    public List<Booking> findByUser(int userId) throws DatabaseOperationException {
        return runBookingQuery(BOOKING_SELECT + "WHERE b.user_id = ? ORDER BY b.booking_id DESC", userId);
    }

    public List<Booking> findAll() throws DatabaseOperationException {
        return runBookingQuery(BOOKING_SELECT + "ORDER BY b.booking_id DESC", null);
    }

    public Booking findById(int bookingId) throws DatabaseOperationException {
        List<Booking> list = runBookingQuery(BOOKING_SELECT + "WHERE b.booking_id = ?", bookingId);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<Booking> runBookingQuery(String sql, Integer parameter) throws DatabaseOperationException {
        List<Booking> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (parameter != null) {
                ps.setInt(1, parameter);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBooking(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Could not read bookings: " + e.getMessage(), e);
        }
        return list;
    }

    private Booking mapBooking(ResultSet rs) throws SQLException {
        Booking b = new Booking();
        b.setBookingId(rs.getInt("booking_id"));
        b.setBookingRef(rs.getString("booking_ref"));
        b.setUserId(rs.getInt("user_id"));
        b.setUserName(rs.getString("user_name"));
        b.setSlotId(rs.getInt("slot_id"));
        b.setExperienceId(rs.getInt("experience_id"));
        b.setExperienceTitle(rs.getString("title"));
        b.setCategory(rs.getString("category"));
        b.setVenue(rs.getString("venue"));
        b.setCity(rs.getString("city"));
        b.setSlotDate(rs.getDate("slot_date").toLocalDate());
        b.setSlotTime(rs.getTime("slot_time").toLocalTime());
        b.setQuantity(rs.getInt("quantity"));
        b.setSubtotal(rs.getDouble("subtotal"));
        b.setServiceFee(rs.getDouble("service_fee"));
        b.setDiscount(rs.getDouble("discount"));
        b.setTotalAmount(rs.getDouble("total_amount"));
        b.setPromoCode(rs.getString("promo_code"));
        b.setStatus(rs.getString("status"));
        b.setSeatLabels(rs.getString("seat_labels"));
        b.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        b.setPaymentMethod(rs.getString("method"));
        b.setTransactionRef(rs.getString("transaction_ref"));
        b.setRefundAmount(rs.getDouble("refund_amount"));
        return b;
    }
}
