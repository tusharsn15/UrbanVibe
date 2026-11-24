package com.urbanvibe.model;

public class Restaurant extends Experience {

    private String cuisine;
    private int totalCapacity;

    public Restaurant() {
        super();
        setCategory("RESTAURANT");
    }

    public Restaurant(int id, String title, String city, String venue, double basePrice, boolean active,
                      String cuisine, int totalCapacity) {
        super(id, title, city, venue, basePrice, "RESTAURANT", active);
        this.cuisine = cuisine;
        this.totalCapacity = totalCapacity;
    }

    public Restaurant(String title, String city, String venue, double basePrice,
                      String cuisine, int totalCapacity) {
        this(0, title, city, venue, basePrice, true, cuisine, totalCapacity);
    }

    public String getCuisine() { return cuisine; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }
    public int getTotalCapacity() { return totalCapacity; }
    public void setTotalCapacity(int totalCapacity) { this.totalCapacity = totalCapacity; }

    @Override
    public boolean isSeatBased() {
        return false;
    }

    @Override
    public double calculatePrice(int guests) {
        return getBasePrice() * guests;
    }

    @Override
    public String getDetails() {
        return String.format("  Restaurant: %s%n  Cuisine   : %s%n  Capacity  : %d guests%n"
                        + "  Location  : %s, %s%n  Cover     : Rs. %.2f per guest",
                getTitle(), cuisine, totalCapacity, getVenue(), getCity(), getBasePrice());
    }
}
