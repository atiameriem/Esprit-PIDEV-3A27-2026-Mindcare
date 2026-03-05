package services;

import models.User;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserServiceF implements IService<User> {

    public UserServiceF() {
    }

    private Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    @Override
    public int create(User user) throws SQLException {
        String query = "INSERT INTO users (nom, prenom, email, mot_de_passe, role, date_inscription) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getMot_de_passe());
            ps.setString(5, user.getRole());

            // If date_inscription is null, use current timestamp
            if (user.getDate_inscription() != null) {
                ps.setTimestamp(6, user.getDate_inscription());
            } else {
                ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            }

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    user.setId_users(id);
                    return id;
                }
            }
        }
        return -1;
    }

    @Override
    public void update(User user) throws SQLException {
        String query = "UPDATE users SET nom=?, prenom=?, email=?, mot_de_passe=?, role=? WHERE id_users=?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getMot_de_passe());
            ps.setString(5, user.getRole());
            ps.setInt(6, user.getId_users());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String query = "DELETE FROM users WHERE id_users=?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public User findById(int id) throws SQLException {
        String query = "SELECT * FROM users WHERE id_users=?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id_users"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getString("email"),
                            rs.getString("mot_de_passe"),
                            rs.getString("role"),
                            rs.getTimestamp("date_inscription"));
                }
            }
        }
        return null;
    }

    @Override
    public List<User> read() throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM users";
        try (Statement st = getConnection().createStatement();
                ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id_users"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("email"),
                        rs.getString("mot_de_passe"),
                        rs.getString("role"),
                        rs.getTimestamp("date_inscription")));
            }
        }
        return users;
    }

    // Method for login
    public User login(String email, String password) throws SQLException {
        String query = "SELECT * FROM users WHERE email = ? AND mot_de_passe = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, email);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id_users"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getString("email"),
                            rs.getString("mot_de_passe"),
                            rs.getString("role"),
                            rs.getTimestamp("date_inscription"));
                }
            }
        }
        return null;
    }

    public List<User> findByRole(String role) throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM users WHERE role = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(new User(
                            rs.getInt("id_users"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getString("email"),
                            rs.getString("mot_de_passe"),
                            rs.getString("role"),
                            rs.getTimestamp("date_inscription")));
                }
            }
        }
        return users;
    }
}
