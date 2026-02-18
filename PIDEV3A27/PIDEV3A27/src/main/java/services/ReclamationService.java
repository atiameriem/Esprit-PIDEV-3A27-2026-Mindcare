package services;

import models.Reclamation;
import models.TypeReclamation;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReclamationService {

    private final Connection connection;

    public ReclamationService() {
        connection = MyDatabase.getInstance().getCnx();
    }

    // ================= CREATE =================
    public void create(Reclamation r) throws SQLException {

        String sql = "INSERT INTO reclamation (id_users, type, description, statut, date) VALUES (?, ?, ?, ?, NOW())";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, r.getIdUser());
            stmt.setString(2, r.getType().name());
            stmt.setString(3, r.getDescription());
            stmt.setString(4, r.getStatut() != null ? r.getStatut() : "EN_ATTENTE");

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                r.setId(rs.getInt(1));
            }
        }
    }

    // ================= GET ALL =================
    public List<Reclamation> getAll() throws SQLException {

        List<Reclamation> list = new ArrayList<>();
        String sql = "SELECT * FROM reclamation";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {

                Reclamation r = new Reclamation();

                r.setId(rs.getInt("id_reclamation"));
                r.setIdUser(rs.getInt("id_users"));

                String typeStr = rs.getString("type");
                TypeReclamation type = TypeReclamation.Autre;

                if (typeStr != null) {
                    try {
                        type = TypeReclamation.valueOf(typeStr);
                    } catch (IllegalArgumentException ignored) {}
                }

                r.setType(type);
                r.setDescription(rs.getString("description"));
                r.setStatut(rs.getString("statut"));
                r.setDate(rs.getDate("date"));

                list.add(r);
            }
        }

        return list;
    }

    // ================= UPDATE =================
    public void update(Reclamation r) throws SQLException {

        String sql = "UPDATE reclamation SET id_users = ?, type = ?, description = ?, statut = ? WHERE id_reclamation = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, r.getIdUser());
            stmt.setString(2, r.getType().name());
            stmt.setString(3, r.getDescription());
            stmt.setString(4, r.getStatut());
            stmt.setInt(5, r.getId());

            stmt.executeUpdate();
        }
    }

    // ================= DELETE =================
    public void delete(int id) throws SQLException {

        String sql = "DELETE FROM reclamation WHERE id_reclamation = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }
}
