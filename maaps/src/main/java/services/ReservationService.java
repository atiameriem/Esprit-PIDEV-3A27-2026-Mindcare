package services;

import models.Reservation;
import utils.MyDatabase;
import utils.UserSession;
import utils.QrCodeUtil;
import utils.QrPayloadUtil;
import services.HistoryService;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {

    private final Connection cnx;

    public ReservationService() {
        cnx = MyDatabase.getInstance().getConnection();
        ensureSchema();
    }

    // ✅ constructeur pour tests unitaires (injection d'une connexion mockée)
    public ReservationService(Connection cnx) {
        this.cnx = cnx;
        ensureSchema();
    }

    /**
     * ✅ Métier:
     * - Ajoute un champ created_at pour pouvoir annuler automatiquement les réservations en attente > 24h
     * - Lance une purge des réservations expirées (EN_ATTENTE) au démarrage du service
     */
    private void ensureSchema() {
        try {
            ensureCreatedAtColumn();
            cancelExpiredPendingReservations(); // purge "best effort"
        } catch (Exception e) {
            // ne pas bloquer l'application si la BD n'est pas prête
            System.err.println("⚠️ ensureSchema: " + e.getMessage());
        }
    }

    private void ensureCreatedAtColumn() throws SQLException {
        DatabaseMetaData meta = cnx.getMetaData();
        try (ResultSet cols = meta.getColumns(null, null, "reservation", "created_at")) {
            if (cols.next()) return; // existe déjà
        }
        // colonne absente => on l'ajoute
        String alter = "ALTER TABLE reservation ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP";
        try (Statement st = cnx.createStatement()) {
            st.execute(alter);
            // Remplir les lignes existantes si besoin
            try (Statement st2 = cnx.createStatement()) {
                // best effort: si ancienne data, on essaye de backfill depuis date_reservation + heure_debut
                st2.executeUpdate(
                        "UPDATE reservation " +
                        "SET created_at = COALESCE(created_at, STR_TO_DATE(CONCAT(date_reservation,' ',heure_debut), '%Y-%m-%d %H:%i:%s')) " +
                        "WHERE created_at IS NULL OR created_at = '0000-00-00 00:00:00'"
                );
                st2.executeUpdate(
                        "UPDATE reservation SET created_at = NOW() " +
                        "WHERE created_at IS NULL OR created_at = '0000-00-00 00:00:00'"
                );
            }
        }
    }

    /**
     * ✅ Métier d’annulation automatique :
     * si une réservation reste EN_ATTENTE > 48h (created_at), le système l'annule automatiquement.
     */
    public int cancelExpiredPendingReservations() throws SQLException {
        // ✅ robuste aux variations (en_attente, EN ATTENTE, espaces, etc.)
        // ✅ fallback: si created_at NULL (ancienne data), on retombe sur date_reservation+heure_debut
        String sql = "UPDATE reservation " +
                "SET statut='Annuler' " +
                "WHERE LOWER(TRIM(statut)) IN ('en_attente','en attente') " +
                "AND COALESCE(created_at, STR_TO_DATE(CONCAT(date_reservation,' ',heure_debut), '%Y-%m-%d %H:%i:%s')) " +
                "<= (NOW() - INTERVAL 48 HOUR)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            int updated = ps.executeUpdate();
            if (updated > 0) {
                // recalcul disponibilité pour tous les locaux touchés (simple: tout recalculer)
                refreshAllLocauxAvailability();
            }
            return updated;
        }
    }

    private void refreshAllLocauxAvailability() throws SQLException {
        String sql = "SELECT id_local FROM localrelaxation";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                updateLocalAvailability(rs.getInt(1));
            }
        }
    }

    /**
     * ✅ Métier capacité:
     * - Si nb réservations actives (EN_ATTENTE ou Confirmer) >= capacité => local indisponible
     * - ExcludeId permet d'exclure une réservation (cas modification)
     */
    private void ensureLocalCapacity(int idLocal, Integer excludeIdReservation) throws SQLException {
        int capacite = getLocalCapacity(idLocal);
        int active = countActiveReservations(idLocal, excludeIdReservation);
        if (capacite > 0 && active >= capacite) {
            updateLocalAvailability(idLocal);
            throw new IllegalArgumentException("Local complet: capacité atteinte (" + capacite + ").");
        }
        // sinon on garde la colonne 'disponible' à jour (en cas de libération)
        updateLocalAvailability(idLocal);
    }

    private int getLocalCapacity(int idLocal) throws SQLException {
        String sql = "SELECT capacite FROM localrelaxation WHERE id_local=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idLocal);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private String getLocalEtat(int idLocal) throws SQLException {
        String sql = "SELECT etat FROM localrelaxation WHERE id_local=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idLocal);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }
        return null;
    }

    private int countActiveReservations(int idLocal, Integer excludeIdReservation) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reservation " +
                "WHERE id_local=? " +
                "AND LOWER(TRIM(statut)) IN ('en_attente','en attente','confirmer') " +
                (excludeIdReservation == null ? "" : "AND id_reservation<>?");
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idLocal);
            if (excludeIdReservation != null) ps.setInt(2, excludeIdReservation);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private void updateLocalAvailability(int idLocal) throws SQLException {
        int capacite = getLocalCapacity(idLocal);
        int active = countActiveReservations(idLocal, null);
        String etat = getLocalEtat(idLocal);

        boolean manualBlocked = etat != null && etat.equalsIgnoreCase("INDISPONIBLE");
        boolean disponible = !manualBlocked && (capacite <= 0 || active < capacite);

        String sql = "UPDATE localrelaxation SET disponible=? WHERE id_local=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setBoolean(1, disponible);
            ps.setInt(2, idLocal);
            ps.executeUpdate();
        }
    }




    public List<Reservation> getAllWithLocalForUser(int userId, boolean isAdminLike) throws SQLException {
        // ✅ purge automatique (best effort) avant lecture
        try { cancelExpiredPendingReservations(); } catch (Exception ignore) {}

        String base = "SELECT r.id_reservation, r.date_reservation, r.heure_debut, r.heure_fin, r.statut, r.type_session, r.motif, " +
                "r.id_local, r.id_utilisateur, r.id_responsable_centre, r.id_historique_utlise, " +
                "l.nom AS local_nom, l.type AS local_type, l.disponible AS local_disponible " +
                "FROM reservation r INNER JOIN localrelaxation l ON r.id_local = l.id_local ";
        String where = isAdminLike ? "" : "WHERE r.id_utilisateur = ? ";
        String order = "ORDER BY r.date_reservation DESC, r.heure_debut DESC";
        List<Reservation> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(base + where + order)) {
            if (!isAdminLike) ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /**
     * Retourne les réservations (avec infos local) qui démarrent dans l'intervalle [start, end).
     * Utilisé pour la synchro calendrier (Nylas) et les rappels.
     */
    public List<Reservation> listBetween(java.time.LocalDateTime start, java.time.LocalDateTime end) throws SQLException {
        // ✅ purge automatique (best effort) avant lecture
        try { cancelExpiredPendingReservations(); } catch (Exception ignore) {}

        if (start == null || end == null) throw new IllegalArgumentException("start/end requis");

        java.time.LocalDate sd = start.toLocalDate();
        java.time.LocalDate ed = end.toLocalDate();

        String sql = "SELECT r.id_reservation, r.date_reservation, r.heure_debut, r.heure_fin, r.statut, r.type_session, r.motif, " +
                "r.id_local, r.id_utilisateur, r.id_responsable_centre, r.id_historique_utlise, " +
                "l.nom AS local_nom, l.type AS local_type, l.disponible AS local_disponible " +
                "FROM reservation r INNER JOIN localrelaxation l ON r.id_local = l.id_local " +
                "WHERE r.date_reservation >= ? AND r.date_reservation <= ? " +
                "ORDER BY r.date_reservation ASC, r.heure_debut ASC";

        List<Reservation> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(sd));
            ps.setDate(2, Date.valueOf(ed));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Reservation r = map(rs);
                    // Filtrage fin en Java (heure incluse)
                    var dt = r.getStartDateTime();
                    if (dt != null && (dt.isEqual(start) || (dt.isAfter(start) && dt.isBefore(end)))) {
                        list.add(r);
                    }
                }
            }
        }
        return list;
    }





    public Reservation getById(int idReservation) throws SQLException {
        // ✅ purge automatique (best effort) avant lecture
        try { cancelExpiredPendingReservations(); } catch (Exception ignore) {}

        String sql = "SELECT r.id_reservation, r.date_reservation, r.heure_debut, r.heure_fin, r.statut, r.type_session, r.motif, " +
                "r.id_local, r.id_utilisateur, r.id_responsable_centre, r.id_historique_utlise, " +
                "l.nom AS local_nom, l.type AS local_type, l.disponible AS local_disponible " +
                "FROM reservation r INNER JOIN localrelaxation l ON r.id_local = l.id_local WHERE r.id_reservation=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idReservation);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public int add(Reservation r) throws SQLException {
        validate(r);
        if (hasOverlap(r.getIdLocal(), r.getDateReservation(), r.getHeureDebut(), r.getHeureFin(), null)) {
            throw new IllegalArgumentException("Conflit: une réservation existe déjà pour ce local dans ce créneau.");
        }

        // ✅ Métier: capacité du local (si nb réservations actives >= capacité => indisponible)
        ensureLocalCapacity(r.getIdLocal(), null);

        // ✅ patient => statut forcé EN_ATTENTE
        if (UserSession.isPatient()) {
            r.setStatut("EN_ATTENTE");
        }
        if (r.getStatut() == null || r.getStatut().isBlank()) r.setStatut("EN_ATTENTE");

        String sql = "INSERT INTO reservation (date_reservation, heure_debut, heure_fin, statut, type_session, motif, id_local, id_utilisateur, id_responsable_centre, id_historique_utlise) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, Date.valueOf(r.getDateReservation()));
            ps.setTime(2, Time.valueOf(r.getHeureDebut()));
            ps.setTime(3, Time.valueOf(r.getHeureFin()));
            ps.setString(4, r.getStatut());
            ps.setString(5, r.getTypeSession());
            ps.setString(6, r.getMotif());
            ps.setInt(7, r.getIdLocal());
            ps.setInt(8, r.getIdUtilisateur());
            if (r.getIdResponsableCentre() == null) ps.setNull(9, Types.INTEGER);
            else ps.setInt(9, r.getIdResponsableCentre());
            if (r.getIdHistoriqueUtilise() == null) ps.setNull(10, Types.INTEGER);
            else ps.setInt(10, r.getIdHistoriqueUtilise());
            ps.executeUpdate();

            // ✅ recalcul disponibilité du local
            updateLocalAvailability(r.getIdLocal());

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);

                    // ✅ Historique (sans table)
                    HistoryService.log(
                            "RESERVATION_CREATE",
                            "Reservation ajoutée id=" + id + ", local=" + r.getIdLocal() + ", date=" + r.getDateReservation() +
                                    ", " + r.getHeureDebut() + "-" + r.getHeureFin() + ", statut=" + r.getStatut()
                    );

                    // ✅ Dès qu'il y a une réservation -> local indisponible
                    recomputeLocalDisponibilite(r.getIdLocal());

                    // ✅ Calendar: création événement lié à la réservation
                    try {
                        CalendarService cal = new CalendarService(cnx);
                        if (r.getDateReservation() != null && r.getHeureDebut() != null && r.getHeureFin() != null) {
                            String title = "Réservation • Local #" + r.getIdLocal();
                            var start = java.time.LocalDateTime.of(r.getDateReservation(), r.getHeureDebut());
                            var end = java.time.LocalDateTime.of(r.getDateReservation(), r.getHeureFin());
                            cal.upsertForReservation(id, title, start, end, "ACTIVE");
                        }
                    } catch (Exception e) {
                        System.err.println("⚠️ Calendar non créé: " + e.getMessage());
                    }

                    return id;
                }
            }
        }
        return -1;
    }

    public void update(Reservation r) throws SQLException {
        if (r.getIdReservation() <= 0) throw new IllegalArgumentException("ID réservation invalide.");
        validate(r);

        Reservation current = getById(r.getIdReservation());
        if (current == null) throw new IllegalArgumentException("Réservation introuvable.");

        // ✅ interdit si status final
        if (current.isLocked()) {
            throw new IllegalArgumentException("Réservation verrouillée (statut confirmé/annulé).");
        }

        if (hasOverlap(r.getIdLocal(), r.getDateReservation(), r.getHeureDebut(), r.getHeureFin(), r.getIdReservation())) {
            throw new IllegalArgumentException("Conflit: une réservation existe déjà pour ce local dans ce créneau.");
        }

        // ✅ Métier: capacité du local (en excluant la réservation en cours de modification)
        ensureLocalCapacity(r.getIdLocal(), r.getIdReservation());

        // ✅ patient ne peut pas changer le statut
        if (UserSession.isPatient()) {
            r.setStatut(current.getStatut() == null ? "EN_ATTENTE" : current.getStatut());
            r.setIdResponsableCentre(current.getIdResponsableCentre());
        }

        String sql = "UPDATE reservation SET date_reservation=?, heure_debut=?, heure_fin=?, type_session=?, motif=?, id_historique_utlise=? WHERE id_reservation=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(r.getDateReservation()));
            ps.setTime(2, Time.valueOf(r.getHeureDebut()));
            ps.setTime(3, Time.valueOf(r.getHeureFin()));
            ps.setString(4, r.getTypeSession());
            ps.setString(5, r.getMotif());
            if (r.getIdHistoriqueUtilise() == null) ps.setNull(6, Types.INTEGER);
            else ps.setInt(6, r.getIdHistoriqueUtilise());
            ps.setInt(7, r.getIdReservation());
            ps.executeUpdate();
        }

        HistoryService.log("RESERVATION_UPDATE", "Reservation modifiée id=" + r.getIdReservation());

        // ✅ Calendar sync
        try {
            CalendarService cal = new CalendarService(cnx);
            String title = "Réservation • " + (r.getLocalNom() == null ? ("Local #" + r.getIdLocal()) : r.getLocalNom());
            var start = java.time.LocalDateTime.of(r.getDateReservation(), r.getHeureDebut());
            var end = java.time.LocalDateTime.of(r.getDateReservation(), r.getHeureFin());
            cal.upsertForReservation(r.getIdReservation(), title, start, end, "ACTIVE");
        } catch (Exception e) {
            System.err.println("⚠️ Calendar non synchronisé (update): " + e.getMessage());
        }

        // ✅ recalcul disponibilité du local
        recomputeLocalDisponibilite(r.getIdLocal());
    }

    // ✅ Seuls admin/responsable_centre peuvent changer le statut
    // admin/responsable_centre peuvent changer le statut
    public void updateStatus(int idReservation, String newStatus) throws SQLException {
        if (!UserSession.canManageReservationsStatus()) {
            throw new IllegalArgumentException("Accès refusé: seul admin/responsable peut changer le statut.");
        }

        // ✅ mapping DB (enum): EN_ATTENTE / Confirmer / Annuler
        String ns = (newStatus == null) ? "" : newStatus.trim();
        String dbStatus;
        if (ns.equalsIgnoreCase("CONFIRMER") || ns.equalsIgnoreCase("Confirmer")) dbStatus = "Confirmer";
        else if (ns.equalsIgnoreCase("ANNULER") || ns.equalsIgnoreCase("Annuler")) dbStatus = "Annuler";
        else throw new IllegalArgumentException("Statut invalide.");

        Reservation current = getById(idReservation);
        if (current == null) throw new IllegalArgumentException("Réservation introuvable.");
        if (current.isLocked()) throw new IllegalArgumentException("Statut déjà final (confirmé/annulé).");

        String sql = "UPDATE reservation SET statut=?, id_responsable_centre=? WHERE id_reservation=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, dbStatus);
            ps.setInt(2, UserSession.getCurrentUser().getIdUsers());
            ps.setInt(3, idReservation);
            ps.executeUpdate();
        }

        HistoryService.log("RESERVATION_STATUS", "Reservation id=" + idReservation + " => " + dbStatus);

        // ✅ recalcul disponibilité du local
        updateLocalAvailability(current.getIdLocal());

        // ✅ Génération automatique du QR code (statut Confirmer)
        if ("Confirmer".equalsIgnoreCase(dbStatus)) {
            try {
                Reservation updated = getById(idReservation);
                if (updated != null) {
                    String payload = QrPayloadUtil.buildPayload(updated);
                    Path out = Paths.get("uploads", "qrcodes", "reservation_" + idReservation + ".png");
                    QrCodeUtil.generatePng(payload, 320, out);
                    System.out.println("✅ QR généré: " + out.toAbsolutePath());
                }
            } catch (Exception e) {
                System.err.println("⚠️ QR non généré: " + e.getMessage());
            }
        }

        // ✅ Calendar sync (local DB)
        try {
            CalendarService cal = new CalendarService(cnx);
            Reservation updated = getById(idReservation);
            if (updated != null && updated.getDateReservation() != null && updated.getHeureDebut() != null && updated.getHeureFin() != null) {
                String title = "Réservation • " + (updated.getLocalNom() == null ? ("Local #" + updated.getIdLocal()) : updated.getLocalNom());
                var start = java.time.LocalDateTime.of(updated.getDateReservation(), updated.getHeureDebut());
                var end = java.time.LocalDateTime.of(updated.getDateReservation(), updated.getHeureFin());
                String status = "Annuler".equalsIgnoreCase(dbStatus) ? "CANCELLED" : "ACTIVE";
                cal.upsertForReservation(idReservation, title, start, end, status);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Calendar non synchronisé: " + e.getMessage());
        }

        // ✅ recalcul disponibilité du local
        recomputeLocalDisponibilite(current.getIdLocal());
    }


// ✅ Appelé par le check-in QR: CONFIRMER -> CHECKED_IN
// ✅ Appelé par le check-in QR: on enregistre dans reservation_checkin (sans changer l'ENUM statut)
public void markCheckedIn(int idReservation) throws SQLException {
    Reservation current = getById(idReservation);
    if (current == null) throw new IllegalArgumentException("Réservation introuvable.");

    String st = current.getStatut() == null ? "" : current.getStatut().trim();
    if (!"Confirmer".equalsIgnoreCase(st)) {
        throw new IllegalArgumentException("Check-in interdit: statut doit être Confirmer.");
    }

    // ✅ éviter double check-in
    if (isAlreadyCheckedIn(idReservation)) {
        throw new IllegalArgumentException("Déjà CHECKED_IN.");
    }

    // ✅ enregistrer le check-in (sans changer reservation.statut car ENUM)
    String sql = "INSERT INTO reservation_checkin(reservation_id, checked_in_at) VALUES (?, NOW())";
    try (PreparedStatement ps = cnx.prepareStatement(sql)) {
        ps.setInt(1, idReservation);
        ps.executeUpdate();
    }
}


    public void delete(int idReservation) throws SQLException {
        Reservation current = getById(idReservation);
        if (current != null && current.isLocked()) {
            throw new IllegalArgumentException("Suppression interdite: réservation confirmée/annulée.");
        }
        String sql = "DELETE FROM reservation WHERE id_reservation=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idReservation);
            ps.executeUpdate();
        }

        HistoryService.log("RESERVATION_DELETE", "Reservation supprimée id=" + idReservation);

        // ✅ recalcul disponibilité du local
        if (current != null) updateLocalAvailability(current.getIdLocal());

        // ✅ Calendar: annulation événement lié
        try {
            new CalendarService(cnx).cancelForReservation(idReservation);
        } catch (Exception e) {
            System.err.println("⚠️ Calendar non annulé: " + e.getMessage());
        }

        // ✅ recalcul disponibilité du local
        if (current != null) recomputeLocalDisponibilite(current.getIdLocal());
    }

    /**
     * Règle demandée: par défaut disponible, mais dès qu'il existe au moins une réservation
     * dont le statut n'est pas ANNULER, le local devient indisponible.
     */
    private void recomputeLocalDisponibilite(int idLocal) throws SQLException {
        String sqlCount = "SELECT COUNT(*) FROM reservation WHERE id_local=? AND (statut IS NULL OR UPPER(statut) <> 'ANNULER')";
        int count = 0;
        try (PreparedStatement ps = cnx.prepareStatement(sqlCount)) {
            ps.setInt(1, idLocal);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) count = rs.getInt(1);
            }
        }

        String sqlUpd = "UPDATE localrelaxation SET disponible=? WHERE id_local=?";
        try (PreparedStatement ps = cnx.prepareStatement(sqlUpd)) {
            ps.setBoolean(1, count == 0);
            ps.setInt(2, idLocal);
            ps.executeUpdate();
        }
    }

    private boolean hasOverlap(int idLocal, LocalDate date, LocalTime start, LocalTime end, Integer ignoreId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reservation WHERE id_local=? AND date_reservation=? " +
                (ignoreId != null ? "AND id_reservation<>? " : "") +
                "AND NOT (heure_fin<=? OR heure_debut>=?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            int idx=1;
            ps.setInt(idx++, idLocal);
            ps.setDate(idx++, Date.valueOf(date));
            if (ignoreId != null) ps.setInt(idx++, ignoreId);
            ps.setTime(idx++, Time.valueOf(start));
            ps.setTime(idx, Time.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private void validate(Reservation r) {
        if (r.getDateReservation() == null) throw new IllegalArgumentException("Date obligatoire.");
        if (r.getHeureDebut() == null) throw new IllegalArgumentException("Heure début obligatoire.");
        if (r.getHeureFin() == null) throw new IllegalArgumentException("Heure fin obligatoire.");
        if (!r.getHeureFin().isAfter(r.getHeureDebut())) throw new IllegalArgumentException("Heure fin doit être après l'heure début.");

        // ✅ Métier: créneaux ouvrables uniquement (08:00-18:00) et pause interdite (12:00-13:00)
        LocalTime open = LocalTime.of(8, 0);
        LocalTime close = LocalTime.of(18, 0);
        if (r.getHeureDebut().isBefore(open) || r.getHeureFin().isAfter(close)) {
            throw new IllegalArgumentException("Les réservations sont autorisées uniquement entre 08:00 et 18:00.");
        }
        LocalTime pauseStart = LocalTime.of(12, 0);
        LocalTime pauseEnd = LocalTime.of(13, 0);
        if (r.getHeureDebut().isBefore(pauseEnd) && r.getHeureFin().isAfter(pauseStart)) {
            throw new IllegalArgumentException("Réservation interdite pendant la pause de 12:00 à 13:00.");
        }

        if (r.getIdLocal() <= 0) throw new IllegalArgumentException("Local invalide.");
        if (r.getIdUtilisateur() <= 0) throw new IllegalArgumentException("Utilisateur invalide.");
        if (r.getTypeSession() == null || r.getTypeSession().isBlank()) throw new IllegalArgumentException("Type session obligatoire.");
        if (r.getMotif() == null || r.getMotif().isBlank()) throw new IllegalArgumentException("Motif obligatoire.");
    }



    public boolean isAlreadyCheckedIn(int reservationId) throws SQLException {

        String sql = "SELECT COUNT(*) FROM reservation_checkin WHERE reservation_id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, reservationId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }

        return false;
    }

    private Reservation map(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setIdReservation(rs.getInt("id_reservation"));
        Date d = rs.getDate("date_reservation");
        if (d != null) r.setDateReservation(d.toLocalDate());
        Time hd = rs.getTime("heure_debut");
        if (hd != null) r.setHeureDebut(hd.toLocalTime());
        Time hf = rs.getTime("heure_fin");
        if (hf != null) r.setHeureFin(hf.toLocalTime());
        r.setStatut(rs.getString("statut"));
        r.setTypeSession(rs.getString("type_session"));
        r.setMotif(rs.getString("motif"));
        r.setIdLocal(rs.getInt("id_local"));
        r.setIdUtilisateur(rs.getInt("id_utilisateur"));

        int rc = rs.getInt("id_responsable_centre");
        if (rs.wasNull()) r.setIdResponsableCentre(null);
        else r.setIdResponsableCentre(rc);

        try {
            int hid = rs.getInt("id_historique_utlise");
            if (rs.wasNull()) r.setIdHistoriqueUtilise(null);
            else r.setIdHistoriqueUtilise(hid);
        } catch (SQLException ignored) {
            // colonne peut ne pas être présente sur certains SELECT (sécurité)
        }

        r.setLocalNom(rs.getString("local_nom"));
        r.setLocalType(rs.getString("local_type"));
        r.setLocalDisponible(rs.getBoolean("local_disponible"));
        return r;
    }
}
