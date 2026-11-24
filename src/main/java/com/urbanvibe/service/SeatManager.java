package com.urbanvibe.service;

import com.urbanvibe.dao.BookingDAO;
import com.urbanvibe.exception.DatabaseOperationException;
import com.urbanvibe.exception.InvalidInputException;
import com.urbanvibe.exception.SeatAlreadyBookedException;
import com.urbanvibe.model.Seat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SeatManager {

    public static final int MAX_SEATS_PER_BOOKING = 6;

    private static final Map<Integer, Object> SLOT_LOCKS = new ConcurrentHashMap<>();
    private static volatile boolean jvmLockEnabled = true;

    private final BookingDAO bookingDAO;

    public SeatManager(BookingDAO bookingDAO) {
        this.bookingDAO = bookingDAO;
    }

    public static synchronized Object getLock(int slotId) {
        Object lock = SLOT_LOCKS.get(slotId);
        if (lock == null) {
            lock = new Object();
            SLOT_LOCKS.put(slotId, lock);
        }
        return lock;
    }

    public static boolean isJvmLockEnabled() {
        return jvmLockEnabled;
    }

    public static void setJvmLockEnabled(boolean enabled) {
        jvmLockEnabled = enabled;
    }

    public List<Seat> getAllSeats(int slotId) throws DatabaseOperationException {
        return bookingDAO.findSeats(slotId);
    }

    public List<Seat> getAvailableSeats(int slotId) throws DatabaseOperationException {
        List<Seat> free = new ArrayList<>();
        for (Seat s : bookingDAO.findSeats(slotId)) {
            if (!s.isBooked()) {
                free.add(s);
            }
        }
        return free;
    }

    public void displaySeatMap(List<Seat> seats) {
        System.out.println();
        System.out.println("                 ---------- S C R E E N ----------");
        String currentRow = "";
        StringBuilder line = new StringBuilder();
        Map<String, Double> prices = new LinkedHashMap<>();
        for (Seat s : seats) {
            String row = s.getLabel().substring(0, 1);
            if (!row.equals(currentRow)) {
                if (line.length() > 0) {
                    System.out.println(line);
                }
                currentRow = row;
                line = new StringBuilder("   " + row + "  ");
            }
            line.append(s.isBooked() ? "[XX ] " : String.format("[%-3s] ", s.getLabel()));
            prices.putIfAbsent(s.getType(), s.getPrice());
        }
        if (line.length() > 0) {
            System.out.println(line);
        }
        System.out.println();
        StringBuilder legend = new StringBuilder("  [XX] = booked   Prices: ");
        for (Map.Entry<String, Double> entry : prices.entrySet()) {
            legend.append(entry.getKey()).append(" Rs. ").append(String.format("%.0f", entry.getValue())).append("   ");
        }
        System.out.println(legend);
    }

    public List<String> parseLabels(String input) throws InvalidInputException {
        Set<String> labels = new LinkedHashSet<>();
        for (String part : input.split(",")) {
            String label = part.trim().toUpperCase();
            if (label.isEmpty()) {
                continue;
            }
            if (!label.matches("^[A-Z]\\d{1,2}$")) {
                throw new InvalidInputException("'" + part.trim() + "' is not a valid seat label (example: A1).");
            }
            labels.add(label);
        }
        if (labels.isEmpty()) {
            throw new InvalidInputException("Please enter at least one seat.");
        }
        if (labels.size() > MAX_SEATS_PER_BOOKING) {
            throw new InvalidInputException("You can book at most " + MAX_SEATS_PER_BOOKING + " seats at a time.");
        }
        return new ArrayList<>(labels);
    }

    public List<Seat> resolveSeats(int slotId, List<String> labels)
            throws InvalidInputException, SeatAlreadyBookedException, DatabaseOperationException {
        Map<String, Seat> byLabel = new LinkedHashMap<>();
        for (Seat s : bookingDAO.findSeats(slotId)) {
            byLabel.put(s.getLabel(), s);
        }
        List<Seat> chosen = new ArrayList<>();
        for (String label : labels) {
            Seat seat = byLabel.get(label);
            if (seat == null) {
                throw new InvalidInputException("Seat " + label + " does not exist for this show.");
            }
            if (seat.isBooked()) {
                throw new SeatAlreadyBookedException("Seat " + label + " is already booked.");
            }
            chosen.add(seat);
        }
        return chosen;
    }
}
