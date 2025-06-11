import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/splash.fxml"));
        Parent root = loader.load();
        
        SplashController splashController = loader.getController();
        splashController.setPrimaryStage(primaryStage);
        
        primaryStage.setTitle("Pizza Dough Calculator");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setResizable(false);
        primaryStage.show();
        
        splashController.startAnimation();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}