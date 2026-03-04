package services;

import models.Participation;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParticipationServiceF {
    public ParticipationServiceF() {
        Connection conn = MyDatabase.getInstance().getConnection();
        try {
            DatabaseMetaData dbmd = conn.getMetaData();
            boolean statusExists = false;
            try (ResultSet rs = dbmd.getColumns(null, null, "participation", "statut")) {
                if (rs.next())
                    statusExists = true;
            }

            if (!statusExists) {
                try (Statement st = conn.createStatement()) {
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
                try (Statement st = conn.createStatement()) {
                    st.execute("ALTER TABLE participation ADD COLUMN rating INT DEFAULT 0");
                    System.out.println("Column rating added to participation table.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Migration error in ParticipationService: " + e.getMessage());
        }
    }

    private Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    public void create(Participation p) throws SQLException {
        String query = "INSERT INTO participation (id_users, id_formation, date_inscription, statut) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pst = getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
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

    public List<Participation> findByUserId(int userId) throws SQLException {
        List<Participation> list = new ArrayList<>();
        String query = "SELECT p.*, f.titre as titre_formation, f.description, f.categorie, f.imagePath, f.id_users as id_createur FROM participation p "
                +
                "JOIN formation f ON p.id_formation = f.id_formation " +
                "WHERE p.id_users = ?";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setInt(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToParticipation(rs));
                }
            }
        }
        return list;
    }

    public List<Participation> findByFormationId(int formationId) throws SQLException {
        List<Participation> list = new ArrayList<>();
        String query = "SELECT p.*, u.nom, u.prenom, f.titre as titre_formation FROM participation p " +
                "JOIN users u ON p.id_users = u.id_users " +
                "JOIN formation f ON p.id_formation = f.id_formation " +
                "WHERE p.id_formation = ?";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
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

    public void updateStatut(int idParticipation, String nouveauStatut) throws SQLException {
        String query = "UPDATE participation SET statut = ? WHERE id_participation = ?";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setString(1, nouveauStatut);
            pst.setInt(2, idParticipation);
            pst.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String query = "DELETE FROM participation WHERE id_participation = ?";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }

    public void deleteByUserAndFormation(int userId, int formationId) throws SQLException {
        String query = "DELETE FROM participation WHERE id_users = ? AND id_formation = ?";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, formationId);
            pst.executeUpdate();
        }
    }

    public void updateRating(int userId, int formationId, int rating) throws SQLException {
        String query = "UPDATE participation SET rating = ? WHERE id_users = ? AND id_formation = ?";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
            pst.setInt(1, rating);
            pst.setInt(2, userId);
            pst.setInt(3, formationId);
            pst.executeUpdate();
            System.out.println("Note " + rating + " enregistrée pour l'utilisateur " + userId);
        }
    }

    public boolean hasRated(int userId, int formationId) throws SQLException {
        String query = "SELECT rating FROM participation WHERE id_users = ? AND id_formation = ?";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
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

    public int countParticipants(int formationId) throws SQLException {
        String query = "SELECT COUNT(*) FROM participation WHERE id_formation = ? AND statut = 'accepté'";
        try (PreparedStatement pst = getConnection().prepareStatement(query)) {
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
            p.setDescription(rs.getString("description"));
        } catch (SQLException e) {
        }
        try {
            p.setCategorie(rs.getString("categorie"));
        } catch (SQLException e) {
        }
        try {
            // imagePath is not present in all joins — silently ignore if absent
            p.setImagePath(rs.getString("imagePath"));
        } catch (SQLException e) {
            // ignore
        }
        try {
            p.setIdCreateur(rs.getInt("id_createur"));
        } catch (SQLException e) {
            // ignore
        }
        return p;
    }
}
