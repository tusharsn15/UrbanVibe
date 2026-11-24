package com.urbanvibe.utility;

import com.urbanvibe.model.User;

public class UserSession {

    private static User currentUser;

    private UserSession() { }

    public static synchronized void login(User user) {
        currentUser = user;
    }

    public static synchronized void logout() {
        currentUser = null;
    }

    public static synchronized User getCurrentUser() {
        return currentUser;
    }

    public static synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    public static synchronized boolean isAdmin() {
        return currentUser != null && User.ROLE_ADMIN.equals(currentUser.getRole());
    }

    public static synchronized boolean isCustomer() {
        return currentUser != null && User.ROLE_CUSTOMER.equals(currentUser.getRole());
    }
}
