package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    // IMPORTANT: Base de données du projet (voir export SQL projet_psychologie)
    private final String URL = "jdbc:mysql://localhost:3306/projet_psychologie?useSSL=false&serverTimezone=UTC&zeroDateTimeBehavior=convertToNull";    private final String USER = "root";
    private final String PASS = "";
    private Connection connection;

    private static MyDatabase instance;

    private MyDatabase(){
        openConnection();
    }

    private void openConnection() {
        try {
            // Si l'ancienne connexion est fermée, on la recrée.
            if (connection != null && !connection.isClosed()) return;
            connection = DriverManager.getConnection(URL, USER, PASS);
            System.out.println("Connection established");

            // Auto-migration (tables forum avancé)
            try {
                DatabaseInitializer.ensureForumExtraTables(connection);
            } catch (SQLException e) {
                System.err.println("[DB INIT] " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("[DB] " + e.getMessage());
        }
    }

    public static MyDatabase getInstance() {
        if(instance == null)
            instance = new MyDatabase();
        return instance;
    }

    public Connection getConnection() {
        // Protection: si une partie du code a fermé la connexion, on la rouvre.
        openConnection();
        return connection;
    }
}
