package com.urbanvibe.model;

public class Movie extends Experience {

    private String language;
    private String genre;
    private int durationMins;
    private double rating;

    public Movie() {
        super();
        setCategory("MOVIE");
    }

    public Movie(int id, String title, String city, String venue, double basePrice, boolean active,
                 String language, String genre, int durationMins, double rating) {
        super(id, title, city, venue, basePrice, "MOVIE", active);
        this.language = language;
        this.genre = genre;
        this.durationMins = durationMins;
        this.rating = rating;
    }

    public Movie(String title, String city, String venue, double basePrice,
                 String language, String genre, int durationMins, double rating) {
        this(0, title, city, venue, basePrice, true, language, genre, durationMins, rating);
    }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public int getDurationMins() { return durationMins; }
    public void setDurationMins(int durationMins) { this.durationMins = durationMins; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    @Override
    public boolean isSeatBased() {
        return true;
    }

    @Override
    public double calculatePrice(int quantity) {
        return getBasePrice() * quantity;
    }

    public double calculatePrice(int quantity, double seatMultiplier) {
        return getBasePrice() * seatMultiplier * quantity;
    }

    @Override
    public String getDetails() {
        return String.format("  Movie     : %s%n  Language  : %s%n  Genre     : %s%n  Duration  : %d min%n"
                        + "  Rating    : %.1f/10%n  Venue     : %s, %s%n  Price     : from Rs. %.2f",
                getTitle(), language, genre, durationMins, rating, getVenue(), getCity(), getBasePrice());
    }
}
