package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    private static final String URL  = "jdbc:mysql://localhost:3306/new-pi";
    private static final String USER = "root";
    private static final String PASS = "";

    private static MyDatabase instance;

    private MyDatabase() {}

    public static synchronized MyDatabase getInstance() {
        if (instance == null)
            instance = new MyDatabase();
        return instance;
    }

    // ✅ Nouvelle connexion à chaque appel
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}