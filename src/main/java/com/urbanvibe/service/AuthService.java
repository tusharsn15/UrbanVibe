package com.urbanvibe.service;

import com.urbanvibe.dao.UserDAO;
import com.urbanvibe.exception.DatabaseOperationException;
import com.urbanvibe.exception.InvalidInputException;
import com.urbanvibe.exception.InvalidLoginException;
import com.urbanvibe.model.Admin;
import com.urbanvibe.model.Customer;
import com.urbanvibe.model.User;
import com.urbanvibe.utility.AppLogger;
import com.urbanvibe.utility.UserSession;
import com.urbanvibe.utility.UserValidator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class AuthService {

    public static final String DEFAULT_ADMIN_EMAIL = "admin@urbanvibe.com";
    public static final String DEFAULT_ADMIN_PASSWORD = "Admin@123";
    public static final String DEMO_CUSTOMER_PASSWORD = "Customer@123";

    private final UserDAO userDAO;

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public Customer register(String name, String email, String password, String phone, String city)
            throws InvalidInputException, DatabaseOperationException {
        UserValidator.validate(name, email, password, phone);
        String cleanEmail = email.trim().toLowerCase();
        if (userDAO.emailExists(cleanEmail)) {
            throw new InvalidInputException("This email is already registered. Please log in instead.");
        }
        Customer customer = new Customer(name.trim(), cleanEmail, phone.trim(), city == null ? "" : city.trim());
        String salt = generateSalt();
        customer.setSalt(salt);
        customer.setPasswordHash(hashPassword(password, salt));
        customer.setUserId(userDAO.insertUser(customer));
        AppLogger.info("New customer registered: " + cleanEmail);
        return customer;
    }

    public User login(String email, String password, String expectedRole)
            throws InvalidLoginException, DatabaseOperationException {
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new InvalidLoginException("Email and password are required.");
        }
        User user = userDAO.findByEmail(email.trim().toLowerCase());
        if (user == null || !verifyPassword(password, user.getSalt(), user.getPasswordHash())) {
            AppLogger.warn("Failed login attempt for " + email);
            throw new InvalidLoginException("Invalid email or password.");
        }
        if (!user.getRole().equals(expectedRole)) {
            throw new InvalidLoginException("This is not a " + expectedRole.toLowerCase()
                    + " account. Please use the other login option.");
        }
        UserSession.login(user);
        AppLogger.info("Login: " + user.getEmail() + " (" + user.getRole() + ")");
        return user;
    }

    public void logout() {
        User user = UserSession.getCurrentUser();
        if (user != null) {
            AppLogger.info("Logout: " + user.getEmail());
        }
        UserSession.logout();
    }

    public User refreshCurrentUser() throws DatabaseOperationException {
        User current = UserSession.getCurrentUser();
        if (current == null) {
            return null;
        }
        User fresh = userDAO.findById(current.getUserId());
        if (fresh != null) {
            UserSession.login(fresh);
        }
        return fresh;
    }

    public void updateProfile(User user, String name, String phone, String city)
            throws InvalidInputException, DatabaseOperationException {
        UserValidator.validate(name, phone);
        user.setName(name.trim());
        user.setPhone(phone.trim());
        user.setCity(city == null ? "" : city.trim());
        userDAO.updateProfile(user);
    }

    public static String generateSalt() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available on this JVM", e);
        }
    }

    public static boolean verifyPassword(String password, String salt, String expectedHash) {
        String actual = hashPassword(password, salt);
        return MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8));
    }

    public void seedDefaultUsers() throws DatabaseOperationException {
        if (userDAO.findByEmail(DEFAULT_ADMIN_EMAIL) == null) {
            Admin admin = new Admin("System Admin", DEFAULT_ADMIN_EMAIL, "9876500000", "ADM001");
            admin.setCity("Chandigarh");
            admin.setSalt(generateSalt());
            admin.setPasswordHash(hashPassword(DEFAULT_ADMIN_PASSWORD, admin.getSalt()));
            userDAO.insertUser(admin);
            AppLogger.info("Default admin created");
        }
        String[][] demo = {
                {"Demo Customer", "customer@urbanvibe.com", "9876500001"},
                {"Demo User A", "demo1@urbanvibe.com", "9876500002"},
                {"Demo User B", "demo2@urbanvibe.com", "9876500003"},
                {"Demo User C", "demo3@urbanvibe.com", "9876500004"}
        };
        for (String[] row : demo) {
            if (userDAO.findByEmail(row[1]) == null) {
                Customer c = new Customer(row[0], row[1], row[2], "Chandigarh");
                c.setSalt(generateSalt());
                c.setPasswordHash(hashPassword(DEMO_CUSTOMER_PASSWORD, c.getSalt()));
                userDAO.insertUser(c);
            }
        }
    }
}
