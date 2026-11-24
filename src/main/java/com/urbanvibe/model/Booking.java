package com.urbanvibe.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Booking {

    public static final String CONFIRMED = "CONFIRMED";
    public static final String CANCELLED = "CANCELLED";

    private int bookingId;
    private String bookingRef;
    private int userId;
    private String userName;
    private int slotId;
    private int experienceId;
    private String experienceTitle;
    private String category;
    private String venue;
    private String city;
    private LocalDate slotDate;
    private LocalTime slotTime;
    private int quantity;
    private double subtotal;
    private double serviceFee;
    private double discount;
    private double totalAmount;
    private String promoCode;
    private String status;
    private String seatLabels;
    private LocalDateTime createdAt;
    private String paymentMethod;
    private String transactionRef;
    private double refundAmount;
    private String receiptPath;

    public Booking() {
        this.status = CONFIRMED;
        this.seatLabels = "";
    }

    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }
    public String getBookingRef() { return bookingRef; }
    public void setBookingRef(String bookingRef) { this.bookingRef = bookingRef; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public int getSlotId() { return slotId; }
    public void setSlotId(int slotId) { this.slotId = slotId; }
    public int getExperienceId() { return experienceId; }
    public void setExperienceId(int experienceId) { this.experienceId = experienceId; }
    public String getExperienceTitle() { return experienceTitle; }
    public void setExperienceTitle(String experienceTitle) { this.experienceTitle = experienceTitle; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }
    public LocalTime getSlotTime() { return slotTime; }
    public void setSlotTime(LocalTime slotTime) { this.slotTime = slotTime; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    public double getServiceFee() { return serviceFee; }
    public void setServiceFee(double serviceFee) { this.serviceFee = serviceFee; }
    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSeatLabels() { return seatLabels; }
    public void setSeatLabels(String seatLabels) { this.seatLabels = seatLabels; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
    public double getRefundAmount() { return refundAmount; }
    public void setRefundAmount(double refundAmount) { this.refundAmount = refundAmount; }
    public String getReceiptPath() { return receiptPath; }
    public void setReceiptPath(String receiptPath) { this.receiptPath = receiptPath; }

    public LocalDateTime getShowDateTime() {
        return LocalDateTime.of(slotDate, slotTime);
    }

    public boolean isConfirmed() {
        return CONFIRMED.equals(status);
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %s %s | qty %d | Rs. %.2f | %s",
                bookingRef, experienceTitle, slotDate, slotTime, quantity, totalAmount, status);
    }
}
