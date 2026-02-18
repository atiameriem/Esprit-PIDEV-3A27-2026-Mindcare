package test;

import models.User;
import services.UserService;

import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        UserService ps = new UserService();
        try {
             ps.create(new User(23,"foulen ","Ben Foulen"));
            // ps.update(new User(1,25, "Skander","Ben Foulen"));
            System.out.println(ps.read());
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }
}
