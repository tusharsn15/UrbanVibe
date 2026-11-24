package com.urbanvibe.model;

import java.time.LocalDateTime;

public class Payment {

    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String REFUNDED = "REFUNDED";

    private int paymentId;
    private int bookingId;
    private double amount;
    private String method;
    private String status;
    private String transactionRef;
    private LocalDateTime paidAt;
    private double refundAmount;

    public Payment() { }

    public Payment(double amount, String method, String status, String transactionRef) {
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.transactionRef = transactionRef;
        this.paidAt = LocalDateTime.now();
    }

    public int getPaymentId() { return paymentId; }
    public void setPaymentId(int paymentId) { this.paymentId = paymentId; }
    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public double getRefundAmount() { return refundAmount; }
    public void setRefundAmount(double refundAmount) { this.refundAmount = refundAmount; }
}
