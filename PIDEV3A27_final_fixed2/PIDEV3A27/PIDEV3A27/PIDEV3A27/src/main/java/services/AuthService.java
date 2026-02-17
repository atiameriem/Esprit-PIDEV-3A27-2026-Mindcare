package services;

import utils.MyDatabase;
import utils.Session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Auth minimal pour la session (2 users ou plus) : email + mot_de_passe.
 * On ne gère pas l'inscription ici.
 */
public class AuthService {

    private final Connection cnx;

    public AuthService() {
        this.cnx = MyDatabase.getInstance().getConnection();
    }

    public boolean login(String email, String password) throws SQLException {
        String sql = "SELECT id_users, nom, prenom, role FROM users WHERE email = ? AND mot_de_passe = ?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, email);
            pst.setString(2, password);

            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return false;

                int id = rs.getInt("id_users");
                String fullName = rs.getString("prenom") + " " + rs.getString("nom");
                String roleDb = rs.getString("role"); // patient | psychologue

                Session.Role role = "psychologue".equalsIgnoreCase(roleDb)
                        ? Session.Role.PSYCHOLOGUE
                        : Session.Role.PATIENT;

                Session.login(id, role, fullName);
                return true;
            }
        }
    }
}
