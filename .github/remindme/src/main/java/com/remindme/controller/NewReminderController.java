package com.remindme.controller;

import com.remindme.model.Reminder;
import com.remindme.service.ReminderManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;

public class NewReminderController implements Initializable {
    
    @FXML private TextField dateField;
    @FXML private TextField timeField;
    @FXML private TextField descriptionField;
    @FXML private CheckBox sameTimeCheckBox;
    @FXML private RadioButton minutesBeforeRadio;
    @FXML private RadioButton hoursBeforeRadio;
    @FXML private RadioButton daysBeforeRadio;
    @FXML private TextField minutesField;
    @FXML private TextField hoursField;
    @FXML private TextField daysField;
    @FXML private Button addButton;
    @FXML private Label errorLabel;
    
    private ToggleGroup reminderGroup;
    private MainController mainController;
    private ReminderManager reminderManager;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        reminderManager = ReminderManager.getInstance();
        
        // Set up toggle group for radio buttons
        reminderGroup = new ToggleGroup();
        minutesBeforeRadio.setToggleGroup(reminderGroup);
        hoursBeforeRadio.setToggleGroup(reminderGroup);
        daysBeforeRadio.setToggleGroup(reminderGroup);
        
        // Set up text field limits
        descriptionField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > 20) {
                descriptionField.setText(oldVal);
            }
        });
        
        // Set up checkbox behavior
        sameTimeCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                reminderGroup.selectToggle(null);
                minutesField.setDisable(true);
                hoursField.setDisable(true);
                daysField.setDisable(true);
            } else {
                minutesField.setDisable(false);
                hoursField.setDisable(false);
                daysField.setDisable(false);
            }
        });
        
        // Set up radio button behavior
        reminderGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                sameTimeCheckBox.setSelected(false);
            }
        });
        
        // Hide error label initially
        errorLabel.setVisible(false);
        
        // Set default values
        LocalDate today = LocalDate.now();
        dateField.setText(today.format(DateTimeFormatter.ofPattern("dd/MM/yy")));
        timeField.setText("12:00");
        sameTimeCheckBox.setSelected(true);
    }
    
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
    
    @FXML
    private void handleAddReminder() {
        try {
            // Validate and parse input
            LocalDate date = parseDate(dateField.getText());
            LocalTime time = parseTime(timeField.getText());
            String description = descriptionField.getText().trim();
            
            if (description.isEmpty()) {
                showError("Description cannot be empty");
                return;
            }
            
            // Determine reminder type and value
            Reminder.ReminderType reminderType;
            int reminderValue = 0;
            
            if (sameTimeCheckBox.isSelected()) {
                reminderType = Reminder.ReminderType.SAME_TIME;
            } else if (minutesBeforeRadio.isSelected()) {
                reminderType = Reminder.ReminderType.MINUTES_BEFORE;
                reminderValue = Integer.parseInt(minutesField.getText());
            } else if (hoursBeforeRadio.isSelected()) {
                reminderType = Reminder.ReminderType.HOURS_BEFORE;
                reminderValue = Integer.parseInt(hoursField.getText());
            } else if (daysBeforeRadio.isSelected()) {
                reminderType = Reminder.ReminderType.DAYS_BEFORE;
                reminderValue = Integer.parseInt(daysField.getText());
            } else {
                showError("Please select a reminder option");
                return;
            }
            
            // Create and add reminder
            Reminder reminder = new Reminder(date, time, description, reminderType, reminderValue);
            reminderManager.addReminder(reminder);
            
            // Show confirmation and close window
            if (mainController != null) {
                mainController.showConfirmationMessage("Reminder added successfully!");
            }
            
            closeWindow();
            
        } catch (Exception e) {
            showError("Invalid input: " + e.getMessage());
        }
    }
    
    private LocalDate parseDate(String dateStr) throws DateTimeParseException {
        // Handle both dd/MM/yy and dd/MM/yyyy formats
        if (dateStr.length() == 8) { // dd/MM/yy
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yy"));
        } else { // dd/MM/yyyy
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
    }
    
    private LocalTime parseTime(String timeStr) throws DateTimeParseException {
        return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
    
    @FXML
    private void handleCancel() {
        closeWindow();
    }
    
    private void closeWindow() {
        Stage stage = (Stage) addButton.getScene().getWindow();
        stage.close();
    }
}