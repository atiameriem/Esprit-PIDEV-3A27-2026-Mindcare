package services;

import models.AppUser;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;

public class AuthService {

    private final Connection cnx;

    public AuthService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    public AppUser login(String email, String motDePasse) throws SQLException {
        String sql = "SELECT id_users, nom, prenom, email, mot_de_passe, role, date_inscription FROM users WHERE email=? AND mot_de_passe=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, motDePasse);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    AppUser u = new AppUser();
                    u.setIdUsers(rs.getInt("id_users"));
                    u.setNom(rs.getString("nom"));
                    u.setPrenom(rs.getString("prenom"));
                    u.setEmail(rs.getString("email"));
                    u.setMotDePasse(rs.getString("mot_de_passe"));
                    u.setRole(rs.getString("role"));
                    Date d = rs.getDate("date_inscription");
                    if (d != null) u.setDateInscription(d.toLocalDate());
                    return u;
                }
            }
        }
        return null;
    }
}
