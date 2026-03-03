module tn.esprit.pidev3a8 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires java.desktop;
    requires java.net.http;
    requires jdk.httpserver;
    requires jdk.jsobject;
    requires com.google.zxing;
    requires com.google.zxing.javase;

    opens test to javafx.fxml;
    opens controllers to javafx.fxml;
    opens models to javafx.base;
    
    exports test;
    exports controllers;
    exports models;
    exports services;
    exports utils;
}
