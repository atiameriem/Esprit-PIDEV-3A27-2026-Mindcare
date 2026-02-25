module pidev3a8 {
    requires javafx.controls;
    requires javafx.fxml;

    requires java.sql;
    requires java.desktop;
    requires jakarta.mail;
    requires com.sun.jna;
    requires vosk;

    opens controllers to javafx.fxml;

    exports test;
    exports controllers;
    exports models;
    exports services;
    exports utils;
}
