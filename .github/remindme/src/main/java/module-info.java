module com.remindme {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    opens com.remindme to javafx.fxml;
    opens com.remindme.controller to javafx.fxml;
    exports com.remindme;
}