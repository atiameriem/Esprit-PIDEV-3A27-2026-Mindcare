package services;

import models.User;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    private final Connection connection;

    public UserService() {
        connection = MyDatabase.getInstance().getCnx();
    }

    // ================= CREATE =================
    public int create(User user) throws SQLException {
        String sql = "INSERT INTO users (nom, prenom, email, telephone, date_inscription, mot_de_passe, role, badge_image, date_naissance) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getNom());
            stmt.setString(2, user.getPrenom());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getTelephone());
            stmt.setDate(5, Date.valueOf(
                    user.getDateInscription() != null ? user.getDateInscription() : LocalDate.now()));
            stmt.setString(6, user.getMotDePasse());
            stmt.setString(7, user.getRole().name());
            stmt.setString(8, user.getBadge());
            stmt.setDate(9, user.getDateNaissance() != null ? Date.valueOf(user.getDateNaissance()) : null);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int generatedId = rs.getInt(1);
                    user.setId(generatedId);
                    return generatedId;
                }
            }
            System.out.println("User ajouté !");
        }
        return -1;
    }

    // ================= GET ALL =================
    public List<User> getAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    // ================= UPDATE =================
    public void update(User user) throws SQLException {
        String sql = "UPDATE users SET nom=?, prenom=?, email=?, telephone=?, "
                + "date_inscription=?, mot_de_passe=?, role=?, badge_image=?, date_naissance=? WHERE id_users=?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getNom());
            stmt.setString(2, user.getPrenom());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getTelephone());
            stmt.setDate(5, Date.valueOf(
                    user.getDateInscription() != null ? user.getDateInscription() : LocalDate.now()));
            stmt.setString(6, user.getMotDePasse());
            stmt.setString(7, user.getRole().name());
            stmt.setString(8, user.getBadge());
            stmt.setDate(9, user.getDateNaissance() != null ? Date.valueOf(user.getDateNaissance()) : null);
            stmt.setInt(10, user.getId());
            stmt.executeUpdate();
            System.out.println("User mis à jour !");
        }
    }

    // ================= DELETE =================
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE id_users=?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            System.out.println("User supprimé !");
        }
    }

    // ================= HELPERS =================
    public boolean existsByEmail(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public boolean existsByEmailExcludeId(String email, int id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ? AND id_users != ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setInt(2, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    // ================= AUTHENTICATE =================
    public User authenticate(String email, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE email=? AND mot_de_passe=?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        }
        return null;
    }

    public User getById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id_users=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    // ================= MAPPER =================
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id_users"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setEmail(rs.getString("email"));
        user.setTelephone(rs.getString("telephone"));
        user.setMotDePasse(rs.getString("mot_de_passe"));
        user.setBadge(rs.getString("badge_image"));

        Date dob = rs.getDate("date_naissance");
        if (dob != null) {
            user.setDateNaissance(dob.toLocalDate());
        }

        Date sqlDate = rs.getDate("date_inscription");
        user.setDateInscription(sqlDate != null ? sqlDate.toLocalDate() : LocalDate.now());

        String roleStr = rs.getString("role");
        User.Role resolvedRole = User.Role.Patient;
        if (roleStr != null) {
            for (User.Role r : User.Role.values()) {
                if (r.name().equalsIgnoreCase(roleStr)) {
                    resolvedRole = r;
                    break;
                }
            }
        }
        user.setRole(resolvedRole);

        return user;
    }
}
