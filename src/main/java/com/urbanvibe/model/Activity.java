package com.urbanvibe.model;

public class Activity extends Experience {

    public static final int GROUP_SIZE = 4;
    public static final double GROUP_DISCOUNT = 0.10;

    private String activityType;
    private int minAge;

    public Activity() {
        super();
        setCategory("ACTIVITY");
    }

    public Activity(int id, String title, String city, String venue, double basePrice, boolean active,
                    String activityType, int minAge) {
        super(id, title, city, venue, basePrice, "ACTIVITY", active);
        this.activityType = activityType;
        this.minAge = minAge;
    }

    public Activity(String title, String city, String venue, double basePrice,
                    String activityType, int minAge) {
        this(0, title, city, venue, basePrice, true, activityType, minAge);
    }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }
    public int getMinAge() { return minAge; }
    public void setMinAge(int minAge) { this.minAge = minAge; }

    @Override
    public boolean isSeatBased() {
        return false;
    }

    @Override
    public double calculatePrice(int participants) {
        double total = getBasePrice() * participants;
        if (participants >= GROUP_SIZE) {
            total = total * (1 - GROUP_DISCOUNT);
        }
        return Math.round(total * 100.0) / 100.0;
    }

    @Override
    public String getDetails() {
        return String.format("  Activity  : %s%n  Type      : %s%n  Min age   : %d+%n"
                        + "  Location  : %s, %s%n  Price     : Rs. %.2f per person (10%% off for groups of %d+)",
                getTitle(), activityType, minAge, getVenue(), getCity(), getBasePrice(), GROUP_SIZE);
    }
}
