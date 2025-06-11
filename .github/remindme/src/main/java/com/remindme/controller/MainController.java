package com.remindme.controller;

import com.remindme.components.AnalogClock;
import com.remindme.model.Reminder;
import com.remindme.service.ReminderManager;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private Button dateButton;
    @FXML private Button nextEventButton;
    @FXML private Button alarmButton;
    @FXML private StackPane clockContainer;
    @FXML private Pane mainPane;
    @FXML private Label confirmationLabel;

    private Timeline clockTimeline;
    private Timeline alarmCheckTimeline;
    private ReminderManager reminderManager;
    private AnalogClock analogClock;
    private boolean alarmActive = false;
    private MediaPlayer alarmPlayer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        reminderManager = ReminderManager.getInstance();

        // Initialize analog clock
        initializeAnalogClock();

        // Initialize alarm functionality
        initializeAlarmSystem();

        // Update date button
        updateDateButton();

        // Update next event button
        updateNextEventButton();

        // Hide confirmation label initially
        confirmationLabel.setVisible(false);

        // Set initial alarm button state (inactive)
        updateAlarmButtonAppearance();

        // Listen for changes in reminders list
        reminderManager.getReminders().addListener((javafx.collections.ListChangeListener<Reminder>) c -> {
            updateNextEventButton();
        });
    }

    private void initializeAnalogClock() {
        // Create and add the analog clock
        analogClock = new AnalogClock();
        clockContainer.getChildren().add(analogClock);

        // Start the clock update timeline
        clockTimeline = new Timeline();
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.getKeyFrames().add(
                new javafx.animation.KeyFrame(Duration.seconds(1), e -> analogClock.updateTime())
        );
        clockTimeline.play();
    }

    private void initializeAlarmSystem() {
        // Initialize alarm sound
        try {
            URL soundURL = getClass().getResource("/sounds/alarm.wav");
            if (soundURL != null) {
                Media alarmSound = new Media(soundURL.toString());
                alarmPlayer = new MediaPlayer(alarmSound);
                alarmPlayer.setCycleCount(3); // Play 3 times
            } else {
                System.err.println("Warning: alarm.wav not found in /sounds/ directory");
            }
        } catch (Exception e) {
            System.err.println("Error loading alarm sound: " + e.getMessage());
        }

        // Start alarm check timeline (check every minute)
        alarmCheckTimeline = new Timeline();
        alarmCheckTimeline.setCycleCount(Timeline.INDEFINITE);
        alarmCheckTimeline.getKeyFrames().add(
                new javafx.animation.KeyFrame(Duration.seconds(60), e -> checkForAlarms())
        );
        alarmCheckTimeline.play();
    }

    private void checkForAlarms() {
        if (!alarmActive) return;

        LocalDateTime now = LocalDateTime.now();

        // Check if any reminder's alarm time matches current time (within 1 minute)
        reminderManager.getReminders().stream()
                .filter(reminder -> {
                    LocalDateTime reminderTime = reminder.getReminderDateTime();
                    return reminderTime.isAfter(now.minusMinutes(1)) &&
                            reminderTime.isBefore(now.plusMinutes(1));
                })
                .findFirst()
                .ifPresent(this::triggerAlarm);
    }

    private void triggerAlarm(Reminder reminder) {
        // Play alarm sound
        if (alarmPlayer != null) {
            alarmPlayer.stop(); // Stop if already playing
            alarmPlayer.play();
        }

        // Show notification
        showConfirmationMessage("⚠️ Reminder: " + reminder.getDescription());

        // Optional: You could also show a popup dialog here
        System.out.println("ALARM: " + reminder.toString());
    }

    @FXML
    private void toggleAlarm() {
        alarmActive = !alarmActive;
        updateAlarmButtonAppearance();

        String message = alarmActive ? "Alarm activated 🔔" : "Alarm deactivated 🔕";
        showConfirmationMessage(message);
    }

    private void updateAlarmButtonAppearance() {
        if (alarmActive) {
            alarmButton.setText("🔔");
            alarmButton.getStyleClass().removeAll("inactive");
            alarmButton.getStyleClass().add("active");
        } else {
            alarmButton.setText("🔕");
            alarmButton.getStyleClass().removeAll("active");
            alarmButton.getStyleClass().add("inactive");
        }
    }

    private void updateDateButton() {
        LocalDateTime now = LocalDateTime.now();
        String dateText = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        dateButton.setText(dateText);
    }

    private void updateNextEventButton() {
        Optional<Reminder> nextReminder = reminderManager.getNextUpcomingReminder();
        if (nextReminder.isPresent()) {
            Reminder reminder = nextReminder.get();
            nextEventButton.setText("Next Event: " + reminder.toString());
            nextEventButton.setDisable(false);
        } else {
            nextEventButton.setText("No upcoming events");
            nextEventButton.setDisable(true);
        }
    }

    @FXML
    private void openNewReminderWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NewReminderWindow.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            Stage stage = new Stage();
            stage.setTitle("New Reminder");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.initModality(Modality.APPLICATION_MODAL);

            NewReminderController controller = loader.getController();
            controller.setMainController(this);

            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openAllEventsWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AllEventsWindow.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            Stage stage = new Stage();
            stage.setTitle("All Events");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.initModality(Modality.APPLICATION_MODAL);

            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showConfirmationMessage(String message) {
        confirmationLabel.setText(message);
        confirmationLabel.setVisible(true);
        confirmationLabel.setOpacity(1.0);
        confirmationLabel.setTranslateY(0);

        // Animation: fade in, move up, then fade out
        TranslateTransition moveUp = new TranslateTransition(Duration.seconds(3), confirmationLabel);
        moveUp.setByY(-50);

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), confirmationLabel);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.seconds(2));

        moveUp.play();
        fadeOut.play();

        fadeOut.setOnFinished(e -> confirmationLabel.setVisible(false));
    }
}