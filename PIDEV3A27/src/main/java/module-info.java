module tn.esprit.pidev3a8 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens test to javafx.fxml;
    opens controllers to javafx.fxml;
    opens models to javafx.base;

    exports test;
    exports controllers;
    exports models;
    exports services;
    exports utils;
    exports utils.forum;
}
