package com.remindme.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Reminder implements Comparable<Reminder> {
    private LocalDate date;
    private LocalTime time;
    private String description;
    private ReminderType reminderType;
    private int reminderValue;
    
    public enum ReminderType {
        SAME_TIME, MINUTES_BEFORE, HOURS_BEFORE, DAYS_BEFORE
    }
    
    public Reminder(LocalDate date, LocalTime time, String description, 
                   ReminderType reminderType, int reminderValue) {
        this.date = date;
        this.time = time;
        this.description = description;
        this.reminderType = reminderType;
        this.reminderValue = reminderValue;
    }
    
    public LocalDateTime getDateTime() {
        return LocalDateTime.of(date, time);
    }
    
    public LocalDateTime getReminderDateTime() {
        LocalDateTime eventDateTime = getDateTime();
        
        switch (reminderType) {
            case SAME_TIME:
                return eventDateTime;
            case MINUTES_BEFORE:
                return eventDateTime.minusMinutes(reminderValue);
            case HOURS_BEFORE:
                return eventDateTime.minusHours(reminderValue);
            case DAYS_BEFORE:
                return eventDateTime.minusDays(reminderValue);
            default:
                return eventDateTime;
        }
    }
    
    public String getFormattedDate() {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
    
    public String getFormattedTime() {
        return time.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
    
    @Override
    public int compareTo(Reminder other) {
        return this.getDateTime().compareTo(other.getDateTime());
    }
    
    @Override
    public String toString() {
        return String.format("%s, %s, %s", description, getFormattedDate(), getFormattedTime());
    }
    
    // Getters and setters
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    
    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public ReminderType getReminderType() { return reminderType; }
    public void setReminderType(ReminderType reminderType) { this.reminderType = reminderType; }
    
    public int getReminderValue() { return reminderValue; }
    public void setReminderValue(int reminderValue) { this.reminderValue = reminderValue; }
}