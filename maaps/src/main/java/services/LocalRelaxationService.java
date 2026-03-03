package services;

import models.LocalRelaxation;
import utils.MyDatabase;
import services.HistoryService;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LocalRelaxationService {

    private final Connection cnx;

    public LocalRelaxationService() {
        cnx = MyDatabase.getInstance().getConnection();
    }


    // ✅ constructeur pour tests (injection connexion)
    public LocalRelaxationService(Connection cnx) {
        this.cnx = cnx;
    }


    public List<LocalRelaxation> getAll() throws SQLException {
        String sql = "SELECT l.id_local, l.nom, l.description, l.type, l.capacite, l.equipements, l.etage, l.duree_max_session, l.tarif_horaire, l.etat, l.disponible, l.image, " +
                     " (SELECT COUNT(*) FROM reservation r " +
                     "   WHERE r.id_local = l.id_local " +
                     "   AND LOWER(TRIM(r.statut)) IN ('en_attente','en attente','confirmer') " +
                     " ) AS active_res_count " +
                     "FROM localrelaxation l ORDER BY l.id_local DESC";
        List<LocalRelaxation> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public LocalRelaxation getById(int idLocal) throws SQLException {
        String sql = "SELECT l.id_local, l.nom, l.description, l.type, l.capacite, l.equipements, l.etage, l.duree_max_session, l.tarif_horaire, l.etat, l.disponible, l.image, " +
                     " (SELECT COUNT(*) FROM reservation r " +
                     "   WHERE r.id_local = l.id_local " +
                     "   AND LOWER(TRIM(r.statut)) IN ('en_attente','en attente','confirmer') " +
                     " ) AS active_res_count " +
                     "FROM localrelaxation l WHERE l.id_local=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idLocal);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public int add(LocalRelaxation l) throws SQLException {
        validate(l);
        // ✅ Par défaut, un local est disponible si son état n'est pas "INDISPONIBLE"
        if (l.getEtat() == null || !l.getEtat().equalsIgnoreCase("INDISPONIBLE")) {
            l.setDisponible(true);
        }
        String sql = "INSERT INTO localrelaxation (nom, description, type, capacite, equipements, etage, duree_max_session, tarif_horaire, etat, disponible, image) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fill(ps, l);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    HistoryService.log("LOCAL_CREATE", "Local ajouté id=" + id + ", nom=" + l.getNom() + ", capacite=" + l.getCapacite());
                    return id;
                }
            }
        }
        return -1;
    }

    public void update(LocalRelaxation l) throws SQLException {
        if (l.getIdLocal() <= 0) throw new IllegalArgumentException("ID local invalide.");
        validate(l);
        String sql = "UPDATE localrelaxation SET nom=?, description=?, type=?, capacite=?, equipements=?, etage=?, duree_max_session=?, tarif_horaire=?, etat=?, disponible=?, image=? " +
                     "WHERE id_local=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            fill(ps, l);
            ps.setInt(12, l.getIdLocal());
            ps.executeUpdate();
        }

        HistoryService.log("LOCAL_UPDATE", "Local modifié id=" + l.getIdLocal() + ", nom=" + l.getNom());
    }

    public void delete(int idLocal) throws SQLException {
        String sql = "DELETE FROM localrelaxation WHERE id_local=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idLocal);
            ps.executeUpdate();
        }

        HistoryService.log("LOCAL_DELETE", "Local supprimé id=" + idLocal);
    }

    private void validate(LocalRelaxation l) {
        if (l.getNom() == null || l.getNom().trim().isEmpty()) throw new IllegalArgumentException("Nom obligatoire.");
        if (l.getDescription() == null || l.getDescription().trim().isEmpty()) throw new IllegalArgumentException("Description obligatoire.");
        if (l.getType() == null || l.getType().trim().isEmpty()) throw new IllegalArgumentException("Type obligatoire.");
        if (l.getCapacite() <= 0) throw new IllegalArgumentException("Capacité doit être > 0.");
        if (l.getEtage() < 0) throw new IllegalArgumentException("Étage invalide.");
        if (l.getDureeMaxSession() <= 0) throw new IllegalArgumentException("Durée max doit être > 0.");
        BigDecimal tarif = l.getTarifHoraire();
        if (tarif == null || tarif.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Tarif invalide.");
        if (l.getEtat() == null || l.getEtat().trim().isEmpty()) throw new IllegalArgumentException("État obligatoire.");
    }

    private void fill(PreparedStatement ps, LocalRelaxation l) throws SQLException {
        ps.setString(1, l.getNom());
        ps.setString(2, l.getDescription());
        ps.setString(3, l.getType());
        ps.setInt(4, l.getCapacite());
        ps.setString(5, l.getEquipements());
        ps.setInt(6, l.getEtage());
        ps.setInt(7, l.getDureeMaxSession());
        ps.setBigDecimal(8, l.getTarifHoraire());
        ps.setString(9, l.getEtat());
        ps.setBoolean(10, l.isDisponible());
        // DB colonne image est souvent NOT NULL -> éviter "Column 'image' cannot be null"
        String img = l.getImage();
        if (img == null || img.trim().isEmpty()) img = "default.png";
        ps.setString(11, img);
    }

    private LocalRelaxation map(ResultSet rs) throws SQLException {
        LocalRelaxation l = new LocalRelaxation();
        l.setIdLocal(rs.getInt("id_local"));
        l.setNom(rs.getString("nom"));
        l.setDescription(rs.getString("description"));
        l.setType(rs.getString("type"));
        l.setCapacite(rs.getInt("capacite"));
        l.setEquipements(rs.getString("equipements"));
        l.setEtage(rs.getInt("etage"));
        l.setDureeMaxSession(rs.getInt("duree_max_session"));
        l.setTarifHoraire(rs.getBigDecimal("tarif_horaire"));
        l.setEtat(rs.getString("etat"));

        // ✅ Métier capacité: indisponible si nb réservations actives >= capacité
        // On calcule la disponibilité à partir de la capacité + nb réservations actives, et on respecte le blocage manuel (etat=INDISPONIBLE).
        int active = 0;
        try { active = rs.getInt("active_res_count"); } catch (SQLException ignore) {}
        boolean manualBlocked = l.getEtat() != null && l.getEtat().equalsIgnoreCase("INDISPONIBLE");
        boolean computed = !manualBlocked && (l.getCapacite() <= 0 || active < l.getCapacite());
        l.setDisponible(computed);
        l.setImage(rs.getString("image"));
        return l;
    }
}
