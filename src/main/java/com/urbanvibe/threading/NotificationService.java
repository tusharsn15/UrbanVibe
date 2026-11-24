package com.urbanvibe.threading;

import com.urbanvibe.utility.AppLogger;
import com.urbanvibe.utility.FileManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class NotificationService implements Runnable {

    private static final String POISON_PILL = "__STOP__";

    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private Thread worker;

    public synchronized void start() {
        if (worker != null && worker.isAlive()) {
            return;
        }
        worker = new Thread(this, "notification-thread");
        worker.setDaemon(true);
        worker.start();
    }

    @Override
    public void run() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        try {
            while (true) {
                String message = queue.take();
                if (POISON_PILL.equals(message)) {
                    break;
                }
                System.out.println();
                System.out.println("  [NOTIFICATION] " + message);
                try {
                    FileManager.appendToFile(FileManager.LOGS_DIR + "/notifications.log",
                            LocalDateTime.now().format(fmt) + " " + message);
                } catch (IOException e) {
                    AppLogger.error("Could not write notification log", e);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void send(String message) {
        queue.offer(message);
    }

    public void sendBookingConfirmation(String customerName, String bookingRef, String title, double total) {
        send(String.format("Hi %s, your booking %s for \"%s\" is confirmed. Amount paid: Rs. %.2f",
                customerName, bookingRef, title, total));
    }

    public void sendCancellation(String customerName, String bookingRef, double refund) {
        send(String.format("Hi %s, booking %s is cancelled. Refund: Rs. %.2f", customerName, bookingRef, refund));
    }

    public synchronized void stop() {
        if (worker == null) {
            return;
        }
        queue.offer(POISON_PILL);
        try {
            worker.join(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        worker = null;
    }
}
