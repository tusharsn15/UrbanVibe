package com.urbanvibe.service;

import com.urbanvibe.exception.PaymentFailedException;
import com.urbanvibe.model.Payment;

public interface PaymentProcessor {

    Payment processPayment(double amount, String method, String detail) throws PaymentFailedException;

    boolean refund(String transactionRef, double amount);
}
