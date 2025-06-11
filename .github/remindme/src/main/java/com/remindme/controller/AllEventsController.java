package com.remindme.controller;

import com.remindme.model.Reminder;
import com.remindme.service.ReminderManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.TextFieldListCell;

import java.net.URL;
import java.util.ResourceBundle;

public class AllEventsController implements Initializable {
    
    @FXML private ListView<Reminder> eventsListView;
    
    private ReminderManager reminderManager;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        reminderManager = ReminderManager.getInstance();
        
        // Set up list view
        eventsListView.setItems(reminderManager.getReminders());
        
        // Custom cell factory to display reminders nicely
        eventsListView.setCellFactory(listView -> new TextFieldListCell<Reminder>() {
            @Override
            public void updateItem(Reminder reminder, boolean empty) {
                super.updateItem(reminder, empty);
                if (empty || reminder == null) {
                    setText(null);
                } else {
                    setText(String.format("📅 %s | ⏰ %s | 📝 %s", 
                            reminder.getFormattedDate(),
                            reminder.getFormattedTime(),
                            reminder.getDescription()));
                }
            }
        });
        
        // Add context menu for deletion (optional feature)
        eventsListView.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("DELETE")) {
                Reminder selected = eventsListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    reminderManager.removeReminder(selected);
                }
            }
        });
    }
}