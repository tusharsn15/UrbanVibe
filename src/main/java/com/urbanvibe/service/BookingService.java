package com.urbanvibe.service;

import com.urbanvibe.dao.BookingDAO;
import com.urbanvibe.exception.BookingNotFoundException;
import com.urbanvibe.exception.DatabaseOperationException;
import com.urbanvibe.exception.InvalidInputException;
import com.urbanvibe.exception.PaymentFailedException;
import com.urbanvibe.exception.SeatAlreadyBookedException;
import com.urbanvibe.model.Booking;
import com.urbanvibe.model.Experience;
import com.urbanvibe.model.Payment;
import com.urbanvibe.model.PromoCode;
import com.urbanvibe.model.Seat;
import com.urbanvibe.model.TimeSlot;
import com.urbanvibe.threading.NotificationService;
import com.urbanvibe.utility.AppLogger;
import com.urbanvibe.utility.ReceiptGenerator;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class BookingService {

    public static final int MAX_QUANTITY = 10;

    private final BookingDAO bookingDAO;
    private final SeatManager seatManager;
    private final PaymentService paymentService;
    private final ReceiptGenerator receiptGenerator;
    private final NotificationService notificationService;

    public BookingService(BookingDAO bookingDAO, SeatManager seatManager, PaymentService paymentService,
                          ReceiptGenerator receiptGenerator, NotificationService notificationService) {
        this.bookingDAO = bookingDAO;
        this.seatManager = seatManager;
        this.paymentService = paymentService;
        this.receiptGenerator = receiptGenerator;
        this.notificationService = notificationService;
    }

    public Booking createDraft(int userId, String userName, Experience exp, TimeSlot slot,
                               List<String> seatLabels, int quantity, String promoCode)
            throws InvalidInputException, SeatAlreadyBookedException, DatabaseOperationException {
        if (slot.getStartDateTime().isBefore(LocalDateTime.now())) {
            throw new InvalidInputException("This show has already started. Please pick another slot.");
        }
        if (slot.isSoldOut()) {
            throw new InvalidInputException("This slot is sold out.");
        }

        double subtotal;
        int finalQuantity;
        String labelText = "";
        if (exp.isSeatBased()) {
            List<Seat> seats = seatManager.resolveSeats(slot.getSlotId(), seatLabels);
            subtotal = 0;
            for (Seat s : seats) {
                subtotal += s.getPrice();
            }
            finalQuantity = seats.size();
            labelText = String.join(",", seatLabels);
        } else {
            if (quantity < 1 || quantity > MAX_QUANTITY) {
                throw new InvalidInputException("Quantity must be between 1 and " + MAX_QUANTITY + ".");
            }
            if (quantity > slot.getAvailableCapacity()) {
                throw new SeatAlreadyBookedException("Only " + slot.getAvailableCapacity()
                        + " place(s) left in this slot.");
            }
            subtotal = exp.calculatePrice(quantity);
            finalQuantity = quantity;
        }
        subtotal = round(subtotal);
        double fee = exp.getServiceFee(subtotal);

        double discount = 0;
        String appliedCode = null;
        if (promoCode != null && !promoCode.trim().isEmpty()) {
            PromoCode promo = bookingDAO.findPromo(promoCode.trim().toUpperCase());
            if (promo == null) {
                throw new InvalidInputException("Promo code '" + promoCode.trim() + "' does not exist.");
            }
            if (promo.isExpired()) {
                throw new InvalidInputException("Promo code " + promo.getCode() + " has expired.");
            }
            if (promo.isExhausted()) {
                throw new InvalidInputException("Promo code " + promo.getCode() + " has been fully used.");
            }
            discount = round(subtotal * promo.getPercentOff() / 100.0);
            appliedCode = promo.getCode();
        }

        Booking b = new Booking();
        b.setUserId(userId);
        b.setUserName(userName);
        b.setSlotId(slot.getSlotId());
        b.setExperienceId(exp.getExperienceId());
        b.setExperienceTitle(exp.getTitle());
        b.setCategory(exp.getCategory());
        b.setVenue(exp.getVenue());
        b.setCity(exp.getCity());
        b.setSlotDate(slot.getSlotDate());
        b.setSlotTime(slot.getSlotTime());
        b.setQuantity(finalQuantity);
        b.setSeatLabels(labelText);
        b.setSubtotal(subtotal);
        b.setServiceFee(fee);
        b.setDiscount(discount);
        b.setPromoCode(appliedCode);
        b.setTotalAmount(round(subtotal + fee - discount));
        return b;
    }

    public Booking confirmBooking(Booking draft, List<String> seatLabels, boolean seatBased,
                                  String paymentMethod, String paymentDetail)
            throws SeatAlreadyBookedException, PaymentFailedException, DatabaseOperationException {

        Payment payment = paymentService.processPayment(draft.getTotalAmount(), paymentMethod, paymentDetail);

        Booking saved;
        try {
            if (SeatManager.isJvmLockEnabled()) {
                synchronized (SeatManager.getLock(draft.getSlotId())) {
                    saved = bookingDAO.createBooking(draft, seatLabels, seatBased, payment);
                }
            } else {
                saved = bookingDAO.createBooking(draft, seatLabels, seatBased, payment);
            }
        } catch (SeatAlreadyBookedException | DatabaseOperationException e) {
            paymentService.refund(payment.getTransactionRef(), payment.getAmount());
            AppLogger.warn("Booking rejected after payment, payment refunded: " + e.getMessage());
            throw e;
        }

        try {
            Booking fresh = bookingDAO.findById(saved.getBookingId());
            if (fresh != null) {
                saved.setCreatedAt(fresh.getCreatedAt());
            }
            saved.setReceiptPath(receiptGenerator.generateReceipt(saved, payment));
        } catch (IOException | DatabaseOperationException e) {
            AppLogger.error("Booking " + saved.getBookingRef() + " saved but receipt failed", e);
        }
        notificationService.sendBookingConfirmation(saved.getUserName(), saved.getBookingRef(),
                saved.getExperienceTitle(), saved.getTotalAmount());
        AppLogger.info("Booking confirmed " + saved.getBookingRef() + " user=" + saved.getUserId()
                + " total=" + saved.getTotalAmount());
        return saved;
    }

    public Booking cancelBooking(int bookingId, int userId)
            throws BookingNotFoundException, DatabaseOperationException {
        Booking b = bookingDAO.findById(bookingId);
        if (b == null || b.getUserId() != userId) {
            throw new BookingNotFoundException("Booking #" + bookingId + " was not found for your account.");
        }
        if (!b.isConfirmed()) {
            throw new BookingNotFoundException("Booking " + b.getBookingRef() + " is already cancelled.");
        }
        if (!BookingHistory.isCancellable(b)) {
            throw new BookingNotFoundException("Booking " + b.getBookingRef()
                    + " cannot be cancelled less than " + BookingHistory.MIN_CANCEL_HOURS + " hours before the show.");
        }
        double refund = BookingHistory.calculateRefund(b);
        bookingDAO.cancelBooking(bookingId, userId, refund);
        paymentService.refund(b.getTransactionRef(), refund);
        b.setStatus(Booking.CANCELLED);
        b.setRefundAmount(refund);
        notificationService.sendCancellation(b.getUserName(), b.getBookingRef(), refund);
        AppLogger.info("Booking cancelled " + b.getBookingRef() + " refund=" + refund);
        return b;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
