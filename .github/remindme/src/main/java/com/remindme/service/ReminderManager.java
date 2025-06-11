package com.remindme.service;

import com.remindme.model.Reminder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ReminderManager {
    private static ReminderManager instance;
    private ObservableList<Reminder> reminders;
    private static final String DATA_FILE = "reminders.dat";
    private static final String DATA_FOLDER = "RemindMeData";

    private ReminderManager() {
        reminders = FXCollections.observableArrayList();
        loadRemindersFromFile();
    }

    public static ReminderManager getInstance() {
        if (instance == null) {
            instance = new ReminderManager();
        }
        return instance;
    }

    public void addReminder(Reminder reminder) {
        reminders.add(reminder);
        reminders.sort(null); // Sort by date/time
        saveRemindersToFile(); // Auto-save when adding
    }

    public ObservableList<Reminder> getReminders() {
        return reminders;
    }

    public Optional<Reminder> getNextUpcomingReminder() {
        LocalDateTime now = LocalDateTime.now();
        return reminders.stream()
                .filter(reminder -> reminder.getDateTime().isAfter(now))
                .findFirst();
    }

    public void removeReminder(Reminder reminder) {
        reminders.remove(reminder);
        saveRemindersToFile(); // Auto-save when removing
    }

    public void clearAllReminders() {
        reminders.clear();
        saveRemindersToFile(); // Auto-save when clearing
    }

    /**
     * Save all reminders to a file in the user's home directory
     */
    private void saveRemindersToFile() {
        try {
            // Create data directory if it doesn't exist
            Path dataDir = Paths.get(System.getProperty("user.home"), DATA_FOLDER);
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }

            // Create file path
            Path filePath = dataDir.resolve(DATA_FILE);

            // Write reminders to file
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(filePath))) {
                for (Reminder reminder : reminders) {
                    // Format: date|time|description|reminderType|reminderValue
                    String line = String.format("%s|%s|%s|%s|%d",
                            reminder.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                            reminder.getTime().format(DateTimeFormatter.ISO_LOCAL_TIME),
                            reminder.getDescription().replace("|", "&#124;"), // Escape pipe characters
                            reminder.getReminderType().name(),
                            reminder.getReminderValue()
                    );
                    writer.println(line);
                }
            }

            System.out.println("Reminders saved successfully to: " + filePath);

        } catch (IOException e) {
            System.err.println("Error saving reminders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load all reminders from file
     */
    private void loadRemindersFromFile() {
        try {
            Path dataDir = Paths.get(System.getProperty("user.home"), DATA_FOLDER);
            Path filePath = dataDir.resolve(DATA_FILE);

            // Check if file exists
            if (!Files.exists(filePath)) {
                System.out.println("No saved reminders file found. Starting fresh.");
                return;
            }

            // Read reminders from file
            try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                String line;
                int loadedCount = 0;

                while ((line = reader.readLine()) != null) {
                    try {
                        Reminder reminder = parseReminderFromLine(line);
                        if (reminder != null) {
                            reminders.add(reminder);
                            loadedCount++;
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing reminder line: " + line);
                        System.err.println("Error: " + e.getMessage());
                    }
                }

                // Sort loaded reminders
                reminders.sort(null);

                System.out.println("Loaded " + loadedCount + " reminders from file");

            }

        } catch (IOException e) {
            System.err.println("Error loading reminders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Parse a single reminder from a file line
     */
    private Reminder parseReminderFromLine(String line) {
        try {
            String[] parts = line.split("\\|");
            if (parts.length != 5) {
                throw new IllegalArgumentException("Invalid line format: expected 5 parts, got " + parts.length);
            }

            LocalDate date = LocalDate.parse(parts[0], DateTimeFormatter.ISO_LOCAL_DATE);
            LocalTime time = LocalTime.parse(parts[1], DateTimeFormatter.ISO_LOCAL_TIME);
            String description = parts[2].replace("&#124;", "|"); // Unescape pipe characters
            Reminder.ReminderType reminderType = Reminder.ReminderType.valueOf(parts[3]);
            int reminderValue = Integer.parseInt(parts[4]);

            return new Reminder(date, time, description, reminderType, reminderValue);

        } catch (Exception e) {
            System.err.println("Error parsing reminder: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get the path where reminders are saved
     */
    public String getSaveFilePath() {
        Path dataDir = Paths.get(System.getProperty("user.home"), DATA_FOLDER);
        return dataDir.resolve(DATA_FILE).toString();
    }

    /**
     * Manually trigger save (useful for testing or explicit saves)
     */
    public void saveNow() {
        saveRemindersToFile();
    }

    /**
     * Get count of loaded reminders
     */
    public int getReminderCount() {
        return reminders.size();
    }
}