package com.urbanvibe.service;

import com.urbanvibe.dao.BookingDAO;
import com.urbanvibe.exception.DatabaseOperationException;
import com.urbanvibe.model.Booking;
import com.urbanvibe.utility.ConsoleView;
import com.urbanvibe.utility.FileManager;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BookingHistory {

    public static final long FULL_REFUND_HOURS = 24;
    public static final long MIN_CANCEL_HOURS = 2;

    private final BookingDAO bookingDAO;

    public BookingHistory(BookingDAO bookingDAO) {
        this.bookingDAO = bookingDAO;
    }

    public List<Booking> getHistory(int userId) throws DatabaseOperationException {
        return bookingDAO.findByUser(userId);
    }

    public void printHistory(List<Booking> bookings) {
        ConsoleView.printBookings(bookings, false);
    }

    private static double hoursUntilShow(Booking b) {
        return Duration.between(LocalDateTime.now(), b.getShowDateTime()).toMinutes() / 60.0;
    }

    public static boolean isCancellable(Booking b) {
        return b.isConfirmed() && hoursUntilShow(b) >= MIN_CANCEL_HOURS;
    }

    public static int getRefundPercent(Booking b) {
        double hours = hoursUntilShow(b);
        if (!b.isConfirmed() || hours < MIN_CANCEL_HOURS) {
            return 0;
        }
        return hours >= FULL_REFUND_HOURS ? 100 : 50;
    }

    public static double calculateRefund(Booking b) {
        return Math.round(b.getTotalAmount() * getRefundPercent(b)) / 100.0;
    }

    public String exportHistory(int userId, List<Booking> bookings) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("BookingRef,Experience,Category,Date,Time,Quantity,Seats,Subtotal,ServiceFee,Discount,Total,Status\n");
        for (Booking b : bookings) {
            csv.append(b.getBookingRef()).append(',')
                    .append('"').append(b.getExperienceTitle().replace("\"", "'")).append("\",")
                    .append(b.getCategory()).append(',')
                    .append(b.getSlotDate()).append(',')
                    .append(b.getSlotTime()).append(',')
                    .append(b.getQuantity()).append(',')
                    .append('"').append(b.getSeatLabels()).append("\",")
                    .append(String.format("%.2f", b.getSubtotal())).append(',')
                    .append(String.format("%.2f", b.getServiceFee())).append(',')
                    .append(String.format("%.2f", b.getDiscount())).append(',')
                    .append(String.format("%.2f", b.getTotalAmount())).append(',')
                    .append(b.getStatus()).append('\n');
        }
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String path = FileManager.EXPORTS_DIR + "/history_user" + userId + "_" + stamp + ".csv";
        FileManager.writeFile(path, csv.toString());
        return path;
    }
}
