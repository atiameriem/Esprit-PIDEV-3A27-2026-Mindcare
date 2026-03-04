package test;

import models.User;
import services.UserService;

import java.sql.SQLException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        UserService ps = new UserService();
        try {
            // Création d'un nouvel utilisateur avec des valeurs fictives pour email, téléphone et date
           // User user = new User(23, "Foulen", "Ben Foulen", "", "", "");
            //ps.create(user);

            // Lecture de tous les utilisateurs
            List<User> users = ps.read();
            for (User u : users) {
                System.out.println(u);
            }

            // Mise à jour d'un utilisateur (exemple)
            // User updated = new User(1, 25, "Skander", "Ben Foulen", "email@test.com", "12345678", "2026-02-17");
            // ps.update(updated);

        } catch (SQLException e) {
            System.err.println("Erreur SQL : " + e.getMessage());
        }
    }
}
