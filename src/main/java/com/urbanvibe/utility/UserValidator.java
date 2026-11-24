package com.urbanvibe.utility;

import com.urbanvibe.exception.InvalidInputException;

import java.util.regex.Pattern;

public class UserValidator {

    private static final Pattern NAME = Pattern.compile("^[A-Za-z][A-Za-z .'-]{1,49}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^[6-9]\\d{9}$");
    private static final Pattern PASSWORD = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    private UserValidator() { }

    public static void validateName(String name) throws InvalidInputException {
        if (name == null || !NAME.matcher(name.trim()).matches()) {
            throw new InvalidInputException("Name must be 2-50 letters (spaces, . ' - allowed).");
        }
    }

    public static void validateEmail(String email) throws InvalidInputException {
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new InvalidInputException("Please enter a valid email address (e.g. name@example.com).");
        }
    }

    public static void validatePassword(String password) throws InvalidInputException {
        if (password == null || !PASSWORD.matcher(password).matches()) {
            throw new InvalidInputException(
                    "Password needs 8+ characters with an uppercase letter, a lowercase letter and a digit.");
        }
    }

    public static void validatePhone(String phone) throws InvalidInputException {
        if (phone == null || !PHONE.matcher(phone.trim()).matches()) {
            throw new InvalidInputException("Phone must be a 10-digit mobile number starting with 6-9.");
        }
    }

    public static void validate(String name, String email, String password, String phone)
            throws InvalidInputException {
        validateName(name);
        validateEmail(email);
        validatePassword(password);
        validatePhone(phone);
    }

    public static void validate(String name, String phone) throws InvalidInputException {
        validateName(name);
        validatePhone(phone);
    }
}
