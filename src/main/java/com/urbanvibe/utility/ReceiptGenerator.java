package com.urbanvibe.utility;

import com.urbanvibe.model.Booking;
import com.urbanvibe.model.Payment;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

public class ReceiptGenerator {

    public String buildReceipt(Booking b, Payment p) {
        StringBuilder sb = new StringBuilder();
        String line = "==========================================\n";
        sb.append(line);
        sb.append("              URBANVIBE RECEIPT\n");
        sb.append("     Entertainment & Experience Booking\n");
        sb.append(line);
        sb.append(String.format("Booking Ref    : %s%n", b.getBookingRef()));
        sb.append(String.format("Booked On      : %s%n",
                b.getCreatedAt() == null ? java.time.LocalDateTime.now().format(fmt())
                        : b.getCreatedAt().format(fmt())));
        sb.append(String.format("Customer       : %s%n", b.getUserName()));
        sb.append("------------------------------------------\n");
        sb.append(String.format("Experience     : %s (%s)%n", b.getExperienceTitle(), b.getCategory()));
        sb.append(String.format("Venue          : %s, %s%n", b.getVenue(), b.getCity()));
        sb.append(String.format("Date           : %s%n", b.getSlotDate()));
        sb.append(String.format("Time           : %s%n", b.getSlotTime()));
        if (b.getSeatLabels() != null && !b.getSeatLabels().isEmpty()) {
            sb.append(String.format("Seats          : %s%n", b.getSeatLabels()));
        }
        sb.append(String.format("Quantity       : %d%n", b.getQuantity()));
        sb.append("------------------------------------------\n");
        sb.append(String.format("Subtotal       : Rs. %.2f%n", b.getSubtotal()));
        if (b.getServiceFee() > 0) {
            sb.append(String.format("Service fee    : Rs. %.2f%n", b.getServiceFee()));
        }
        if (b.getDiscount() > 0) {
            sb.append(String.format("Discount       : -Rs. %.2f (%s)%n", b.getDiscount(), b.getPromoCode()));
        }
        sb.append(String.format("TOTAL PAID     : Rs. %.2f%n", b.getTotalAmount()));
        sb.append("------------------------------------------\n");
        sb.append(String.format("Payment method : %s%n", p.getMethod()));
        sb.append(String.format("Transaction ID : %s%n", p.getTransactionRef()));
        sb.append(String.format("Payment status : %s%n", p.getStatus()));
        sb.append(line);
        sb.append("  Please show this receipt at the venue.\n");
        sb.append("  Thank you for booking with UrbanVibe!\n");
        sb.append(line);
        return sb.toString();
    }

    private static DateTimeFormatter fmt() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    }

    public String generateReceipt(Booking b, Payment p) throws IOException {
        Path path = Paths.get(FileManager.RECEIPTS_DIR, "BOOKING_" + b.getBookingRef() + ".txt");
        Files.createDirectories(path.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(buildReceipt(b, p));
        }
        return path.toString();
    }
}
