package com.urbanvibe.menu;

import com.urbanvibe.api.ApiService;
import com.urbanvibe.exception.BookingNotFoundException;
import com.urbanvibe.exception.DatabaseOperationException;
import com.urbanvibe.exception.InvalidInputException;
import com.urbanvibe.exception.PaymentFailedException;
import com.urbanvibe.exception.SeatAlreadyBookedException;
import com.urbanvibe.model.Booking;
import com.urbanvibe.model.Customer;
import com.urbanvibe.model.Event;
import com.urbanvibe.model.Experience;
import com.urbanvibe.model.Seat;
import com.urbanvibe.model.TimeSlot;
import com.urbanvibe.model.User;
import com.urbanvibe.service.AuthService;
import com.urbanvibe.service.BookingHistory;
import com.urbanvibe.service.BookingService;
import com.urbanvibe.service.ExperienceService;
import com.urbanvibe.service.SearchService;
import com.urbanvibe.service.SeatManager;
import com.urbanvibe.utility.AppLogger;
import com.urbanvibe.utility.ConsoleView;
import com.urbanvibe.utility.InputHelper;
import com.urbanvibe.utility.UserSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CustomerMenu {

    private final AuthService authService;
    private final ExperienceService experienceService;
    private final SearchService searchService;
    private final BookingService bookingService;
    private final BookingHistory bookingHistory;
    private final SeatManager seatManager;
    private final ApiService apiService;

    public CustomerMenu(AuthService authService, ExperienceService experienceService, SearchService searchService,
                        BookingService bookingService, BookingHistory bookingHistory, SeatManager seatManager,
                        ApiService apiService) {
        this.authService = authService;
        this.experienceService = experienceService;
        this.searchService = searchService;
        this.bookingService = bookingService;
        this.bookingHistory = bookingHistory;
        this.seatManager = seatManager;
        this.apiService = apiService;
    }

    public void show() {
        boolean running = true;
        while (running && UserSession.isLoggedIn()) {
            ConsoleView.header(UserSession.getCurrentUser().getDashboardTitle());
            System.out.println("  1. Explore Movies");
            System.out.println("  2. Explore Events");
            System.out.println("  3. Explore Restaurants");
            System.out.println("  4. Explore Activities");
            System.out.println("  5. Search Experiences");
            System.out.println("  6. Book Experience");
            System.out.println("  7. View Booking History");
            System.out.println("  8. Cancel Booking");
            System.out.println("  9. View Profile");
            System.out.println("  10. Logout");
            System.out.println();
            try {
                int choice = InputHelper.readInt("Enter your choice: ", 1, 10);
                switch (choice) {
                    case 1: explore("MOVIE"); break;
                    case 2: explore("EVENT"); break;
                    case 3: explore("RESTAURANT"); break;
                    case 4: explore("ACTIVITY"); break;
                    case 5: search(); break;
                    case 6: bookByCategory(); break;
                    case 7: viewHistory(); break;
                    case 8: cancelBooking(); break;
                    case 9: viewProfile(); break;
                    default:
                        authService.logout();
                        System.out.println("  You have been logged out. See you soon!");
                        running = false;
                }
            } catch (InvalidInputException e) {
                ConsoleView.error(e.getMessage());
            } catch (SeatAlreadyBookedException e) {
                ConsoleView.error("Booking not completed: " + e.getMessage());
            } catch (PaymentFailedException e) {
                ConsoleView.error("Payment failed: " + e.getMessage() + " You have not been charged.");
            } catch (BookingNotFoundException e) {
                ConsoleView.error(e.getMessage());
            } catch (DatabaseOperationException e) {
                ConsoleView.error("Database problem: " + e.getMessage());
                AppLogger.error("Customer menu database error", e);
            } catch (IOException e) {
                ConsoleView.error("File problem: " + e.getMessage());
                AppLogger.error("Customer menu file error", e);
            }
        }
    }

    private void explore(String category) throws DatabaseOperationException, InvalidInputException,
            SeatAlreadyBookedException, PaymentFailedException {
        ConsoleView.header(category + " - available experiences");
        List<Experience> list = experienceService.listByCategory(category);
        ConsoleView.printExperiences(list);
        selectAndOpen(list);
    }

    private void selectAndOpen(List<Experience> list) throws DatabaseOperationException, InvalidInputException,
            SeatAlreadyBookedException, PaymentFailedException {
        if (list.isEmpty()) {
            return;
        }
        System.out.println();
        int id = InputHelper.readInt("Enter experience ID for details (0 = back): ");
        if (id == 0) {
            return;
        }
        Experience chosen = null;
        for (Experience e : list) {
            if (e.getExperienceId() == id) {
                chosen = e;
            }
        }
        if (chosen == null) {
            throw new InvalidInputException("ID " + id + " is not in the list shown above.");
        }
        showDetails(chosen);
        if (InputHelper.confirm("\n  Book this experience now? (y/n): ")) {
            bookFlow(chosen);
        }
    }

    private void showDetails(Experience exp) throws DatabaseOperationException {
        ConsoleView.header(exp.getTitle());
        System.out.println(exp.getDetails());
        if (exp instanceof Event && ((Event) exp).isOutdoor()) {
            System.out.println("  " + apiService.getWeather(exp.getCity()));
        }
        System.out.println();
        ConsoleView.printSlots(experienceService.getSlots(exp.getExperienceId(), true));
    }

    private void search() throws DatabaseOperationException, InvalidInputException, SeatAlreadyBookedException,
            PaymentFailedException {
        ConsoleView.header("SEARCH EXPERIENCES (CHANDIGARH)");
        System.out.println("  1. By keyword (title, venue)");
        System.out.println("  2. By category");
        System.out.println("  3. By price range");
        int option = InputHelper.readInt("Choose search type: ", 1, 3);
        List<Experience> results;
        if (option == 1) {
            results = searchService.search(InputHelper.readRequired("Keyword: "));
        } else if (option == 2) {
            String category = InputHelper.readLine("Category (MOVIE/EVENT/RESTAURANT/ACTIVITY, Enter = any): ");
            results = searchService.search(category, "Chandigarh");
        } else {
            double min = InputHelper.readDouble("Minimum price: ");
            double max = InputHelper.readDouble("Maximum price: ");
            results = searchService.search(min, max);
        }
        String sort = InputHelper.readLine("Sort by (p = price, t = title, Enter = none): ").toLowerCase();
        if (sort.equals("p")) {
            searchService.sortByPrice(results);
        } else if (sort.equals("t")) {
            searchService.sortByTitle(results);
        }
        System.out.println();
        System.out.println("  " + results.size() + " result(s):");
        ConsoleView.printExperiences(results);
        selectAndOpen(results);
    }

    private void bookByCategory() throws DatabaseOperationException, InvalidInputException,
            SeatAlreadyBookedException, PaymentFailedException {
        ConsoleView.header("BOOK AN EXPERIENCE");
        System.out.println("  1. Movie   2. Event   3. Restaurant   4. Activity");
        int c = InputHelper.readInt("Select category: ", 1, 4);
        String[] categories = {"MOVIE", "EVENT", "RESTAURANT", "ACTIVITY"};
        List<Experience> list = experienceService.listByCategory(categories[c - 1]);
        ConsoleView.printExperiences(list);
        if (list.isEmpty()) {
            return;
        }
        int id = InputHelper.readInt("Enter experience ID to book (0 = back): ");
        if (id == 0) {
            return;
        }
        Experience exp = experienceService.getById(id);
        if (!exp.isActive() || !exp.getCategory().equals(categories[c - 1])) {
            throw new InvalidInputException("ID " + id + " is not available in this category.");
        }
        bookFlow(exp);
    }

    private void bookFlow(Experience exp) throws DatabaseOperationException, InvalidInputException,
            SeatAlreadyBookedException, PaymentFailedException {
        User user = UserSession.getCurrentUser();
        ConsoleView.header("BOOKING: " + exp.getTitle());
        List<TimeSlot> slots = experienceService.getSlots(exp.getExperienceId(), true);
        ConsoleView.printSlots(slots);
        if (slots.isEmpty()) {
            return;
        }
        int slotId = InputHelper.readInt("\nEnter slot ID (0 = cancel): ");
        if (slotId == 0) {
            return;
        }
        TimeSlot slot = null;
        for (TimeSlot s : slots) {
            if (s.getSlotId() == slotId) {
                slot = s;
            }
        }
        if (slot == null) {
            throw new InvalidInputException("Slot " + slotId + " is not in the list shown above.");
        }
        if (slot.isSoldOut()) {
            throw new InvalidInputException("That slot is sold out.");
        }

        List<String> labels = new ArrayList<>();
        int quantity = 0;
        if (exp.isSeatBased()) {
            List<Seat> seats = seatManager.getAllSeats(slotId);
            seatManager.displaySeatMap(seats);
            labels = seatManager.parseLabels(InputHelper.readRequired(
                    "\nEnter seat(s), comma separated (e.g. A1,A2): "));
        } else {
            quantity = InputHelper.readInt("Number of guests / participants (1-" + BookingService.MAX_QUANTITY + "): ",
                    1, BookingService.MAX_QUANTITY);
        }
        String promo = InputHelper.readLine("Promo code (press Enter to skip): ");

        Booking draft = bookingService.createDraft(user.getUserId(), user.getName(), exp, slot, labels, quantity, promo);
        printSummary(draft);
        if (!InputHelper.confirm("\nConfirm and pay? (y/n): ")) {
            System.out.println("  Booking cancelled. Nothing was charged.");
            return;
        }

        System.out.println("  Payment method: 1. Card   2. UPI   3. Wallet");
        int m = InputHelper.readInt("Select payment method: ", 1, 3);
        String method;
        String detail = "";
        if (m == 1) {
            method = "CARD";
            detail = InputHelper.readRequired("Card number (16 digits, simulated): ");
        } else if (m == 2) {
            method = "UPI";
            detail = InputHelper.readRequired("UPI id (e.g. name@bank): ");
        } else {
            method = "WALLET";
        }
        System.out.println("  Processing payment...");
        Booking saved = bookingService.confirmBooking(draft, labels, exp.isSeatBased(), method, detail);

        ConsoleView.header("BOOKING CONFIRMED");
        System.out.println("  Booking ID  : " + saved.getBookingRef());
        System.out.println("  Experience  : " + saved.getExperienceTitle());
        System.out.println("  When        : " + saved.getSlotDate() + " " + saved.getSlotTime());
        if (!saved.getSeatLabels().isEmpty()) {
            System.out.println("  Seats       : " + saved.getSeatLabels());
        }
        System.out.println("  Quantity    : " + saved.getQuantity());
        System.out.println("  Total paid  : " + ConsoleView.money(saved.getTotalAmount()));
        if (saved.getReceiptPath() != null) {
            System.out.println("  Receipt     : " + saved.getReceiptPath());
        }
    }

    private void printSummary(Booking b) {
        ConsoleView.header("BOOKING SUMMARY");
        System.out.println("  Experience : " + b.getExperienceTitle() + " (" + b.getCategory() + ")");
        System.out.println("  Venue      : " + b.getVenue() + ", " + b.getCity());
        System.out.println("  Date/Time  : " + b.getSlotDate() + " " + b.getSlotTime());
        if (!b.getSeatLabels().isEmpty()) {
            System.out.println("  Seats      : " + b.getSeatLabels());
        }
        System.out.println("  Quantity   : " + b.getQuantity());
        System.out.println("  Subtotal   : " + ConsoleView.money(b.getSubtotal()));
        if (b.getServiceFee() > 0) {
            System.out.println("  Service fee: " + ConsoleView.money(b.getServiceFee()));
        }
        if (b.getDiscount() > 0) {
            System.out.println("  Discount   : -" + ConsoleView.money(b.getDiscount()) + " (" + b.getPromoCode() + ")");
        }
        System.out.println("  TOTAL      : " + ConsoleView.money(b.getTotalAmount()));
    }

    private void viewHistory() throws DatabaseOperationException, IOException {
        User user = UserSession.getCurrentUser();
        ConsoleView.header("YOUR BOOKING HISTORY");
        List<Booking> list = bookingHistory.getHistory(user.getUserId());
        bookingHistory.printHistory(list);
        if (!list.isEmpty() && InputHelper.confirm("\n  Export this history to a CSV file? (y/n): ")) {
            System.out.println("  Exported to: " + bookingHistory.exportHistory(user.getUserId(), list));
        }
    }

    private void cancelBooking() throws DatabaseOperationException, InvalidInputException, BookingNotFoundException {
        User user = UserSession.getCurrentUser();
        ConsoleView.header("CANCEL A BOOKING");
        List<Booking> list = bookingHistory.getHistory(user.getUserId());
        List<Booking> active = new ArrayList<>();
        for (Booking b : list) {
            if (b.isConfirmed()) {
                active.add(b);
            }
        }
        if (active.isEmpty()) {
            System.out.println("  You have no active bookings.");
            return;
        }
        ConsoleView.printBookings(active, false);
        System.out.println("\n  Refund policy: 24h+ before show = 100%, 2h-24h = 50%, under 2h = not cancellable.");
        int id = InputHelper.readInt("Enter booking ID to cancel (0 = back): ");
        if (id == 0) {
            return;
        }
        Booking target = null;
        for (Booking b : active) {
            if (b.getBookingId() == id) {
                target = b;
            }
        }
        if (target == null) {
            throw new BookingNotFoundException("Booking ID " + id + " is not one of your active bookings.");
        }
        if (!BookingHistory.isCancellable(target)) {
            throw new BookingNotFoundException(target.getBookingRef() + " is too close to show time to cancel.");
        }
        System.out.println("  Refund for " + target.getBookingRef() + ": "
                + ConsoleView.money(BookingHistory.calculateRefund(target))
                + " (" + BookingHistory.getRefundPercent(target) + "%)");
        if (!InputHelper.confirm("  Cancel this booking? (y/n): ")) {
            System.out.println("  Cancellation aborted.");
            return;
        }
        Booking done = bookingService.cancelBooking(id, user.getUserId());
        System.out.println("  Booking " + done.getBookingRef() + " cancelled. Refund: "
                + ConsoleView.money(done.getRefundAmount()));
    }

    private void viewProfile() throws InvalidInputException, DatabaseOperationException {
        User user = authService.refreshCurrentUser();
        ConsoleView.header("YOUR PROFILE");
        if (user instanceof Customer) {
            System.out.println(((Customer) user).viewProfile());
        }
        if (InputHelper.confirm("\n  Edit profile? (y/n): ")) {
            String name = InputHelper.readRequired("New name [" + user.getName() + "]: ");
            String phone = InputHelper.readRequired("New phone [" + user.getPhone() + "]: ");
            String city = InputHelper.readLine("New city [" + user.getCity() + "]: ");
            if (city.isEmpty()) {
                city = "Chandigarh";
            }
            authService.updateProfile(user, name, phone, city);
            System.out.println("  Profile updated.");
        }
    }
}
