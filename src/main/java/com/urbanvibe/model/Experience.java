package com.urbanvibe.model;

    public abstract class Experience implements Bookable {

    private int experienceId;
    private String title;
    private String city;
    private String venue;
    private double basePrice;
    private String category;
    private boolean active;

    public Experience() {
        this.active = true;
    }

    public Experience(int experienceId, String title, String city, String venue,
                      double basePrice, String category, boolean active) {
        this.experienceId = experienceId;
        this.title = title;
        this.city = city;
        this.venue = venue;
        this.basePrice = basePrice;
        this.category = category;
        this.active = active;
    }

    public abstract String getDetails();

    public double getServiceFee(double subtotal) {
        return 0.0;
    }

    @Override
    public String getBookingLabel() {
        return category + ": " + title;
    }

    public int getExperienceId() { return experienceId; }
    public void setExperienceId(int experienceId) { this.experienceId = experienceId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public double getBasePrice() { return basePrice; }
    public void setBasePrice(double basePrice) { this.basePrice = basePrice; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return String.format("[%d] %s (%s) - %s, %s | from Rs. %.2f%s",
                experienceId, title, category, venue, city, basePrice, active ? "" : " [INACTIVE]");
    }
}
