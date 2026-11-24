package com.urbanvibe.service;

import com.urbanvibe.dao.ExperienceDAO;
import com.urbanvibe.exception.DatabaseOperationException;
import com.urbanvibe.exception.InvalidInputException;
import com.urbanvibe.exception.InvalidLoginException;
import com.urbanvibe.model.Experience;
import com.urbanvibe.model.TimeSlot;
import com.urbanvibe.utility.AppLogger;
import com.urbanvibe.utility.UserSession;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class ExperienceService {

    private static final double MAX_PRICE = 100000;
    private final ExperienceDAO experienceDAO;

    public ExperienceService(ExperienceDAO experienceDAO) {
        this.experienceDAO = experienceDAO;
    }

    private void requireAdmin() throws InvalidLoginException {
        if (!UserSession.isAdmin()) {
            throw new InvalidLoginException("Access denied: administrator privileges required.");
        }
    }

    private void validate(Experience exp) throws InvalidInputException {
        if (exp.getTitle() == null || exp.getTitle().trim().length() < 2) {
            throw new InvalidInputException("Title must have at least 2 characters.");
        }
        if (exp.getCity() == null || exp.getCity().trim().isEmpty()) {
            throw new InvalidInputException("City is required.");
        }
        if (exp.getVenue() == null || exp.getVenue().trim().isEmpty()) {
            throw new InvalidInputException("Venue is required.");
        }
        if (exp.getBasePrice() < 0 || exp.getBasePrice() > MAX_PRICE) {
            throw new InvalidInputException("Price must be between 0 and " + (int) MAX_PRICE + ".");
        }
    }

    public int addExperience(Experience exp)
            throws InvalidLoginException, InvalidInputException, DatabaseOperationException {
        requireAdmin();
        validate(exp);
        int id = experienceDAO.insertExperience(exp);
        AppLogger.info("Experience added: #" + id + " " + exp.getTitle());
        return id;
    }

    public void updateExperience(Experience exp)
            throws InvalidLoginException, InvalidInputException, DatabaseOperationException {
        requireAdmin();
        validate(exp);
        experienceDAO.updateExperience(exp);
        AppLogger.info("Experience updated: #" + exp.getExperienceId());
    }

    public boolean deleteExperience(int experienceId) throws InvalidLoginException, DatabaseOperationException {
        requireAdmin();
        boolean deleted = experienceDAO.deleteExperience(experienceId);
        AppLogger.info("Experience " + (deleted ? "deleted" : "deactivated") + ": #" + experienceId);
        return deleted;
    }

    public List<Experience> listByCategory(String category) throws DatabaseOperationException {
        return experienceDAO.findByCategory(category, true);
    }

    public List<Experience> listAll(boolean onlyActive) throws DatabaseOperationException {
        return experienceDAO.findAll(onlyActive);
    }

    public Experience getById(int id) throws DatabaseOperationException, InvalidInputException {
        Experience exp = experienceDAO.findById(id);
        if (exp == null) {
            throw new InvalidInputException("No experience with ID " + id + ".");
        }
        return exp;
    }

    public List<TimeSlot> getSlots(int experienceId, boolean futureOnly) throws DatabaseOperationException {
        return experienceDAO.findSlots(experienceId, futureOnly);
    }

    private void validateSlotTime(LocalDate date, LocalTime time) throws InvalidInputException {
        if (LocalDateTime.of(date, time).isBefore(LocalDateTime.now())) {
            throw new InvalidInputException("Slot date and time must be in the future.");
        }
    }

    public int addSlot(int experienceId, LocalDate date, LocalTime time, int capacity)
            throws InvalidLoginException, InvalidInputException, DatabaseOperationException {
        requireAdmin();
        Experience exp = getById(experienceId);
        if (exp.isSeatBased()) {
            throw new InvalidInputException("This experience uses seats. Enter rows and seats per row instead.");
        }
        validateSlotTime(date, time);
        if (capacity < 1 || capacity > 500) {
            throw new InvalidInputException("Capacity must be between 1 and 500.");
        }
        TimeSlot slot = new TimeSlot(0, experienceId, date, time, capacity, capacity);
        return experienceDAO.insertSlot(slot, exp.getBasePrice(), 0, 0);
    }

    public int addSlot(int experienceId, LocalDate date, LocalTime time, int rows, int seatsPerRow)
            throws InvalidLoginException, InvalidInputException, DatabaseOperationException {
        requireAdmin();
        Experience exp = getById(experienceId);
        if (!exp.isSeatBased()) {
            throw new InvalidInputException("This experience is capacity based. Enter a single capacity instead.");
        }
        validateSlotTime(date, time);
        if (rows < 1 || rows > 10 || seatsPerRow < 1 || seatsPerRow > 15) {
            throw new InvalidInputException("Use 1-10 rows and 1-15 seats per row.");
        }
        TimeSlot slot = new TimeSlot(0, experienceId, date, time, rows * seatsPerRow, rows * seatsPerRow);
        return experienceDAO.insertSlot(slot, exp.getBasePrice(), rows, seatsPerRow);
    }

    public void deleteSlot(int slotId) throws InvalidLoginException, DatabaseOperationException {
        requireAdmin();
        experienceDAO.deleteSlot(slotId);
    }

    public void updateCapacity(int slotId, int newCapacity)
            throws InvalidLoginException, InvalidInputException, DatabaseOperationException {
        requireAdmin();
        TimeSlot slot = experienceDAO.findSlotById(slotId);
        if (slot == null) {
            throw new InvalidInputException("No slot with ID " + slotId + ".");
        }
        Experience exp = experienceDAO.findById(slot.getExperienceId());
        if (exp != null && exp.isSeatBased()) {
            throw new InvalidInputException("Seat-based slots get their capacity from the seat map.");
        }
        if (newCapacity < 1 || newCapacity > 500) {
            throw new InvalidInputException("Capacity must be between 1 and 500.");
        }
        experienceDAO.updateSlotCapacity(slotId, newCapacity);
    }
}
