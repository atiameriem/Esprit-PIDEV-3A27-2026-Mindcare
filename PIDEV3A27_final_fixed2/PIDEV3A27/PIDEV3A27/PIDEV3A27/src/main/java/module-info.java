module tn.esprit.pidev3a8 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.media;
    requires java.net.http;
    requires java.desktop;
    requires java.sql;
    requires twilio;
    requires org.slf4j;
    // PDF export
    requires openhtmltopdf.pdfbox;
    requires openhtmltopdf.svg.support;
    opens test to javafx.fxml;
    opens controllers to javafx.fxml;
    opens models to javafx.base;

    exports test;
    exports controllers;
    exports models;
    exports services;
    exports utils;
}
