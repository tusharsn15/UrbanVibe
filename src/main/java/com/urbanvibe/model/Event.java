package com.urbanvibe.model;

public class Event extends Experience {

    public static final double SERVICE_FEE_RATE = 0.05;

    private String artist;
    private String eventType;
    private boolean outdoor;

    public Event() {
        super();
        setCategory("EVENT");
    }

    public Event(int id, String title, String city, String venue, double basePrice, boolean active,
                 String artist, String eventType, boolean outdoor) {
        super(id, title, city, venue, basePrice, "EVENT", active);
        this.artist = artist;
        this.eventType = eventType;
        this.outdoor = outdoor;
    }

    public Event(String title, String city, String venue, double basePrice,
                 String artist, String eventType, boolean outdoor) {
        this(0, title, city, venue, basePrice, true, artist, eventType, outdoor);
    }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public boolean isOutdoor() { return outdoor; }
    public void setOutdoor(boolean outdoor) { this.outdoor = outdoor; }

    @Override
    public boolean isSeatBased() {
        return true;
    }

    @Override
    public double calculatePrice(int quantity) {
        return getBasePrice() * quantity;
    }

    @Override
    public double getServiceFee(double subtotal) {
        return Math.round(subtotal * SERVICE_FEE_RATE * 100.0) / 100.0;
    }

    @Override
    public String getDetails() {
        return String.format("  Event     : %s%n  Artist    : %s%n  Type      : %s%n  Setting   : %s%n"
                        + "  Venue     : %s, %s%n  Price     : from Rs. %.2f (+5%% service fee)",
                getTitle(), artist, eventType, outdoor ? "Outdoor" : "Indoor",
                getVenue(), getCity(), getBasePrice());
    }
}
