module com.safenote.safenote {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires java.desktop;
    requires javafx.swing;


    opens com.safenote to javafx.fxml, javafx.controls
            , javafx.web, java.sql, java.desktop, javafx.swing;

    exports com.safenote;
}