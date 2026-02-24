module tn.esprit.pidev3a8 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    requires java.net.http;

    requires jakarta.mail;
    // ✅ AJOUTER dans module-info.java
    requires javafx.media;// <-- Ajoute ceci

// Replace with:
    requires jakarta.activation;
    opens test to javafx.fxml;
    opens controllers to javafx.fxml;
    opens models to javafx.base;

    exports test;
    exports controllers;
    exports models;
    exports services;
    exports utils;
}
