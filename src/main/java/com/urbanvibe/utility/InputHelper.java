package com.urbanvibe.utility;

import com.urbanvibe.exception.InvalidInputException;

import java.util.Scanner;

public class InputHelper {

    private static final Scanner SCANNER = new Scanner(System.in);

    private InputHelper() { }

    public static String readLine(String prompt) {
        System.out.print(prompt);
        if (!SCANNER.hasNextLine()) {
            throw new IllegalStateException("Input closed");
        }
        return SCANNER.nextLine().trim();
    }

    public static String readRequired(String prompt) throws InvalidInputException {
        String value = readLine(prompt);
        if (value.isEmpty()) {
            throw new InvalidInputException("This field cannot be empty.");
        }
        return value;
    }

    public static int readInt(String prompt) throws InvalidInputException {
        String text = readLine(prompt);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new InvalidInputException("'" + text + "' is not a valid whole number.");
        }
    }

    public static int readInt(String prompt, int min, int max) throws InvalidInputException {
        int value = readInt(prompt);
        if (value < min || value > max) {
            throw new InvalidInputException("Please enter a number between " + min + " and " + max + ".");
        }
        return value;
    }

    public static double readDouble(String prompt) throws InvalidInputException {
        String text = readLine(prompt);
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new InvalidInputException("'" + text + "' is not a valid number.");
        }
    }

    public static boolean confirm(String prompt) {
        String answer = readLine(prompt).toLowerCase();
        return answer.equals("y") || answer.equals("yes");
    }
}
