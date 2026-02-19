package services;

import utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Accès DB lié aux utilisateurs (ex: récupérer le rôle).
 * Toute logique SQL doit rester dans services/.
 */
public class UserService {

    /**
     * Retourne le rôle depuis la table users.
     * - Priorité: id_users si > 0
     * - Fallback: email si non vide
     */
    public String fetchRoleFromDb(int idUsers, String email) {
        final String sqlById = "SELECT role FROM users WHERE id_users = ? LIMIT 1";
        final String sqlByEmail = "SELECT role FROM users WHERE email = ? LIMIT 1";

        try {
            // IMPORTANT: ne pas fermer la connexion du singleton.
            Connection c = MyDatabase.getInstance().getConnection();
            if (c == null) return null;

            if (idUsers > 0) {
                try (PreparedStatement ps = c.prepareStatement(sqlById)) {
                    ps.setInt(1, idUsers);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return rs.getString("role");
                    }
                }
            }

            if (email != null && !email.isBlank()) {
                try (PreparedStatement ps = c.prepareStatement(sqlByEmail)) {
                    ps.setString(1, email.trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return rs.getString("role");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[USER] fetchRoleFromDb: " + e.getMessage());
        }

        return null;
    }
}
