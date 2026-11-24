package com.urbanvibe.model;

import java.util.Objects;

public class Seat {

    private int seatId;
    private int slotId;
    private String label;
    private String type;
    private double price;
    private boolean booked;

    public Seat() { }

    public Seat(int seatId, int slotId, String label, String type, double price, boolean booked) {
        this.seatId = seatId;
        this.slotId = slotId;
        this.label = label;
        this.type = type;
        this.price = price;
        this.booked = booked;
    }

    public Seat(int seatId, int slotId, String label, String type, double price) {
        this(seatId, slotId, label, type, price, false);
    }

    public int getSeatId() { return seatId; }
    public void setSeatId(int seatId) { this.seatId = seatId; }
    public int getSlotId() { return slotId; }
    public void setSlotId(int slotId) { this.slotId = slotId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public boolean isBooked() { return booked; }
    public void setBooked(boolean booked) { this.booked = booked; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Seat)) return false;
        Seat other = (Seat) o;
        return slotId == other.slotId && Objects.equals(label, other.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slotId, label);
    }

    @Override
    public String toString() {
        return label + " (" + type + ", Rs. " + price + (booked ? ", BOOKED" : "") + ")";
    }
}
