package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {
//3306 → port MySQL
    private final String URL = "jdbc:mysql://localhost:3306/projet_psychologie";
    private final String USER = "root";
    private final String PASS = "";
    //Objet Java représentant la connexion à la base
    private Connection connection;
//Cette variable va contenir l’unique instance.(le singleton)
    private static MyDatabase instance;
 //onstructeur privé pour empecher de faire
 // une nouvell databs a partir de lexterirzur
    private MyDatabase(){
        try {
            //DriverManager ouvre la connexion à MySQL
            connection = DriverManager.getConnection(URL,USER,PASS);
            System.out.println("Connection established");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    //Si instance n’existe pas → on la crée
    //Sinon → on retourne celle existante
    public static MyDatabase getInstance() {
        if(instance == null)
            instance = new MyDatabase();
        return instance;
    }
//Permet aux services d’obtenir la connexion.
    public Connection getConnection() {
        return connection;
    }
}
