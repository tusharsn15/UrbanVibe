package com.urbanvibe.menu;

import com.urbanvibe.dao.BookingDAO;
import com.urbanvibe.dao.ExperienceDAO;
import com.urbanvibe.dao.UserDAO;
import com.urbanvibe.exception.DatabaseOperationException;
import com.urbanvibe.exception.InvalidInputException;
import com.urbanvibe.exception.InvalidLoginException;
import com.urbanvibe.model.Activity;
import com.urbanvibe.model.Customer;
import com.urbanvibe.model.Event;
import com.urbanvibe.model.Experience;
import com.urbanvibe.model.Movie;
import com.urbanvibe.model.Restaurant;
import com.urbanvibe.model.Seat;
import com.urbanvibe.model.TimeSlot;
import com.urbanvibe.model.User;
import com.urbanvibe.service.AuthService;
import com.urbanvibe.service.BookingService;
import com.urbanvibe.service.ExperienceService;
import com.urbanvibe.service.SeatManager;
import com.urbanvibe.threading.BookingTask;
import com.urbanvibe.utility.AppLogger;
import com.urbanvibe.utility.ConsoleView;
import com.urbanvibe.utility.InputHelper;
import com.urbanvibe.utility.ReportGenerator;
import com.urbanvibe.utility.UserSession;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class AdminMenu {

    private final AuthService authService;
    private final ExperienceService experienceService;
    private final BookingService bookingService;
    private final SeatManager seatManager;
    private final UserDAO userDAO;
    private final BookingDAO bookingDAO;
    private final ExperienceDAO experienceDAO;
    private final ReportGenerator reportGenerator;

    public AdminMenu(AuthService authService, ExperienceService experienceService, BookingService bookingService,
                     SeatManager seatManager, UserDAO userDAO, BookingDAO bookingDAO, ExperienceDAO experienceDAO,
                     ReportGenerator reportGenerator) {
        this.authService = authService;
        this.experienceService = experienceService;
        this.bookingService = bookingService;
        this.seatManager = seatManager;
        this.userDAO = userDAO;
        this.bookingDAO = bookingDAO;
        this.experienceDAO = experienceDAO;
        this.reportGenerator = reportGenerator;
    }

    public void show() {
        boolean running = true;
        while (running && UserSession.isAdmin()) {
            ConsoleView.header(UserSession.getCurrentUser().getDashboardTitle());
            System.out.println("  1. Add Experience");
            System.out.println("  2. Update Experience");
            System.out.println("  3. Delete Experience");
            System.out.println("  4. View All Experiences");
            System.out.println("  5. Manage Seats and Slots");
            System.out.println("  6. View Customers");
            System.out.println("  7. View Bookings");
            System.out.println("  8. Generate Reports");
            System.out.println("  9. Logout");
            System.out.println();
            try {
                int choice = InputHelper.readInt("Enter your choice: ", 1, 9);
                switch (choice) {
                    case 1: addExperience(); break;
                    case 2: updateExperience(); break;
                    case 3: deleteExperience(); break;
                    case 4: viewAll(); break;
                    case 5: manageSlots(); break;
                    case 6: viewCustomers(); break;
                    case 7: viewBookings(); break;
                    case 8: reports(); break;
                    default:
                        authService.logout();
                        System.out.println("  Admin logged out.");
                        running = false;
                }
            } catch (InvalidInputException e) {
                ConsoleView.error(e.getMessage());
            } catch (InvalidLoginException e) {
                ConsoleView.error(e.getMessage());
            } catch (DatabaseOperationException e) {
                ConsoleView.error("Database problem: " + e.getMessage());
                AppLogger.error("Admin menu database error", e);
            } catch (IOException e) {
                ConsoleView.error("File problem: " + e.getMessage());
                AppLogger.error("Admin menu file error", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ConsoleView.error("Operation interrupted.");
            }
        }
    }

    private void addExperience() throws InvalidInputException, InvalidLoginException, DatabaseOperationException {
        ConsoleView.header("ADD EXPERIENCE");
        System.out.println("  1. Movie   2. Event   3. Restaurant   4. Activity");
        int type = InputHelper.readInt("Select category: ", 1, 4);
        String title = InputHelper.readRequired("Title: ");
        String city = InputHelper.readRequired("City: ");
        String venue = InputHelper.readRequired("Venue: ");
        double price = InputHelper.readDouble("Base price (Rs.): ");

        Experience exp;
        switch (type) {
            case 1:
                String language = InputHelper.readRequired("Language: ");
                String genre = InputHelper.readRequired("Genre: ");
                int duration = InputHelper.readInt("Duration (minutes): ", 1, 600);
                double rating = InputHelper.readDouble("Rating (0-10): ");
                if (rating < 0 || rating > 10) {
                    throw new InvalidInputException("Rating must be between 0 and 10.");
                }
                exp = new Movie(title, city, venue, price, language, genre, duration, rating);
                break;
            case 2:
                String artist = InputHelper.readRequired("Artist / performer: ");
                String eventType = InputHelper.readRequired("Event type (e.g. Concert): ");
                boolean outdoor = InputHelper.confirm("Outdoor event? (y/n): ");
                exp = new Event(title, city, venue, price, artist, eventType, outdoor);
                break;
            case 3:
                String cuisine = InputHelper.readRequired("Cuisine: ");
                int capacity = InputHelper.readInt("Total seating capacity: ", 1, 1000);
                exp = new Restaurant(title, city, venue, price, cuisine, capacity);
                break;
            default:
                String activityType = InputHelper.readRequired("Activity type: ");
                int minAge = InputHelper.readInt("Minimum age: ", 0, 100);
                exp = new Activity(title, city, venue, price, activityType, minAge);
        }
        int id = experienceService.addExperience(exp);
        System.out.println("  Experience created with ID " + id + ". Add time slots under 'Manage Seats and Slots'.");
    }

    private void updateExperience() throws InvalidInputException, InvalidLoginException, DatabaseOperationException {
        ConsoleView.header("UPDATE EXPERIENCE");
        ConsoleView.printExperiences(experienceService.listAll(false));
        int id = InputHelper.readInt("Experience ID to update (0 = back): ");
        if (id == 0) {
            return;
        }
        Experience exp = experienceService.getById(id);
        System.out.println("  Press Enter to keep the current value.");
        String title = InputHelper.readLine("Title [" + exp.getTitle() + "]: ");
        String city = InputHelper.readLine("City [" + exp.getCity() + "]: ");
        String venue = InputHelper.readLine("Venue [" + exp.getVenue() + "]: ");
        String price = InputHelper.readLine("Base price [" + exp.getBasePrice() + "]: ");
        String active = InputHelper.readLine("Active (y/n) [" + (exp.isActive() ? "y" : "n") + "]: ").toLowerCase();
        if (!title.isEmpty()) exp.setTitle(title);
        if (!city.isEmpty()) exp.setCity(city);
        if (!venue.isEmpty()) exp.setVenue(venue);
        if (!price.isEmpty()) {
            try {
                exp.setBasePrice(Double.parseDouble(price));
            } catch (NumberFormatException e) {
                throw new InvalidInputException("'" + price + "' is not a valid price.");
            }
        }
        if (active.equals("y")) exp.setActive(true);
        if (active.equals("n")) exp.setActive(false);
        experienceService.updateExperience(exp);
        System.out.println("  Experience updated. (Note: existing seat prices do not change.)");
    }

    private void deleteExperience() throws InvalidInputException, InvalidLoginException, DatabaseOperationException {
        ConsoleView.header("DELETE EXPERIENCE");
        ConsoleView.printExperiences(experienceService.listAll(false));
        int id = InputHelper.readInt("Experience ID to delete (0 = back): ");
        if (id == 0) {
            return;
        }
        Experience exp = experienceService.getById(id);
        if (!InputHelper.confirm("  Delete '" + exp.getTitle() + "'? (y/n): ")) {
            System.out.println("  Deletion aborted.");
            return;
        }
        if (experienceService.deleteExperience(id)) {
            System.out.println("  Experience deleted permanently.");
        } else {
            System.out.println("  It has existing bookings, so it was deactivated instead (history is preserved).");
        }
    }

    private void viewAll() throws DatabaseOperationException {
        ConsoleView.header("ALL EXPERIENCES");
        ConsoleView.printExperiences(experienceService.listAll(false));
    }

    private void manageSlots() throws InvalidInputException, InvalidLoginException, DatabaseOperationException,
            InterruptedException {
        ConsoleView.header("MANAGE SEATS AND SLOTS");
        System.out.println("  1. View slots of an experience");
        System.out.println("  2. Add a slot");
        System.out.println("  3. Delete a slot");
        System.out.println("  4. Change capacity (restaurants / activities)");
        System.out.println("  5. View seat map of a slot");
        System.out.println("  6. Run concurrent booking demo (multithreading)");
        System.out.println("  7. Back");
        int choice = InputHelper.readInt("Enter your choice: ", 1, 7);
        switch (choice) {
            case 1: {
                int id = InputHelper.readInt("Experience ID: ");
                ConsoleView.printSlots(experienceService.getSlots(id, false));
                break;
            }
            case 2: addSlot(); break;
            case 3: {
                int slotId = InputHelper.readInt("Slot ID to delete: ");
                experienceService.deleteSlot(slotId);
                System.out.println("  Slot deleted.");
                break;
            }
            case 4: {
                int slotId = InputHelper.readInt("Slot ID: ");
                int capacity = InputHelper.readInt("New total capacity: ");
                experienceService.updateCapacity(slotId, capacity);
                System.out.println("  Capacity updated.");
                break;
            }
            case 5: {
                int slotId = InputHelper.readInt("Slot ID: ");
                List<Seat> seats = seatManager.getAllSeats(slotId);
                if (seats.isEmpty()) {
                    System.out.println("  This slot has no seat map (capacity-based or unknown slot).");
                } else {
                    seatManager.displaySeatMap(seats);
                }
                break;
            }
            case 6: concurrencyDemo(); break;
            default:
                break;
        }
    }

    private void addSlot() throws InvalidInputException, InvalidLoginException, DatabaseOperationException {
        int expId = InputHelper.readInt("Experience ID: ");
        Experience exp = experienceService.getById(expId);
        LocalDate date;
        LocalTime time;
        try {
            date = LocalDate.parse(InputHelper.readRequired("Date (yyyy-mm-dd): "));
            time = LocalTime.parse(InputHelper.readRequired("Time (HH:mm): "));
        } catch (DateTimeParseException e) {
            throw new InvalidInputException("Invalid date or time format.");
        }
        int slotId;
        if (exp.isSeatBased()) {
            int rows = InputHelper.readInt("Number of rows (1-10): ");
            int perRow = InputHelper.readInt("Seats per row (1-15): ");
            slotId = experienceService.addSlot(expId, date, time, rows, perRow);
        } else {
            int capacity = InputHelper.readInt("Capacity (guests/participants): ");
            slotId = experienceService.addSlot(expId, date, time, capacity);
        }
        System.out.println("  Slot created with ID " + slotId + ".");
    }

    private void concurrencyDemo() throws InvalidInputException, DatabaseOperationException, InterruptedException {
        ConsoleView.header("CONCURRENT BOOKING DEMO");
        System.out.println("  Several customers book at the same moment using separate threads.");
        System.out.println("  Real bookings, payments and receipts are created for the winners.");
        System.out.println();
        System.out.println("  1. Race: 2 customers try to book the SAME seat");
        System.out.println("  2. Race: 3 customers try to book the SAME seat (JVM lock OFF - database decides)");
        System.out.println("  3. Parallel: 3 customers book DIFFERENT seats at the same time");
        int mode = InputHelper.readInt("Choose demo: ", 1, 3);

        int slotId = InputHelper.readInt("Seat-based slot ID (e.g. a movie slot): ");
        TimeSlot slot = experienceDAO.findSlotById(slotId);
        if (slot == null) {
            throw new InvalidInputException("No slot with ID " + slotId + ".");
        }
        Experience exp = experienceService.getById(slot.getExperienceId());
        if (!exp.isSeatBased()) {
            throw new InvalidInputException("The demo needs a seat-based slot (movie or event).");
        }
        List<Seat> seats = seatManager.getAvailableSeats(slotId);
        int needed = mode == 3 ? 3 : 1;
        if (seats.size() < needed) {
            throw new InvalidInputException("Not enough free seats in this slot for the demo.");
        }

        String[] emails = {"demo1@urbanvibe.com", "demo2@urbanvibe.com", "demo3@urbanvibe.com"};
        int userCount = mode == 1 ? 2 : 3;
        List<User> users = new ArrayList<>();
        for (int i = 0; i < userCount; i++) {
            User u = userDAO.findByEmail(emails[i]);
            if (u == null) {
                throw new InvalidInputException("Demo customer " + emails[i] + " is missing.");
            }
            users.add(u);
        }

        List<List<String>> seatPlan = new ArrayList<>();
        for (int i = 0; i < userCount; i++) {
            List<String> mine = new ArrayList<>();
            mine.add(mode == 3 ? seats.get(i).getLabel() : seats.get(0).getLabel());
            seatPlan.add(mine);
        }
        boolean lockOn = mode != 2;
        System.out.println();
        System.out.println("  Starting " + userCount + " threads. JVM synchronized lock: " + (lockOn ? "ON" : "OFF"));
        for (int i = 0; i < userCount; i++) {
            System.out.println("    " + users.get(i).getName() + " wants seat " + seatPlan.get(i));
        }
        System.out.println();

        List<BookingTask> tasks = BookingTask.runConcurrently(bookingService, exp, slot, users, seatPlan, lockOn);

        int winners = 0;
        System.out.println("  ---- Results ----");
        for (BookingTask t : tasks) {
            System.out.println("  " + t.describe());
            if (t.isSuccess()) {
                winners++;
            }
        }
        System.out.println();
        if (mode == 3) {
            System.out.println("  " + winners + " of " + userCount + " bookings succeeded (different seats never conflict).");
        } else {
            System.out.println("  " + winners + " of " + userCount + " succeeded. Exactly one winner = no double booking.");
        }
        System.out.println("  The database (row locks + UNIQUE seat constraint) made the final decision.");
    }

    private void viewCustomers() throws DatabaseOperationException {
        ConsoleView.header("CUSTOMERS");
        List<Customer> customers = userDAO.getAllCustomers();
        if (customers.isEmpty()) {
            System.out.println("  No customers registered yet.");
            return;
        }
        System.out.println(String.format("  %-4s %-20s %-28s %-12s %-12s %s",
                "ID", "Name", "Email", "Phone", "City", "Points"));
        for (Customer c : customers) {
            System.out.println(String.format("  %-4d %-20s %-28s %-12s %-12s %d",
                    c.getUserId(), c.getName(), c.getEmail(), c.getPhone(), c.getCity(), c.getLoyaltyPoints()));
        }
    }

    private void viewBookings() throws DatabaseOperationException {
        ConsoleView.header("ALL BOOKINGS");
        ConsoleView.printBookings(bookingDAO.findAll(), true);
    }

    private void reports() throws DatabaseOperationException, IOException, InvalidInputException {
        ConsoleView.header("REPORTS");
        System.out.println("  1. Revenue by category");
        System.out.println("  2. Bookings per experience");
        System.out.println("  3. Occupancy by category");
        System.out.println("  4. Generate full report file (reports/)");
        int choice = InputHelper.readInt("Enter your choice: ", 1, 4);
        System.out.println();
        switch (choice) {
            case 1: System.out.println(reportGenerator.revenueReport()); break;
            case 2: System.out.println(reportGenerator.bookingsPerExperienceReport()); break;
            case 3: System.out.println(reportGenerator.occupancyReport()); break;
            default:
                String path = reportGenerator.saveFullReport();
                System.out.println("  Full report saved to: " + path);
        }
    }
}
