package services;

import models.Reservation;
import utils.MyDatabase;
import utils.QrPayloadUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Business logic:
 * - Reservation exists
 * - statut = Confirmer
 * - date = aujourd'hui
 * - heure valide (now between heure_debut and heure_fin)
 * - pas déjà CHECKED_IN (table reservation_checkin)
 * - Then store checkInTime
 */
public class CheckInService {

    private final ReservationService reservationService;
    private final Connection cnx;

    public CheckInService() {
        this.cnx = MyDatabase.getInstance().getConnection();
        this.reservationService = new ReservationService(this.cnx);
    }

    public CheckInService(Connection cnx) {
        this.cnx = cnx;
        this.reservationService = new ReservationService(cnx);
    }

    public String checkInFromQr(String qrText) throws Exception {
        QrPayloadUtil.ParsedPayload p = QrPayloadUtil.parseAndVerify(qrText);

        Reservation r = reservationService.getById(p.reservationId);
        if (r == null) throw new IllegalArgumentException("Réservation introuvable.");

        // ✅ statut doit être "Confirmer" (ENUM)
        String st = r.getStatut() == null ? "" : r.getStatut().trim();
        if (!"Confirmer".equalsIgnoreCase(st)) {
            throw new IllegalArgumentException("Statut invalide. Attendu: Confirmer.");
        }

        // ✅ déjà check-in ?
        if (reservationService.isAlreadyCheckedIn(r.getIdReservation())) {
            throw new IllegalArgumentException("Déjà CHECKED_IN.");
        }

        // ✅ date = aujourd'hui
        if (r.getDateReservation() == null || !r.getDateReservation().equals(LocalDate.now())) {
            throw new IllegalArgumentException("Date invalide (doit être aujourd'hui).");
        }

        // ✅ heure valide
        LocalTime now = LocalTime.now();
        if (r.getHeureDebut() == null || r.getHeureFin() == null) {
            throw new IllegalArgumentException("Heures manquantes.");
        }
        if (now.isBefore(r.getHeureDebut()) || now.isAfter(r.getHeureFin())) {
            throw new IllegalArgumentException("Heure invalide (hors créneau).");
        }

        // ✅ store checkin time (DB)
        upsertCheckin(r.getIdReservation());

        return "✅ CHECK-IN validé pour réservation #" + r.getIdReservation()
                + " à " + Timestamp.from(java.time.Instant.now());
    }

    private void upsertCheckin(int reservationId) throws Exception {
        String sqlCreate = "CREATE TABLE IF NOT EXISTS reservation_checkin (" +
                "reservation_id INT PRIMARY KEY, " +
                "checked_in_at DATETIME NOT NULL" +
                ")";
        try (Statement st = cnx.createStatement()) {
            st.execute(sqlCreate);
        }

        // MySQL upsert
        String sql = "INSERT INTO reservation_checkin(reservation_id, checked_in_at) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE checked_in_at=VALUES(checked_in_at)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ps.setTimestamp(2, Timestamp.from(java.time.Instant.now()));
            ps.executeUpdate();
        }
    }
}