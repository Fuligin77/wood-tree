package com.remindme;

import com.remindme.service.ReminderManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class RemindMeApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainWindow.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            primaryStage.setTitle("Remind Me");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);

            // Handle application close event
            primaryStage.setOnCloseRequest(event -> {
                // Save reminders before closing
                ReminderManager.getInstance().saveNow();
                System.out.println("Application closed. Reminders saved.");
            });

            primaryStage.show();

            // Print loading information
            ReminderManager manager = ReminderManager.getInstance();
            System.out.println("Application started. Loaded " + manager.getReminderCount() + " reminders.");
            System.out.println("Save file location: " + manager.getSaveFilePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() throws Exception {
        // Additional cleanup when application stops
        ReminderManager.getInstance().saveNow();
        System.out.println("Application stopped. Final save completed.");
        super.stop();
    }

    public static void main(String[] args) {
        // Add shutdown hook for emergency saves
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                ReminderManager.getInstance().saveNow();
                System.out.println("Emergency save completed via shutdown hook.");
            } catch (Exception e) {
                System.err.println("Error during emergency save: " + e.getMessage());
            }
        }));

        launch(args);
    }
}