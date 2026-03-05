package services;

import models.User;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IService<User> {

    private Connection getConnection() {
        Connection conn = MyDatabase.getInstance().getConnection();
        if (conn == null) {
            System.err.println("❌ UserService: Connection is null! Check MyDatabase configuration.");
        }
        return conn;
    }

    // ================= CREATE =================
    @Override
    public int create(User user) throws SQLException {
        String sql = "INSERT INTO users (nom, prenom, email, telephone, date_inscription, mot_de_passe, role, badge_image, date_naissance) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getNom());
            stmt.setString(2, user.getPrenom());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getTelephone());
            stmt.setDate(5, Date.valueOf(
                    user.getDateInscription() != null ? user.getDateInscription() : LocalDate.now()));
            stmt.setString(6, user.getMotDePasse());
            stmt.setString(7, user.getRole() != null ? user.getRole().name() : User.Role.Patient.name());
            stmt.setString(8, user.getBadge());
            stmt.setDate(9, user.getDateNaissance() != null ? Date.valueOf(user.getDateNaissance()) : null);

            System.out.println("💾 Inserting User: " + user.getNom() + ", Role: "
                    + (user.getRole() != null ? user.getRole().name() : "NULL"));
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                }
            }
        }
        return user.getId();
    }

    // ================= UPDATE =================
    @Override
    public void update(User user) throws SQLException {
        String sql = "UPDATE users SET nom=?, prenom=?, email=?, telephone=?, "
                + "date_inscription=?, mot_de_passe=?, role=?, badge_image=?, date_naissance=? WHERE id_users=?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, user.getNom());
            stmt.setString(2, user.getPrenom());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getTelephone());
            stmt.setDate(5, Date.valueOf(
                    user.getDateInscription() != null ? user.getDateInscription() : LocalDate.now()));
            stmt.setString(6, user.getMotDePasse());
            stmt.setString(7, user.getRole() != null ? user.getRole().name() : User.Role.Patient.name());
            stmt.setString(8, user.getBadge());
            stmt.setDate(9, user.getDateNaissance() != null ? Date.valueOf(user.getDateNaissance()) : null);
            stmt.setInt(10, user.getId());
            stmt.executeUpdate();
        }
    }

    // ================= DELETE =================
    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE id_users=?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // ================= READ =================
    @Override
    public List<User> read() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        Connection conn = getConnection();
        if (conn == null)
            return users;

        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    // ================= AUTHENTICATE =================
    public User authenticate(String email, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE email=? AND mot_de_passe=?";
        Connection conn = getConnection();
        if (conn == null)
            return null;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, password);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    public boolean existsByEmail(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        Connection conn = getConnection();
        if (conn == null)
            return false;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public boolean existsByEmailExcludeId(String email, int id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ? AND id_users != ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setInt(2, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public boolean existsByPhone(String phone) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE telephone = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, phone);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public User getByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email=?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return mapResultSetToUser(rs);
            }
        }
        return null;
    }

    public User getById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id_users=?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return mapResultSetToUser(rs);
            }
        }
        return null;
    }

    /**
     * Compatibilité Forum — récupère le rôle (String) depuis la DB.
     * Utilisé par utils.forum.Permissions en fallback.
     */
    public String fetchRoleFromDb(int idUsers, String email) {
        final String sqlById = "SELECT role FROM users WHERE id_users = ? LIMIT 1";
        final String sqlByEmail = "SELECT role FROM users WHERE email = ? LIMIT 1";
        try {
            Connection c = getConnection();
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

    /**
     * Compatibilité Forum — récupère l'email d'un utilisateur par ID.
     */
    public String fetchEmailById(int idUsers) throws SQLException {
        String sql = "SELECT email FROM users WHERE id_users=? LIMIT 1";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, idUsers);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("email");
            }
        }
        return null;
    }

    public void updatePassword(String email, String newPassword) throws SQLException {
        String sql = "UPDATE users SET mot_de_passe = ? WHERE email = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, newPassword);
            stmt.setString(2, email);
            stmt.executeUpdate();
            System.out.println("Mot de passe mis à jour pour : " + email);
        }
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
        if (dob != null)
            user.setDateNaissance(dob.toLocalDate());

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
