package com.urbanvibe.threading;

import com.urbanvibe.exception.DatabaseOperationException;
import com.urbanvibe.exception.InvalidInputException;
import com.urbanvibe.exception.PaymentFailedException;
import com.urbanvibe.exception.SeatAlreadyBookedException;
import com.urbanvibe.model.Booking;
import com.urbanvibe.model.Experience;
import com.urbanvibe.model.TimeSlot;
import com.urbanvibe.model.User;
import com.urbanvibe.service.BookingService;
import com.urbanvibe.service.SeatManager;
import com.urbanvibe.utility.AppLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BookingTask implements Runnable {

    private final BookingService bookingService;
    private final User user;
    private final Experience experience;
    private final TimeSlot slot;
    private final List<String> seatLabels;
    private final CountDownLatch startGate;
    private volatile String result = "not run";
    private volatile boolean success = false;

    public BookingTask(BookingService bookingService, User user, Experience experience, TimeSlot slot,
                       List<String> seatLabels, CountDownLatch startGate) {
        this.bookingService = bookingService;
        this.user = user;
        this.experience = experience;
        this.slot = slot;
        this.seatLabels = seatLabels;
        this.startGate = startGate;
    }

    public String getResult() { return result; }
    public boolean isSuccess() { return success; }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        try {
            startGate.await();
            Booking draft = bookingService.createDraft(user.getUserId(), user.getName(), experience, slot,
                    seatLabels, seatLabels.size(), null);
            Booking saved = bookingService.confirmBooking(draft, seatLabels, true, "WALLET", "");
            result = "SUCCESS - booking " + saved.getBookingRef() + " for seat(s) " + saved.getSeatLabels();
            success = true;
        } catch (SeatAlreadyBookedException e) {
            result = "REJECTED - SeatAlreadyBookedException: " + e.getMessage();
        } catch (InvalidInputException | PaymentFailedException | DatabaseOperationException e) {
            result = "FAILED - " + e.getClass().getSimpleName() + ": " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result = "INTERRUPTED";
        }
        AppLogger.info("BookingTask [" + threadName + "] " + user.getName() + " -> " + result);
    }

    public static List<BookingTask> runConcurrently(BookingService service, Experience experience, TimeSlot slot,
                                                    List<User> users, List<List<String>> seatsPerUser,
                                                    boolean lockEnabled) throws InterruptedException {
        SeatManager.setJvmLockEnabled(lockEnabled);
        CountDownLatch gate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(users.size());
        List<BookingTask> tasks = Collections.synchronizedList(new ArrayList<>());
        try {
            for (int i = 0; i < users.size(); i++) {
                BookingTask task = new BookingTask(service, users.get(i), experience, slot, seatsPerUser.get(i), gate);
                tasks.add(task);
                pool.submit(task);
            }
            gate.countDown();
        } finally {
            pool.shutdown();
            if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
            SeatManager.setJvmLockEnabled(true);
        }
        return new ArrayList<>(tasks);
    }

    public String describe() {
        return user.getName() + ": " + result;
    }
}
