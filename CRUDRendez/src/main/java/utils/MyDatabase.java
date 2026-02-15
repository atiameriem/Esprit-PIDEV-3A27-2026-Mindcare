package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    private  final  String URL="jdbc:mysql://localhost:3306/projet_psychologie";
    private  final  String USER="root";
    private  final  String PASSWORD="";
    private Connection cnx;
    private  static utils.MyDatabase instance ;

    private MyDatabase() {
        try {
            cnx = DriverManager.getConnection(URL,USER,PASSWORD);
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
        return cnx;
    }
}