package com.urbanvibe.utility;

import com.urbanvibe.database.DatabaseConnection;
import com.urbanvibe.exception.DatabaseOperationException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReportGenerator {

    public String revenueReport() throws DatabaseOperationException {
        StringBuilder sb = new StringBuilder();
        sb.append("REVENUE BY CATEGORY (confirmed bookings)\n");
        sb.append(String.format("%-12s %10s %12s%n", "Category", "Bookings", "Revenue"));
        String sql = "SELECT e.category, COUNT(b.booking_id) AS bookings, COALESCE(SUM(b.total_amount),0) AS revenue "
                + "FROM bookings b JOIN time_slots ts ON ts.slot_id = b.slot_id "
                + "JOIN experiences e ON e.experience_id = ts.experience_id "
                + "WHERE b.status = 'CONFIRMED' GROUP BY e.category ORDER BY revenue DESC";
        double grand = 0;
        int count = 0;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                sb.append(String.format("%-12s %10d %12.2f%n", rs.getString(1), rs.getInt(2), rs.getDouble(3)));
                grand += rs.getDouble(3);
                count += rs.getInt(2);
            }
            sb.append(String.format("%-12s %10d %12.2f%n", "TOTAL", count, grand));

            try (PreparedStatement ps2 = con.prepareStatement(
                    "SELECT COUNT(*), COALESCE(SUM(p.refund_amount),0) FROM bookings b "
                            + "JOIN payments p ON p.booking_id = b.booking_id WHERE b.status = 'CANCELLED'");
                 ResultSet rs2 = ps2.executeQuery()) {
                rs2.next();
                sb.append(String.format("%nCancelled bookings: %d | Total refunded: Rs. %.2f%n",
                        rs2.getInt(1), rs2.getDouble(2)));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Could not build revenue report: " + e.getMessage(), e);
        }
        return sb.toString();
    }

    public String bookingsPerExperienceReport() throws DatabaseOperationException {
        StringBuilder sb = new StringBuilder();
        sb.append("BOOKINGS PER EXPERIENCE\n");
        sb.append(String.format("%-28s %-11s %8s %8s %12s%n", "Experience", "Category", "Bookings", "Tickets", "Revenue"));
        String sql = "SELECT e.title, e.category, COUNT(b.booking_id) AS bookings, SUM(b.quantity) AS tickets, "
                + "SUM(b.total_amount) AS revenue FROM bookings b "
                + "JOIN time_slots ts ON ts.slot_id = b.slot_id "
                + "JOIN experiences e ON e.experience_id = ts.experience_id "
                + "WHERE b.status = 'CONFIRMED' GROUP BY e.experience_id, e.title, e.category "
                + "ORDER BY bookings DESC, revenue DESC";
        boolean any = false;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                any = true;
                sb.append(String.format("%-28s %-11s %8d %8d %12.2f%n", rs.getString(1), rs.getString(2),
                        rs.getInt(3), rs.getInt(4), rs.getDouble(5)));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Could not build experience report: " + e.getMessage(), e);
        }
        if (!any) {
            sb.append("(no confirmed bookings yet)\n");
        }
        return sb.toString();
    }

    public String occupancyReport() throws DatabaseOperationException {
        StringBuilder sb = new StringBuilder();
        sb.append("OCCUPANCY BY CATEGORY (all slots)\n");
        sb.append(String.format("%-12s %8s %10s %10s%n", "Category", "Sold", "Capacity", "Occupancy"));
        String sql = "SELECT e.category, SUM(ts.total_capacity - ts.available_capacity) AS sold, "
                + "SUM(ts.total_capacity) AS capacity FROM time_slots ts "
                + "JOIN experiences e ON e.experience_id = ts.experience_id GROUP BY e.category ORDER BY sold DESC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int sold = rs.getInt(2);
                int cap = rs.getInt(3);
                double pct = cap == 0 ? 0 : sold * 100.0 / cap;
                sb.append(String.format("%-12s %8d %10d %9.1f%%%n", rs.getString(1), sold, cap, pct));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Could not build occupancy report: " + e.getMessage(), e);
        }
        return sb.toString();
    }

    public String saveFullReport() throws DatabaseOperationException, IOException {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        StringBuilder sb = new StringBuilder();
        sb.append("URBANVIBE ADMIN REPORT - generated ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .append("\n==========================================\n\n");
        sb.append(revenueReport()).append('\n');
        sb.append(bookingsPerExperienceReport()).append('\n');
        sb.append(occupancyReport());
        String path = FileManager.REPORTS_DIR + "/report_" + stamp + ".txt";
        FileManager.writeFile(path, sb.toString());
        return path;
    }
}
