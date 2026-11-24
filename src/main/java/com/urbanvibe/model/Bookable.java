package com.urbanvibe.model;

public interface Bookable {

boolean isSeatBased();

double calculatePrice(int quantity);

String getBookingLabel();
}
