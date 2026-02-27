package services;

import models.ProgressionModule;
import utils.MyDatabase;
import java.util.List;
import java.sql.*;

public class ProgressionService {

    private final Connection connection;

    public ProgressionService() {
        this.connection = MyDatabase.getInstance().getConnection();

        // Auto-create table if it doesn't exist for convenience
        try (Statement st = this.connection.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS progression_module ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "id_user INT NOT NULL, "
                    + "id_module INT NOT NULL, "
                    + "taux_completion DOUBLE DEFAULT 0.0, "
                    + "temps_passe INT DEFAULT 0, "
                    + "UNIQUE (id_user, id_module))");

            // Migration if column doesn't exist
            try {
                st.execute("ALTER TABLE progression_module ADD COLUMN temps_passe INT DEFAULT 0");
            } catch (SQLException ignored) {
            }

        } catch (SQLException e) {
            System.out.println("Warning: Could not check/create progression_module table: " + e.getMessage());
        }
    }

    public ProgressionModule findByUserAndModule(int id_user, int id_module) throws SQLException {
        String query = "SELECT * FROM progression_module WHERE id_user = ? AND id_module = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id_user);
            ps.setInt(2, id_module);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ProgressionModule(
                            rs.getInt("id"),
                            rs.getInt("id_user"),
                            rs.getInt("id_module"),
                            rs.getDouble("taux_completion"),
                            rs.getInt("temps_passe"));
                }
            }
        }
        return null; // Not found
    }

    public void updateProgression(int id_user, int id_module, double taux, int tempsMinutes) throws SQLException {
        ProgressionModule existing = findByUserAndModule(id_user, id_module);
        if (existing != null) {
            String query = "UPDATE progression_module SET taux_completion = ?, temps_passe = temps_passe + ? WHERE id_user = ? AND id_module = ?";
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setDouble(1, taux);
                ps.setInt(2, tempsMinutes);
                ps.setInt(3, id_user);
                ps.setInt(4, id_module);
                ps.executeUpdate();
            }
        } else {
            String query = "INSERT INTO progression_module (id_user, id_module, taux_completion, temps_passe) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setInt(1, id_user);
                ps.setInt(2, id_module);
                ps.setDouble(3, taux);
                ps.setInt(4, tempsMinutes);
                ps.executeUpdate();
            }
        }
    }

    // Compatibility method
    public void updateProgression(int id_user, int id_module, double taux) throws SQLException {
        updateProgression(id_user, id_module, taux, 0);
    }

    public int getTotalTimeSpent(int id_user) throws SQLException {
        String query = "SELECT SUM(temps_passe) as total FROM progression_module WHERE id_user = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id_user);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt("total");
            }
        }
        return 0;
    }

    public List<ProgressionModule> getAllForUser(int id_user) throws SQLException {
        List<ProgressionModule> list = new java.util.ArrayList<>();
        String query = "SELECT * FROM progression_module WHERE id_user = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id_user);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ProgressionModule(
                            rs.getInt("id"),
                            rs.getInt("id_user"),
                            rs.getInt("id_module"),
                            rs.getDouble("taux_completion"),
                            rs.getInt("temps_passe")));
                }
            }
        }
        return list;
    }

}
