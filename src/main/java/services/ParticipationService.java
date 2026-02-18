package services;

import models.Participation;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParticipationService {
    private final Connection connection;

    public ParticipationService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    public void create(Participation p) throws SQLException {
        String query = "INSERT INTO participation (id_users, id_formation, date_inscription) VALUES (?, ?, ?)";
        try (PreparedStatement pst = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, p.getIdUser());
            pst.setInt(2, p.getIdFormation());
            pst.setTimestamp(3, new Timestamp(p.getDateInscription().getTime()));
            pst.executeUpdate();
            try (ResultSet gk = pst.getGeneratedKeys()) {
                if (gk.next())
                    p.setIdParticipation(gk.getInt(1));
            }
        }
    }

    public List<Participation> findByUserId(int userId) throws SQLException {
        List<Participation> list = new ArrayList<>();
        String query = "SELECT p.*, f.titre as titre_formation, f.imagePath FROM participation p " +
                "JOIN formation f ON p.id_formation = f.id_formation " +
                "WHERE p.id_users = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Participation p = new Participation();
                    p.setIdParticipation(rs.getInt("id_participation"));
                    p.setIdUser(rs.getInt("id_users"));
                    p.setIdFormation(rs.getInt("id_formation"));
                    p.setDateInscription(rs.getTimestamp("date_inscription"));
                    p.setStatut(rs.getString("statut"));
                    p.setTitreFormation(rs.getString("titre_formation"));
                    p.setImagePath(rs.getString("imagePath"));
                    list.add(p);
                }
            }
        }
        return list;
    }

    public void delete(int id) throws SQLException {
        String query = "DELETE FROM participation WHERE id_participation = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }
}
