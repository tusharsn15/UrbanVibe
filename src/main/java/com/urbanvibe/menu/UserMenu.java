package com.urbanvibe.menu;

import com.urbanvibe.exception.DatabaseOperationException;
import com.urbanvibe.exception.InvalidInputException;
import com.urbanvibe.exception.InvalidLoginException;
import com.urbanvibe.model.Customer;
import com.urbanvibe.model.Experience;
import com.urbanvibe.model.User;
import com.urbanvibe.service.AuthService;
import com.urbanvibe.service.ExperienceService;
import com.urbanvibe.utility.AppLogger;
import com.urbanvibe.utility.ConsoleView;
import com.urbanvibe.utility.InputHelper;

import java.util.List;

public class UserMenu {

    private final AuthService authService;
    private final ExperienceService experienceService;
    private final CustomerMenu customerMenu;
    private final AdminMenu adminMenu;

    public UserMenu(AuthService authService, ExperienceService experienceService,
                    CustomerMenu customerMenu, AdminMenu adminMenu) {
        this.authService = authService;
        this.experienceService = experienceService;
        this.customerMenu = customerMenu;
        this.adminMenu = adminMenu;
    }

    public void showMainMenu() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println(ConsoleView.LINE);
            System.out.println("               URBANVIBE");
            System.out.println("     Entertainment & Experience Booking");
            System.out.println(ConsoleView.LINE);
            System.out.println();
            System.out.println("1. Register");
            System.out.println("2. Customer Login");
            System.out.println("3. Admin Login");
            System.out.println("4. Browse Experiences");
            System.out.println("5. Exit");
            System.out.println();
            try {
                int choice = InputHelper.readInt("Enter your choice: ", 1, 5);
                switch (choice) {
                    case 1: registerFlow(); break;
                    case 2:
                        if (loginFlow(User.ROLE_CUSTOMER)) {
                            customerMenu.show();
                        }
                        break;
                    case 3:
                        if (loginFlow(User.ROLE_ADMIN)) {
                            adminMenu.show();
                        }
                        break;
                    case 4: browseAsGuest(); break;
                    default:
                        System.out.println("  Thank you for using UrbanVibe. Goodbye!");
                        running = false;
                }
            } catch (InvalidInputException e) {
                ConsoleView.error(e.getMessage());
            } catch (InvalidLoginException e) {
                ConsoleView.error("Login failed: " + e.getMessage());
            } catch (DatabaseOperationException e) {
                ConsoleView.error("Database problem: " + e.getMessage());
                AppLogger.error("Main menu database error", e);
            }
        }
    }

    private void registerFlow() throws InvalidInputException, DatabaseOperationException {
        ConsoleView.header("REGISTER NEW CUSTOMER");
        String name = InputHelper.readRequired("Full name: ");
        String email = InputHelper.readRequired("Email: ");
        String password = InputHelper.readRequired("Password (8+ chars, upper, lower, digit): ");
        String phone = InputHelper.readRequired("Phone (10 digits): ");
        String city = InputHelper.readLine("City: ");
        Customer c = authService.register(name, email, password, phone, city);
        System.out.println("  Registration successful! Your customer ID is " + c.getUserId() + ". Please log in.");
    }

    private boolean loginFlow(String role) throws InvalidLoginException, DatabaseOperationException {
        ConsoleView.header(role.equals(User.ROLE_ADMIN) ? "ADMIN LOGIN" : "CUSTOMER LOGIN");
        String email = InputHelper.readLine("Email: ");
        String password = InputHelper.readLine("Password: ");
        User user = authService.login(email, password, role);
        System.out.println("  Login successful. Welcome, " + user.getName() + "!");
        return true;
    }

    private void browseAsGuest() throws DatabaseOperationException {
        ConsoleView.header("BROWSE EXPERIENCES (guest)");
        List<Experience> all = experienceService.listAll(true);
        ConsoleView.printExperiences(all);
        System.out.println();
        System.out.println("  Log in as a customer to see time slots and to book.");
    }
}
