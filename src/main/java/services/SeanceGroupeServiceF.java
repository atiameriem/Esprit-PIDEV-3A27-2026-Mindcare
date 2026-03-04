package services;

import models.SeanceGroupe;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SeanceGroupeServiceF implements IService<SeanceGroupe> {

    public SeanceGroupeServiceF() {
        try {
            Connection connection = MyDatabase.getInstance().getConnection();
            DatabaseMetaData dbmd = connection.getMetaData();

            // Vérifier si la table existe
            boolean tableExists = false;
            try (ResultSet rs = dbmd.getTables(null, null, "seance_groupe", null)) {
                if (rs.next())
                    tableExists = true;
            }

            if (!tableExists) {
                try (Statement st = MyDatabase.getInstance().getConnection().createStatement()) {
                    st.execute("""
                                CREATE TABLE seance_groupe (
                                    seance_id INT PRIMARY KEY AUTO_INCREMENT,
                                    titre VARCHAR(200) NOT NULL,
                                    id_formation INT NOT NULL,
                                    id_users INT NOT NULL,
                                    date_heure DATETIME NOT NULL,
                                    duree_minutes INT DEFAULT 90,
                                    lien_jitsi VARCHAR(300),
                                    statut ENUM('PLANIFIEE','EN_COURS','TERMINEE','ANNULEE') DEFAULT 'PLANIFIEE',
                                    description TEXT,
                                    capacite_max INT DEFAULT 20,
                                    google_event_id VARCHAR(255),
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    FOREIGN KEY (id_formation) REFERENCES formation(id_formation),
                                    FOREIGN KEY (id_users) REFERENCES users(id_users)
                                )
                            """);
                    st.execute("""
                                CREATE TABLE seance_participant (
                                    id INT PRIMARY KEY AUTO_INCREMENT,
                                    seance_id INT NOT NULL,
                                    id_users INT NOT NULL,
                                    statut VARCHAR(50) DEFAULT 'EN_LIGNE',
                                    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    FOREIGN KEY (seance_id) REFERENCES seance_groupe(seance_id) ON DELETE CASCADE,
                                    FOREIGN KEY (id_users) REFERENCES users(id_users) ON DELETE CASCADE
                                )
                            """);
                    System.out.println("Tables seance_groupe et seance_participant créées.");
                }
            } else {
                // Vérifier si la colonne google_event_id existe
                boolean columnExists = false;
                try (ResultSet rsCol = dbmd.getColumns(null, null, "seance_groupe", "google_event_id")) {
                    if (rsCol.next())
                        columnExists = true;
                }
                if (!columnExists) {
                    try (Statement st = MyDatabase.getInstance().getConnection().createStatement()) {
                        st.execute("ALTER TABLE seance_groupe ADD COLUMN google_event_id VARCHAR(255)");
                        System.out.println("Colonne google_event_id ajoutée à seance_groupe.");
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Migration error in SeanceGroupeService: " + e.getMessage());
        }
    }

    @Override
    public int create(SeanceGroupe s) throws SQLException {
        String query = "INSERT INTO seance_groupe (titre, id_formation, id_users, date_heure, " +
                "duree_minutes, lien_jitsi, statut, description, capacite_max, google_event_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = MyDatabase.getInstance().getConnection();
                PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getTitre());
            ps.setInt(2, s.getIdFormation());
            ps.setInt(3, s.getIdUsers());
            ps.setTimestamp(4, Timestamp.valueOf(s.getDateHeure()));
            ps.setInt(5, s.getDureeMinutes());
            ps.setString(6, s.getLienJitsi());
            ps.setString(7, s.getStatut() != null ? s.getStatut() : "PLANIFIEE");
            ps.setString(8, s.getDescription());
            ps.setInt(9, s.getCapaciteMax());
            ps.setString(10, s.getGoogleEventId());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0)
                throw new SQLException("Création séance échouée, aucune ligne affectée.");

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    s.setSeanceId(generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Création séance échouée, aucun ID obtenu.");
                }
            }
        }
    }

    @Override
    public void update(SeanceGroupe s) throws SQLException {
        String query = "UPDATE seance_groupe SET titre=?, id_formation=?, date_heure=?, " +
                "duree_minutes=?, lien_jitsi=?, statut=?, description=?, capacite_max=?, google_event_id=? " +
                "WHERE seance_id=?";
        try (Connection connection = MyDatabase.getInstance().getConnection();
                PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, s.getTitre());
            ps.setInt(2, s.getIdFormation());
            ps.setTimestamp(3, Timestamp.valueOf(s.getDateHeure()));
            ps.setInt(4, s.getDureeMinutes());
            ps.setString(5, s.getLienJitsi());
            ps.setString(6, s.getStatut());
            ps.setString(7, s.getDescription());
            ps.setInt(8, s.getCapaciteMax());
            ps.setString(9, s.getGoogleEventId());
            ps.setInt(10, s.getSeanceId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String query = "DELETE FROM seance_groupe WHERE seance_id=?";
        try (Connection connection = MyDatabase.getInstance().getConnection();
                PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<SeanceGroupe> read() throws SQLException {
        List<SeanceGroupe> liste = new ArrayList<>();
        String query = "SELECT s.*, f.titre as titre_formation, CONCAT(u.prenom, ' ', u.nom) as psychoName " +
                "FROM seance_groupe s " +
                "JOIN formation f ON s.id_formation = f.id_formation " +
                "LEFT JOIN users u ON s.id_users = u.id_users";
        try (Connection connection = MyDatabase.getInstance().getConnection();
                Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(query)) {
            while (rs.next())
                liste.add(mapper(rs));
        }
        return liste;
    }

    // 🔍 Par formation
    public List<SeanceGroupe> findByFormation(int idFormation) throws SQLException {
        List<SeanceGroupe> liste = new ArrayList<>();
        String query = "SELECT s.*, f.titre as titre_formation, CONCAT(u.prenom, ' ', u.nom) as psychoName " +
                "FROM seance_groupe s " +
                "JOIN formation f ON s.id_formation = f.id_formation " +
                "LEFT JOIN users u ON s.id_users = u.id_users " +
                "WHERE s.id_formation=? ORDER BY s.date_heure";
        try (Connection connection = MyDatabase.getInstance().getConnection();
                PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, idFormation);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    liste.add(mapper(rs));
            }
        } catch (SQLException e) {
            System.out.println("findByFormation: " + e.getMessage());
        }
        return liste;
    }

    // 🔍 Par psychologue
    public List<SeanceGroupe> findByPsychologue(int idUsers) throws SQLException {
        List<SeanceGroupe> liste = new ArrayList<>();
        String query = "SELECT s.*, f.titre as titre_formation, CONCAT(u.prenom, ' ', u.nom) as psychoName " +
                "FROM seance_groupe s " +
                "JOIN formation f ON s.id_formation = f.id_formation " +
                "LEFT JOIN users u ON s.id_users = u.id_users " +
                "WHERE s.id_users=? ORDER BY s.date_heure";
        try (Connection connection = MyDatabase.getInstance().getConnection();
                PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, idUsers);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    liste.add(mapper(rs));
            }
        } catch (SQLException e) {
            System.out.println("findByPsychologue: " + e.getMessage());
        }
        return liste;
    }

    // 🔍 Par ID
    public SeanceGroupe findById(int id) throws SQLException {
        String query = "SELECT s.*, f.titre as titre_formation, CONCAT(u.prenom, ' ', u.nom) as psychoName " +
                "FROM seance_groupe s " +
                "JOIN formation f ON s.id_formation = f.id_formation " +
                "LEFT JOIN users u ON s.id_users = u.id_users " +
                "WHERE s.seance_id=?";
        try (Connection connection = MyDatabase.getInstance().getConnection();
                PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return mapper(rs);
            }
        }
        return null;
    }

    // 🔑 Mettre à jour uniquement le Google Event ID
    public void updateGoogleEventId(int seanceId, String googleEventId) {
        String query = "UPDATE seance_groupe SET google_event_id=? WHERE seance_id=?";
        try (Connection connection = MyDatabase.getInstance().getConnection();
                PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, googleEventId);
            ps.setInt(2, seanceId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println(
                        "✅ google_event_id sauvegardé en DB pour seance_id=" + seanceId + " : " + googleEventId);
            } else {
                System.err.println("⚠️ updateGoogleEventId : aucune ligne mise à jour pour seance_id=" + seanceId);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur updateGoogleEventId : " + e.getMessage());
        }
    }

    // 🔄 Mettre à jour statut
    public void updateStatut(int id, String statut) throws SQLException {
        String query = "UPDATE seance_groupe SET statut=? WHERE seance_id=?";
        try (Connection connection = MyDatabase.getInstance().getConnection();
                PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, statut);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    // 🔗 Mettre à jour lien Jitsi
    public void updateLienJitsi(int id, String lien) throws SQLException {
        String query = "UPDATE seance_groupe SET lien_jitsi=? WHERE seance_id=?";
        try (Connection connection = MyDatabase.getInstance().getConnection();
                PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, lien);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    // 🔢 Compter participants
    public int compterParticipants(int seanceId) {
        String query = "SELECT COUNT(*) FROM seance_participant WHERE seance_id=?";
        try (Connection connection = MyDatabase.getInstance().getConnection();
                PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, seanceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("compterParticipants: " + e.getMessage());
        }
        return 0;
    }

    // ➕ Ajouter un participant
    public boolean isUserPresent(int seanceId, int userId) {
        String query = "SELECT 1 FROM seance_participant WHERE seance_id=? AND id_users=?";
        try (Connection connection = MyDatabase.getInstance().getConnection();
                PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, seanceId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public void ajouterParticipant(int seanceId, int userId) throws SQLException {
        // Vérifier si déjà présent
        String check = "SELECT 1 FROM seance_participant WHERE seance_id=? AND id_users=?";
        try (Connection connection = MyDatabase.getInstance().getConnection();
                PreparedStatement ps = connection.prepareStatement(check)) {
            ps.setInt(1, seanceId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return; // Déjà enregistré
            }
        }

        String query = "INSERT INTO seance_participant (seance_id, id_users) VALUES (?, ?)";
        try (Connection connection = MyDatabase.getInstance().getConnection();
                PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, seanceId);
            ps.setInt(2, userId);
            int res = ps.executeUpdate();
            System.out.println("[DB] Insertion participant : " + res + " ligne(s) ajoutée(s).");
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Erreur insertion participant : " + e.getMessage());
            throw e;
        }
    }

    // ➖ Retirer un participant
    public void retirerParticipant(int seanceId, int userId) throws SQLException {
        String query = "DELETE FROM seance_participant WHERE seance_id=? AND id_users=?";
        try (Connection connection = MyDatabase.getInstance().getConnection();
                PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, seanceId);
            ps.setInt(2, userId);
            int res = ps.executeUpdate();
            System.out.println("[DB] Retrait participant : " + res + " ligne(s) supprimée(s).");
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Erreur retrait participant : " + e.getMessage());
            throw e;
        }
    }

    // 👥 Liste des noms
    public List<String> getNomsParticipants(int seanceId) {
        List<String> noms = new ArrayList<>();
        String query = "SELECT CONCAT(u.prenom, ' ', u.nom) FROM seance_participant sp " +
                "JOIN users u ON sp.id_users = u.id_users " +
                "WHERE sp.seance_id=?";
        try (Connection connection = MyDatabase.getInstance().getConnection();
                PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, seanceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    noms.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            System.out.println("getNomsParticipants: " + e.getMessage());
        }
        return noms;
    }

    private SeanceGroupe mapper(ResultSet rs) throws SQLException {
        SeanceGroupe s = new SeanceGroupe();
        s.setSeanceId(rs.getInt("seance_id"));
        s.setTitre(rs.getString("titre"));
        s.setIdFormation(rs.getInt("id_formation"));
        s.setIdUsers(rs.getInt("id_users"));
        s.setDateHeure(rs.getTimestamp("date_heure").toLocalDateTime());
        s.setDureeMinutes(rs.getInt("duree_minutes"));
        s.setStatut(rs.getString("statut"));
        s.setDescription(rs.getString("description"));
        s.setCapaciteMax(rs.getInt("capacite_max"));
        s.setGoogleEventId(rs.getString("google_event_id"));
        try {
            s.setTitreFormation(rs.getString("titre_formation"));
        } catch (SQLException e) {
            // silent ignore
        }
        try {
            s.setLienJitsi(rs.getString("lien_jitsi"));
        } catch (SQLException e) {
            s.setLienJitsi(null); // safe fallback
        }
        try {
            s.setPsychoName(rs.getString("psychoName"));
        } catch (SQLException e) {
            // silent ignore
        }
        return s;
    }
}