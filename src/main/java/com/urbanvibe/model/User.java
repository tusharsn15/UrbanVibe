package com.urbanvibe.model;

    public abstract class User {

    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_ADMIN = "ADMIN";

    private int userId;
    private String name;
    private String email;
    private String passwordHash;
    private String salt;
    private String phone;
    private String role;
    private String city;

    public User() {
        this.role = ROLE_CUSTOMER;
        this.city = "";
    }

    public User(int userId, String name, String email, String passwordHash, String salt,
                String phone, String role, String city) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.phone = phone;
        this.role = role;
        this.city = city;
    }

    public User(String name, String email, String phone, String role) {
        this(0, name, email, null, null, phone, role, "");
    }

    public abstract String getDashboardTitle();

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    @Override
    public String toString() {
        return "User{id=" + userId + ", name=" + name + ", email=" + email + ", role=" + role + "}";
    }
}
