package com.urbanvibe.model;

public class Customer extends User {

    private int loyaltyPoints;

    public Customer() {
        super();
        setRole(ROLE_CUSTOMER);
    }

    public Customer(int userId, String name, String email, String passwordHash, String salt,
                    String phone, String city, int loyaltyPoints) {
        super(userId, name, email, passwordHash, salt, phone, ROLE_CUSTOMER, city);
        this.loyaltyPoints = loyaltyPoints;
    }

    public Customer(String name, String email, String phone, String city) {
        this(0, name, email, null, null, phone, city, 0);
    }

    public int getLoyaltyPoints() { return loyaltyPoints; }
    public void setLoyaltyPoints(int loyaltyPoints) { this.loyaltyPoints = loyaltyPoints; }

    public void addLoyaltyPoints(int points) {
        if (points > 0) {
            this.loyaltyPoints += points;
        }
    }

    @Override
    public String getDashboardTitle() {
        return "CUSTOMER DASHBOARD - Welcome, " + getName();
    }

    public String viewProfile() {
        StringBuilder sb = new StringBuilder();
        sb.append("  Customer ID    : ").append(getUserId()).append('\n');
        sb.append("  Name           : ").append(getName()).append('\n');
        sb.append("  Email          : ").append(getEmail()).append('\n');
        sb.append("  Phone          : ").append(getPhone()).append('\n');
        sb.append("  City           : ").append(getCity()).append('\n');
        sb.append("  Loyalty points : ").append(loyaltyPoints);
        return sb.toString();
    }
}
