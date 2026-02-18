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
    public void create(User user) throws SQLException {
        String sql = "INSERT INTO users (nom, prenom, email, telephone, age, date_inscription, mot_de_passe, role) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getNom());
            stmt.setString(2, user.getPrenom());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getTelephone());
            stmt.setInt(5, user.getAge());
            stmt.setDate(6, Date.valueOf(
                    user.getDateInscription() != null ? user.getDateInscription() : LocalDate.now()));
            stmt.setString(7, user.getMotDePasse());
            stmt.setString(8, user.getRole().name());
            stmt.executeUpdate();
            System.out.println("User ajouté !");
        }
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
        String sql = "UPDATE users SET nom=?, prenom=?, email=?, telephone=?, age=?, "
                + "date_inscription=?, mot_de_passe=?, role=? WHERE id_users=?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getNom());
            stmt.setString(2, user.getPrenom());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getTelephone());
            stmt.setInt(5, user.getAge());
            stmt.setDate(6, Date.valueOf(
                    user.getDateInscription() != null ? user.getDateInscription() : LocalDate.now()));
            stmt.setString(7, user.getMotDePasse());
            stmt.setString(8, user.getRole().name());
            stmt.setInt(9, user.getId());
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

    // ================= MAPPER =================
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id_users"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setEmail(rs.getString("email"));
        user.setTelephone(rs.getString("telephone"));
        user.setAge(rs.getInt("age"));
        user.setMotDePasse(rs.getString("mot_de_passe"));

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
