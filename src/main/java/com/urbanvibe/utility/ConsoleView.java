package com.urbanvibe.utility;

import com.urbanvibe.model.Booking;
import com.urbanvibe.model.Experience;
import com.urbanvibe.model.TimeSlot;

import java.util.List;

public class ConsoleView {

    public static final String LINE = "==========================================";

    private ConsoleView() { }

    public static void header(String title) {
        System.out.println();
        System.out.println(LINE);
        System.out.println("  " + title);
        System.out.println(LINE);
    }

    public static void error(String message) {
        System.out.println("  [!] " + message);
    }

    public static void info(String message) {
        System.out.println("  " + message);
    }

    public static String money(double amount) {
        return String.format("Rs. %.2f", amount);
    }

    public static void printExperiences(List<Experience> list) {
        if (list.isEmpty()) {
            info("No experiences found.");
            return;
        }
        for (Experience e : list) {
            System.out.println("  " + e);
        }
    }

    public static void printSlots(List<TimeSlot> slots) {
        if (slots.isEmpty()) {
            info("No upcoming slots available.");
            return;
        }
        System.out.println(String.format("  %-8s %-12s %-8s %s", "SlotID", "Date", "Time", "Availability"));
        for (TimeSlot s : slots) {
            String avail = s.isSoldOut() ? "SOLD OUT" : s.getAvailableCapacity() + " / " + s.getTotalCapacity();
            System.out.println(String.format("  %-8d %-12s %-8s %s",
                    s.getSlotId(), s.getSlotDate(), s.getSlotTime().toString().substring(0, 5), avail));
        }
    }

    public static void printBookings(List<Booking> bookings, boolean showUser) {
        if (bookings.isEmpty()) {
            info("No bookings found.");
            return;
        }
        System.out.println(String.format("  %-4s %-12s %-24s %-11s %-6s %-4s %-12s %s",
                "ID", "Reference", "Experience", "Date", "Time", "Qty", "Total", showUser ? "Customer / Status" : "Status"));
        for (Booking b : bookings) {
            String title = b.getExperienceTitle().length() > 24
                    ? b.getExperienceTitle().substring(0, 21) + "..." : b.getExperienceTitle();
            String tail = showUser ? b.getUserName() + " / " + b.getStatus() : b.getStatus();
            System.out.println(String.format("  %-4d %-12s %-24s %-11s %-6s %-4d %-12s %s",
                    b.getBookingId(), b.getBookingRef(), title, b.getSlotDate(),
                    b.getSlotTime().toString().substring(0, 5), b.getQuantity(),
                    String.format("%.2f", b.getTotalAmount()), tail));
        }
    }
}
