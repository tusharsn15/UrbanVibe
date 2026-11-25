package com.urbanvibe;

import com.urbanvibe.api.ApiService;
import com.urbanvibe.dao.BookingDAO;
import com.urbanvibe.dao.ExperienceDAO;
import com.urbanvibe.dao.UserDAO;
import com.urbanvibe.database.DatabaseConnection;
import com.urbanvibe.exception.DatabaseOperationException;
import com.urbanvibe.menu.AdminMenu;
import com.urbanvibe.menu.CustomerMenu;
import com.urbanvibe.menu.UserMenu;
import com.urbanvibe.service.AuthService;
import com.urbanvibe.service.BookingHistory;
import com.urbanvibe.service.BookingService;
import com.urbanvibe.service.ExperienceService;
import com.urbanvibe.service.PaymentService;
import com.urbanvibe.service.SearchService;
import com.urbanvibe.service.SeatManager;
import com.urbanvibe.threading.NotificationService;
import com.urbanvibe.utility.AppLogger;
import com.urbanvibe.utility.FileManager;
import com.urbanvibe.utility.ReceiptGenerator;
import com.urbanvibe.utility.ReportGenerator;

public class Main {

    public static void main(String[] args) {
        FileManager.ensureDirectories();
        AppLogger.start();
        DatabaseConnection.loadConfig();

        NotificationService notificationService = new NotificationService();
        try {
            if (!DatabaseConnection.testConnection()) {
                System.out.println("ERROR: Cannot connect to the MySQL database 'urbanvibe_db'.");
                System.out.println("  1. Make sure MySQL is running.");
                System.out.println("  2. Run database/schema.sql once to create the database.");
                System.out.println("  3. Check db.url / db.user / db.password in config/config.properties");
                System.out.println("     (copy config/config.example.properties first).");
                AppLogger.error("Start-up failed: database not reachable", null);
                return;
            }
            UserDAO userDAO = new UserDAO();
            ExperienceDAO experienceDAO = new ExperienceDAO();
            BookingDAO bookingDAO = new BookingDAO();
            AuthService authService = new AuthService(userDAO);
            authService.seedDefaultUsers();
            ExperienceService experienceService = new ExperienceService(experienceDAO);
            SearchService searchService = new SearchService(experienceDAO);
            SeatManager seatManager = new SeatManager(bookingDAO);
            PaymentService paymentService = new PaymentService();
            ReceiptGenerator receiptGenerator = new ReceiptGenerator();
            BookingService bookingService = new BookingService(bookingDAO, seatManager, paymentService,
                    receiptGenerator, notificationService);
            BookingHistory bookingHistory = new BookingHistory(bookingDAO);
            ApiService apiService = new ApiService();
            ReportGenerator reportGenerator = new ReportGenerator();

            notificationService.start();
            CustomerMenu customerMenu = new CustomerMenu(authService, experienceService, searchService,
                    bookingService, bookingHistory, seatManager, apiService);
            AdminMenu adminMenu = new AdminMenu(authService, experienceService, bookingService, seatManager,
                    userDAO, bookingDAO, experienceDAO, reportGenerator);
            UserMenu userMenu = new UserMenu(authService, experienceService, customerMenu, adminMenu);

            AppLogger.info("UrbanVibe started");
            userMenu.showMainMenu();
            AppLogger.info("UrbanVibe closed normally");

        } catch (DatabaseOperationException e) {
            System.out.println("Start-up problem: " + e.getMessage());
            AppLogger.error("Start-up failed", e);
        } catch (IllegalStateException e) {
            System.out.println();
            System.out.println("Input ended. Goodbye!");
        } finally {
            notificationService.stop();
            AppLogger.shutdown();
        }
    }}