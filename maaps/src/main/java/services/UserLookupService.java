package services;

import models.AppUser;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;

public class UserLookupService {
    private final Connection cnx;

    public UserLookupService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    public AppUser getFirstByRole(String role) throws SQLException {
        String sql = "SELECT id_users, nom, prenom, email, mot_de_passe, role, date_inscription FROM users WHERE role=? ORDER BY id_users ASC LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    private AppUser map(ResultSet rs) throws SQLException {
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
