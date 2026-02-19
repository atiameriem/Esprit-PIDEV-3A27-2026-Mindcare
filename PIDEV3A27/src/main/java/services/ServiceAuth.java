package services;

import models.User;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;

/**
 * Authentification simple: email OU nom, + mot_de_passe (comparaison directe).
 * Si ton projet stocke le mot de passe hashé, adapte la vérification ici.
 */
public class ServiceAuth {

    private Connection connection() {
        return MyDatabase.getInstance().getConnection();
    }

    /**
     * @param emailOrNom email (ex: a@b.com) OU nom (ex: Gnaoui)
     * @param password mot_de_passe (comparaison directe)
     * @return User (idUsers, nom, prenom, role, etc.) si OK, sinon null
     */
    public User login(String emailOrNom, String password) {
        if (emailOrNom == null || password == null) return null;

        String ident = emailOrNom.trim();
        String pass = password.trim();
        if (ident.isEmpty() || pass.isEmpty()) return null;

        String sql = "SELECT id_users, nom, prenom, email, image, mot_de_passe, role, date_inscription " +
                "FROM users WHERE email = ? OR nom = ? LIMIT 1";

        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, ident);
            ps.setString(2, ident);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                String dbPass = rs.getString("mot_de_passe");
                if (dbPass == null || !dbPass.equals(pass)) return null;

                User u = new User();
                u.setIdUsers(rs.getInt("id_users"));
                u.setNom(rs.getString("nom"));
                u.setPrenom(rs.getString("prenom"));
                u.setEmail(rs.getString("email"));
                u.setImage(rs.getString("image"));
                u.setMotDePasse(dbPass);
                u.setRole(rs.getString("role"));

                Date d = rs.getDate("date_inscription");
                if (d != null) u.setDateInscription(d.toLocalDate());
                else u.setDateInscription(LocalDate.now());

                return u;
            }
        } catch (SQLException e) {
            System.err.println("[AUTH] " + e.getMessage());
            return null;
        }
    }
}
