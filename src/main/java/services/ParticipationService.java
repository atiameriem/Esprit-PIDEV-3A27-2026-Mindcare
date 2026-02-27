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
        try {
            DatabaseMetaData dbmd = connection.getMetaData();
            boolean statusExists = false;
            try (ResultSet rs = dbmd.getColumns(null, null, "participation", "statut")) {
                if (rs.next())
                    statusExists = true;
            }

            if (!statusExists) {
                try (Statement st = connection.createStatement()) {
                    st.execute("ALTER TABLE participation ADD COLUMN statut VARCHAR(50) DEFAULT 'en attente'");
                    System.out.println("Column statut added to participation table.");
                }
            }

            // Migration pour la colonne 'note' / 'rating'
            boolean ratingExists = false;
            try (ResultSet rs = dbmd.getColumns(null, null, "participation", "rating")) {
                if (rs.next())
                    ratingExists = true;
            }
            if (!ratingExists) {
                try (Statement st = connection.createStatement()) {
                    st.execute("ALTER TABLE participation ADD COLUMN rating INT DEFAULT 0");
                    System.out.println("Column rating added to participation table.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Migration error in ParticipationService: " + e.getMessage());
        }
    }

    public void create(Participation p) throws SQLException {
        String query = "INSERT INTO participation (id_users, id_formation, date_inscription, statut) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pst = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, p.getIdUser());
            pst.setInt(2, p.getIdFormation());
            pst.setTimestamp(3, new Timestamp(p.getDateInscription().getTime()));
            pst.setString(4, p.getStatut() != null ? p.getStatut() : "en attente");
            pst.executeUpdate();
            try (ResultSet gk = pst.getGeneratedKeys()) {
                if (gk.next())
                    p.setIdParticipation(gk.getInt(1));
            }
        }
    }

    /**
     * Find all participations for a given user (with formation title and image)
     */
    public List<Participation> findByUserId(int userId) throws SQLException {
        List<Participation> list = new ArrayList<>();
        String query = "SELECT p.*, f.titre as titre_formation, f.imagePath FROM participation p " +
                "JOIN formation f ON p.id_formation = f.id_formation " +
                "WHERE p.id_users = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToParticipation(rs));
                }
            }
        }
        return list;
    }

    /**
     * Find all enrollments for a given formation (admin/psy view — returns patient
     * name in titreFormation)
     */
    public List<Participation> findByFormationId(int formationId) throws SQLException {
        List<Participation> list = new ArrayList<>();
        String query = "SELECT p.*, u.nom, u.prenom, f.titre as titre_formation FROM participation p " +
                "JOIN users u ON p.id_users = u.id_users " +
                "JOIN formation f ON p.id_formation = f.id_formation " +
                "WHERE p.id_formation = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, formationId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Participation p = mapResultSetToParticipation(rs);
                    // Override titreFormation with "Nom Prénom" for admin display
                    p.setTitreFormation(rs.getString("nom") + " " + rs.getString("prenom"));
                    list.add(p);
                }
            }
        }
        return list;
    }

    /**
     * Update participation status (accept / refuse)
     */
    public void updateStatut(int idParticipation, String nouveauStatut) throws SQLException {
        String query = "UPDATE participation SET statut = ? WHERE id_participation = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, nouveauStatut);
            pst.setInt(2, idParticipation);
            pst.executeUpdate();
        }
    }

    /**
     * Delete a participation by ID
     */
    public void delete(int id) throws SQLException {
        String query = "DELETE FROM participation WHERE id_participation = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }

    /**
     * Noter une formation (1 à 5 stars)
     */
    public void updateRating(int userId, int formationId, int rating) throws SQLException {
        String query = "UPDATE participation SET rating = ? WHERE id_users = ? AND id_formation = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, rating);
            pst.setInt(2, userId);
            pst.setInt(3, formationId);
            pst.executeUpdate();
            System.out.println("Note " + rating + " enregistrée pour l'utilisateur " + userId);
        }
    }

    public boolean hasRated(int userId, int formationId) throws SQLException {
        String query = "SELECT rating FROM participation WHERE id_users = ? AND id_formation = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, formationId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("rating") > 0;
                }
            }
        }
        return false;
    }

    /**
     * Count the number of accepted participants for a given formation
     */
    public int countParticipants(int formationId) throws SQLException {
        String query = "SELECT COUNT(*) FROM participation WHERE id_formation = ? AND statut = 'accepté'";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, formationId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private Participation mapResultSetToParticipation(ResultSet rs) throws SQLException {
        Participation p = new Participation();
        p.setIdParticipation(rs.getInt("id_participation"));
        p.setIdUser(rs.getInt("id_users"));
        p.setIdFormation(rs.getInt("id_formation"));
        p.setDateInscription(rs.getTimestamp("date_inscription"));
        p.setStatut(rs.getString("statut"));
        p.setTitreFormation(rs.getString("titre_formation"));
        p.setRating(rs.getInt("rating"));
        try {
            // imagePath is not present in all joins — silently ignore if absent
            p.setImagePath(rs.getString("imagePath"));
        } catch (SQLException e) {
            // ignore
        }
        return p;
    }
}
