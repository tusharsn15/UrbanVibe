package com.urbanvibe.service;

import com.urbanvibe.database.DatabaseConnection;
import com.urbanvibe.exception.PaymentFailedException;
import com.urbanvibe.model.Payment;
import com.urbanvibe.utility.AppLogger;

import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

public class PaymentService implements PaymentProcessor {

    private static final Pattern UPI = Pattern.compile("^[A-Za-z0-9._-]{2,}@[A-Za-z]{2,}$");
    private final Random random = new Random();
    private final double failureRate;

    public PaymentService() {
        double rate = 0.0;
        try {
            rate = Double.parseDouble(DatabaseConnection.getSetting("URBANVIBE_PAYMENT_FAILURE_RATE",
                    "payment.failure.rate", "0"));
        } catch (NumberFormatException e) {
            AppLogger.warn("Invalid payment.failure.rate in config; using 0");
        }
        this.failureRate = Math.max(0.0, Math.min(1.0, rate));
    }

    public Payment processPayment(double amount, String method) throws PaymentFailedException {
        return processPayment(amount, method, "");
    }

    @Override
    public Payment processPayment(double amount, String method, String detail) throws PaymentFailedException {
        if (amount <= 0) {
            throw new PaymentFailedException("Invalid payment amount.");
        }
        String cleanDetail = detail == null ? "" : detail.replace(" ", "");
        switch (method) {
            case "CARD":
                if (!cleanDetail.matches("\\d{16}")) {
                    throw new PaymentFailedException("Invalid card number. Enter 16 digits.");
                }
                if (cleanDetail.endsWith("0000")) {
                    fail("Card declined by the bank (simulated).");
                }
                break;
            case "UPI":
                if (!UPI.matcher(cleanDetail).matches()) {
                    throw new PaymentFailedException("Invalid UPI id (example: name@bank).");
                }
                if (cleanDetail.toLowerCase().startsWith("fail")) {
                    fail("UPI payment rejected (simulated).");
                }
                break;
            case "WALLET":
                break;
            default:
                throw new PaymentFailedException("Unsupported payment method: " + method);
        }
        simulateProcessingDelay();
        if (failureRate > 0 && random.nextDouble() < failureRate) {
            fail("Payment gateway timeout (random simulated failure).");
        }
        String ref = "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        AppLogger.info("Payment SUCCESS " + ref + " " + method + " Rs." + amount);
        return new Payment(amount, method, Payment.SUCCESS, ref);
    }

    private void fail(String reason) throws PaymentFailedException {
        AppLogger.warn("Payment FAILED: " + reason);
        throw new PaymentFailedException(reason);
    }

    private void simulateProcessingDelay() {
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean refund(String transactionRef, double amount) {
        AppLogger.info("Refund issued for " + transactionRef + ": Rs." + amount);
        return true;
    }
}
