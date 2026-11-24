package com.urbanvibe.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class TimeSlot {

    private int slotId;
    private int experienceId;
    private LocalDate slotDate;
    private LocalTime slotTime;
    private int totalCapacity;
    private int availableCapacity;

    public TimeSlot() { }

    public TimeSlot(int slotId, int experienceId, LocalDate slotDate, LocalTime slotTime,
                    int totalCapacity, int availableCapacity) {
        this.slotId = slotId;
        this.experienceId = experienceId;
        this.slotDate = slotDate;
        this.slotTime = slotTime;
        this.totalCapacity = totalCapacity;
        this.availableCapacity = availableCapacity;
    }

    public int getSlotId() { return slotId; }
    public void setSlotId(int slotId) { this.slotId = slotId; }
    public int getExperienceId() { return experienceId; }
    public void setExperienceId(int experienceId) { this.experienceId = experienceId; }
    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }
    public LocalTime getSlotTime() { return slotTime; }
    public void setSlotTime(LocalTime slotTime) { this.slotTime = slotTime; }
    public int getTotalCapacity() { return totalCapacity; }
    public void setTotalCapacity(int totalCapacity) { this.totalCapacity = totalCapacity; }
    public int getAvailableCapacity() { return availableCapacity; }
    public void setAvailableCapacity(int availableCapacity) { this.availableCapacity = availableCapacity; }

    public LocalDateTime getStartDateTime() {
        return LocalDateTime.of(slotDate, slotTime);
    }

    public boolean isSoldOut() {
        return availableCapacity <= 0;
    }

    @Override
    public String toString() {
        return String.format("Slot %d | %s %s | %d/%d available",
                slotId, slotDate, slotTime, availableCapacity, totalCapacity);
    }
}
