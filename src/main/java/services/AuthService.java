package services;

import utils.MyDatabase;
import utils.Session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthService {

    public boolean login(String email, String password) throws SQLException {
        String sql = "SELECT id_users, nom, prenom, role FROM users " +
                "WHERE email = ? AND mot_de_passe = ?";

        try (Connection cnx = MyDatabase.getInstance().getConnection();
             PreparedStatement pst = cnx.prepareStatement(sql)) {

            pst.setString(1, email);
            pst.setString(2, password);

            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return false;

                int    id         = rs.getInt("id_users");
                String prenom     = rs.getString("prenom");
                String nom        = rs.getString("nom");
                String roleDb     = rs.getString("role");
                String nomComplet = (prenom != null ? prenom : "")
                        + " "
                        + (nom != null ? nom : "");

                Session.Role role = mapRole(roleDb);

                // ✅ Log pour vérifier le mapping
                System.out.println("🔐 Login : " + nomComplet.trim()
                        + " | DB role='" + roleDb + "'"
                        + " | Session.Role=" + role);

                Session.login(id, role, nomComplet.trim());
                return true;
            }
        }
    }

    private Session.Role mapRole(String roleDb) {
        if (roleDb == null) return Session.Role.USER;

        switch (roleDb.toLowerCase().trim()) {

            case "admin":
                return Session.Role.ADMIN;

            case "psychologue":
                return Session.Role.PSYCHOLOGUE;

            // ✅ CORRECTION PRINCIPALE : "responsablec" ajouté
            case "responsablec":         // ← valeur réelle en DB
            case "responsable_centre":
            case "responsable-centre":
            case "responsablecentre":
                return Session.Role.RESPONSABLEC;

            case "patient":
            case "user":
            default:
                return Session.Role.USER;
        }
    }
}