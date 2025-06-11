import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SplashController {
    
    @FXML
    private Label splashText;
    
    private Stage primaryStage;
    
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }
    
    public void startAnimation() {
        // Initial position - above the window
        splashText.setTranslateY(-300);
        
        // Create timeline for the bouncing animation
        Timeline timeline = new Timeline();
        
        // Drop down animation (0-1 second)
        KeyFrame dropDown = new KeyFrame(Duration.seconds(0.8),
            new KeyValue(splashText.translateYProperty(), 0, Interpolator.EASE_OUT));
        
        // First bounce up (1-1.3 seconds)
        KeyFrame bounce1Up = new KeyFrame(Duration.seconds(1.1),
            new KeyValue(splashText.translateYProperty(), -80, Interpolator.EASE_OUT));
        
        // First bounce down (1.3-1.6 seconds)
        KeyFrame bounce1Down = new KeyFrame(Duration.seconds(1.4),
            new KeyValue(splashText.translateYProperty(), 0, Interpolator.EASE_IN));
        
        // Second bounce up (1.6-1.8 seconds)
        KeyFrame bounce2Up = new KeyFrame(Duration.seconds(1.7),
            new KeyValue(splashText.translateYProperty(), -40, Interpolator.EASE_OUT));
        
        // Second bounce down (1.8-2 seconds)
        KeyFrame bounce2Down = new KeyFrame(Duration.seconds(1.9),
            new KeyValue(splashText.translateYProperty(), 0, Interpolator.EASE_IN));
        
        // Third small bounce up (2-2.1 seconds)
        KeyFrame bounce3Up = new KeyFrame(Duration.seconds(2.05),
            new KeyValue(splashText.translateYProperty(), -20, Interpolator.EASE_OUT));
        
        // Third bounce down (2.1-2.2 seconds)
        KeyFrame bounce3Down = new KeyFrame(Duration.seconds(2.15),
            new KeyValue(splashText.translateYProperty(), 0, Interpolator.EASE_IN));
        
        // Stay in middle (2.2-2.5 seconds)
        KeyFrame stay = new KeyFrame(Duration.seconds(2.5),
            new KeyValue(splashText.translateYProperty(), 0));
        
        timeline.getKeyFrames().addAll(dropDown, bounce1Up, bounce1Down, 
                                      bounce2Up, bounce2Down, bounce3Up, bounce3Down, stay);
        
        // After bouncing animation, fade out
        timeline.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), splashText);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> loadMainWindow());
            fadeOut.play();
        });
        
        timeline.play();
    }
    
    private void loadMainWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 800, 600);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}