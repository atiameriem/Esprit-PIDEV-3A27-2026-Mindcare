package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    private final String URL = "jdbc:mysql://localhost:3306/projet_psychologie?zeroDateTimeBehavior=CONVERT_TO_NULL&autoReconnect=true";
    private final String USER = "root";
    private final String PASSWORD = "";
    private Connection cnx;
    private static utils.MyDatabase instance;

    private MyDatabase() {
        try {
            cnx = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connected to database");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static utils.MyDatabase getInstance() {
        if (instance == null)
            instance = new utils.MyDatabase();
        return instance;
    }

    public Connection getCnx() {
        try {
            if (cnx == null || cnx.isClosed() || !cnx.isValid(2)) {
                System.out.println("Connection is null, closed or invalid. Reconnecting...");
                cnx = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (SQLException e) {
            System.err.println("Error while validating or re-establishing connection: " + e.getMessage());
        }
        return cnx;
    }
}