package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    private final String URL = "jdbc:mysql://localhost:3306/pi?zeroDateTimeBehavior=CONVERT_TO_NULL";
    private final String USER = "root";
    private final String PASS = "";
    private Connection connection;

    private static MyDatabase instance;

    private MyDatabase() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASS);
            connection.setAutoCommit(true);
            System.out.println("✅ Database Connection success and autoCommit enabled: " + URL);
        } catch (SQLException e) {
            System.err.println("❌ Database Connection FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static MyDatabase getInstance() {
        if (instance == null)
            instance = new MyDatabase();
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                System.out.println("🔄 Reconnecting to database...");
                connection = DriverManager.getConnection(URL, USER, PASS);
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to refresh database connection: " + e.getMessage());
        }
        return connection;
    }
}
