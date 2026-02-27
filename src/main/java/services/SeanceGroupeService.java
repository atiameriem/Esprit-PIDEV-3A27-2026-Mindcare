package services;

import models.SeanceGroupe;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SeanceGroupeService implements IService<SeanceGroupe> {

    private final Connection connection;

    public SeanceGroupeService() {
        connection = MyDatabase.getInstance().getConnection();
        try {
            DatabaseMetaData dbmd = connection.getMetaData();

            // Vérifier si la table existe
            boolean tableExists = false;
            try (ResultSet rs = dbmd.getTables(null, null, "seance_groupe", null)) {
                if (rs.next())
                    tableExists = true;
            }

            if (!tableExists) {
                try (Statement st = connection.createStatement()) {
                    st.execute("""
                                CREATE TABLE seance_groupe (
                                    id INT PRIMARY KEY AUTO_INCREMENT,
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
                                    seance_id INT NOT NULL,
                                    user_id INT NOT NULL,
                                    PRIMARY KEY (seance_id, user_id),
                                    FOREIGN KEY (seance_id) REFERENCES seance_groupe(id) ON DELETE CASCADE,
                                    FOREIGN KEY (user_id) REFERENCES users(id_users) ON DELETE CASCADE
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
                    try (Statement st = connection.createStatement()) {
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
        try (PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
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
                    s.setId(generatedId);
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
                "WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, s.getTitre());
            ps.setInt(2, s.getIdFormation());
            ps.setTimestamp(3, Timestamp.valueOf(s.getDateHeure()));
            ps.setInt(4, s.getDureeMinutes());
            ps.setString(5, s.getLienJitsi());
            ps.setString(6, s.getStatut());
            ps.setString(7, s.getDescription());
            ps.setInt(8, s.getCapaciteMax());
            ps.setString(9, s.getGoogleEventId());
            ps.setInt(10, s.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String query = "DELETE FROM seance_groupe WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<SeanceGroupe> read() throws SQLException {
        List<SeanceGroupe> liste = new ArrayList<>();
        String query = "SELECT s.*, f.titre as titre_formation FROM seance_groupe s " +
                "JOIN formation f ON s.id_formation = f.id_formation";
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(query)) {
            while (rs.next())
                liste.add(mapper(rs));
        }
        return liste;
    }

    // 🔍 Par formation
    public List<SeanceGroupe> findByFormation(int idFormation) throws SQLException {
        List<SeanceGroupe> liste = new ArrayList<>();
        String query = "SELECT s.*, f.titre as titre_formation FROM seance_groupe s " +
                "JOIN formation f ON s.id_formation = f.id_formation " +
                "WHERE s.id_formation=? ORDER BY s.date_heure";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
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
        String query = "SELECT s.*, f.titre as titre_formation FROM seance_groupe s " +
                "JOIN formation f ON s.id_formation = f.id_formation " +
                "WHERE s.id_users=? ORDER BY s.date_heure";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
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
        String query = "SELECT s.*, f.titre as titre_formation FROM seance_groupe s " +
                "JOIN formation f ON s.id_formation = f.id_formation " +
                "WHERE s.id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return mapper(rs);
            }
        }
        return null;
    }

    // 🔄 Mettre à jour statut
    public void updateStatut(int id, String statut) throws SQLException {
        String query = "UPDATE seance_groupe SET statut=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, statut);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    // 🔗 Mettre à jour lien Jitsi
    public void updateLienJitsi(int id, String lien) throws SQLException {
        String query = "UPDATE seance_groupe SET lien_jitsi=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, lien);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    // 🔢 Compter participants
    public int compterParticipants(int seanceId) {
        String query = "SELECT COUNT(*) FROM seance_participant WHERE seance_id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, seanceId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("compterParticipants: " + e.getMessage());
        }
        return 0;
    }

    private SeanceGroupe mapper(ResultSet rs) throws SQLException {
        SeanceGroupe s = new SeanceGroupe();
        s.setId(rs.getInt("id"));
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
        return s;
    }
}