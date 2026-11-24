package com.urbanvibe.utility;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AppLogger {

    private static final BlockingQueue<String[]> QUEUE = new LinkedBlockingQueue<>();
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static Thread worker;
    private static boolean running = false;

    private AppLogger() { }

    public static synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        worker = new Thread(new Runnable() {
            @Override
            public void run() {
                processQueue();
            }
        }, "logger-thread");
        worker.setDaemon(true);
        worker.start();
    }

    private static void processQueue() {
        try {
            while (true) {
                String[] entry = QUEUE.take();
                if ("STOP".equals(entry[0])) {
                    break;
                }
                try {
                    FileManager.appendToFile(FileManager.LOGS_DIR + "/application.log", entry[1]);
                    if ("ERROR".equals(entry[0])) {
                        FileManager.appendToFile(FileManager.LOGS_DIR + "/error.log", entry[1]);
                    }
                } catch (IOException e) {
                    System.err.println("Logger could not write: " + e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void info(String message) {
        enqueue("INFO", message, null);
    }

    public static void warn(String message) {
        enqueue("WARN", message, null);
    }

    public static void error(String message, Throwable cause) {
        enqueue("ERROR", message, cause);
    }

    private static void enqueue(String level, String message, Throwable cause) {
        StringBuilder line = new StringBuilder();
        line.append(LocalDateTime.now().format(FORMAT)).append(" [").append(level).append("] [")
                .append(Thread.currentThread().getName()).append("] ").append(message);
        if (cause != null) {
            line.append(" | ").append(cause.getClass().getSimpleName()).append(": ").append(cause.getMessage());
        }
        QUEUE.offer(new String[]{level, line.toString()});
    }

    public static synchronized void shutdown() {
        if (!running) {
            return;
        }
        QUEUE.offer(new String[]{"STOP", ""});
        try {
            worker.join(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        running = false;
    }
}
