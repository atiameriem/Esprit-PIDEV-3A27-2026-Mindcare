package services;

import models.RendezVous;
import models.RendezVousView;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class ServiceRendezVous {

    private final Connection cnx;

    public ServiceRendezVous(Connection cnx) {
        this.cnx = cnx;
    }

    // ====================== READ ======================

    public List<RendezVous> findByPsychologist(int idPsychologist) throws SQLException {
        String sql = """
            SELECT id_rv, id_patient, id_psychologist, statutrv, confirmation_status,
                   appointment_date, type_rendez_vous, appointment_timerv
            FROM rendez_vous
            WHERE id_psychologist = ?
            ORDER BY appointment_date DESC, appointment_timerv DESC
        """;

        List<RendezVous> list = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idPsychologist);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<RendezVous> findByPatient(int idPatient) throws SQLException {
        String sql = """
            SELECT id_rv, id_patient, id_psychologist, statutrv, confirmation_status,
                   appointment_date, type_rendez_vous, appointment_timerv
            FROM rendez_vous
            WHERE id_patient = ?
            ORDER BY appointment_date DESC, appointment_timerv DESC
        """;

        List<RendezVous> list = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idPatient);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public boolean existsRendezVousForPatient(int idRv, int idPatient) throws SQLException {
        String sql = "SELECT 1 FROM rendez_vous WHERE id_rv=? AND id_patient=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idRv);
            pst.setInt(2, idPatient);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ====================== VIEWS ======================

    public List<RendezVousView> findViewsByPsychologist(int idPsychologist) throws SQLException {
        String sql = """
            SELECT rv.*,
                   p.nom  AS patient_nom, p.prenom AS patient_prenom,
                   psy.nom AS psy_nom,  psy.prenom AS psy_prenom
            FROM rendez_vous rv
            JOIN users p   ON p.id_users   = rv.id_patient 
            JOIN users psy ON psy.id_users = rv.id_psychologist
            WHERE rv.id_psychologist=?
            ORDER BY rv.appointment_date DESC, rv.appointment_timerv DESC
        """;

        List<RendezVousView> out = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idPsychologist);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) out.add(mapView(rs));
            }
        }
        return out;
    }

    public List<RendezVousView> findViewsByPatient(int idPatient) throws SQLException {
        String sql = """
            SELECT rv.*,
                   p.nom  AS patient_nom, p.prenom AS patient_prenom,
                   psy.nom AS psy_nom,  psy.prenom AS psy_prenom
            FROM rendez_vous rv
            JOIN users p   ON p.id_users   = rv.id_patient
            JOIN users psy ON psy.id_users = rv.id_psychologist
            WHERE rv.id_patient=?
            ORDER BY rv.appointment_date DESC, rv.appointment_timerv DESC
        """;

        List<RendezVousView> out = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idPatient);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) out.add(mapView(rs));
            }
        }
        return out;
    }

    private RendezVousView mapView(ResultSet rs) throws SQLException {
        RendezVousView v = new RendezVousView();

        v.setIdRv(rs.getInt("id_rv"));
        v.setIdPatient(rs.getInt("id_patient"));
        v.setIdPsychologist(rs.getInt("id_psychologist"));

        String statut = rs.getString("statutrv");
        if (statut != null && !statut.trim().isEmpty()) {
            v.setStatutRv(RendezVous.StatutRV.valueOf(statut.trim()));
        }

        String conf = rs.getString("confirmation_status");
        if (conf != null && !conf.trim().isEmpty()) {
            v.setConfirmationStatus(RendezVous.ConfirmationStatus.valueOf(conf.trim()));
        }

        String type = rs.getString("type_rendez_vous");
        if (type != null && !type.trim().isEmpty()) {
            v.setTypeRendezVous(RendezVous.TypeRV.valueOf(type.trim()));
        }

        v.setAppointmentDate(rs.getDate("appointment_date"));
        v.setAppointmentTimeRv(rs.getTime("appointment_timerv"));

        String patientFull = safeFullName(rs.getString("patient_prenom"), rs.getString("patient_nom"));
        String psyFull = safeFullName(rs.getString("psy_prenom"), rs.getString("psy_nom"));
        v.setPatientFullName(patientFull);
        v.setPsychologistFullName(psyFull);

        return v;
    }

    private String safeFullName(String prenom, String nom) {
        String p = prenom == null ? "" : prenom.trim();
        String n = nom == null ? "" : nom.trim();
        return (p + " " + n).trim();
    }

    public boolean isPsychologistUser(int idPsychologist) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE id_users=? AND role='psychologue'";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idPsychologist);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ====================== DISPONIBILITÉ (créneaux déjà réservés) ======================

    /**
     * Retourne les heures déjà prises pour un psychologue et une date.
     * NB: on bloque les créneaux même si le RDV est en_attente, car il est déjà réservé.
     * @param excludeRvId optionnel : permet d'exclure un rendez-vous (utile en mode édition)
     */
    public Set<LocalTime> getReservedTimes(int idPsychologist, LocalDate date, Integer excludeRvId) throws SQLException {
        String sql = """
            SELECT id_rv, appointment_timerv
            FROM rendez_vous
            WHERE id_psychologist = ?
              AND DATE(appointment_date) = ?
              AND appointment_timerv IS NOT NULL
              AND (
                    statutrv = 'en_cours'
                    OR confirmation_status IN ('confirme','en_attente')
                  )
        """;

        Set<LocalTime> out = new HashSet<>();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idPsychologist);
            pst.setDate(2, java.sql.Date.valueOf(date));
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int idRv = rs.getInt("id_rv");
                    if (excludeRvId != null && excludeRvId == idRv) continue;
                    Time t = rs.getTime("appointment_timerv");
                    if (t != null) out.add(t.toLocalTime());
                }
            }
        }
        return out;
    }

    // ====================== CRUD PATIENT ======================

    public int addAndReturnId(RendezVous rv) throws SQLException {
        String sql = """
            INSERT INTO rendez_vous (id_patient, id_psychologist, statutrv, confirmation_status,
                                    appointment_date, type_rendez_vous, appointment_timerv)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, rv.getIdPatient());
            pst.setInt(2, rv.getIdPsychologist());

            pst.setString(3, (rv.getStatutRv() == null) ? "" : rv.getStatutRv().name());
            pst.setString(4, (rv.getConfirmationStatus() == null)
                    ? RendezVous.ConfirmationStatus.en_attente.name()
                    : rv.getConfirmationStatus().name());

            pst.setDate(5, rv.getAppointmentDate());
            pst.setString(6, rv.getTypeRendezVous().name());
            pst.setTime(7, rv.getAppointmentTimeRv());

            pst.executeUpdate();

            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public void updateForPatient(RendezVous rv, int idPatient) throws SQLException {
        String sql = """
            UPDATE rendez_vous
            SET id_psychologist=?, appointment_date=?, type_rendez_vous=?, appointment_timerv=?
            WHERE id_rv=? AND id_patient=? AND confirmation_status='en_attente'
        """;

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, rv.getIdPsychologist());
            pst.setDate(2, rv.getAppointmentDate());
            pst.setString(3, rv.getTypeRendezVous().name());
            pst.setTime(4, rv.getAppointmentTimeRv());
            pst.setInt(5, rv.getIdRv());
            pst.setInt(6, idPatient);
            pst.executeUpdate();
        }
    }

    public void deleteForPatient(int idRv, int idPatient) throws SQLException {
        String delCR = "DELETE FROM compte_rendu_seance WHERE id_appointment = ?";
        String delRV = "DELETE FROM rendez_vous WHERE id_rv=? AND id_patient=? AND confirmation_status='en_attente'";

        try {
            cnx.setAutoCommit(false);

            try (PreparedStatement pst1 = cnx.prepareStatement(delCR)) {
                pst1.setInt(1, idRv);
                pst1.executeUpdate();
            }

            try (PreparedStatement pst2 = cnx.prepareStatement(delRV)) {
                pst2.setInt(1, idRv);
                pst2.setInt(2, idPatient);
                pst2.executeUpdate();
            }

            cnx.commit();

        } catch (SQLException e) {
            cnx.rollback();
            throw e;
        } finally {
            cnx.setAutoCommit(true);
        }
    }

    private RendezVous map(ResultSet rs) throws SQLException {
        String statut = rs.getString("statutrv");
        RendezVous.StatutRV st = (statut == null || statut.trim().isEmpty())
                ? null
                : RendezVous.StatutRV.valueOf(statut.trim());

        String conf = rs.getString("confirmation_status");
        RendezVous.ConfirmationStatus cs = (conf == null || conf.trim().isEmpty())
                ? null
                : RendezVous.ConfirmationStatus.valueOf(conf.trim());

        return new RendezVous(
                rs.getInt("id_rv"),
                rs.getInt("id_patient"),
                rs.getInt("id_psychologist"),
                st,
                cs,
                rs.getDate("appointment_date"),
                RendezVous.TypeRV.valueOf(rs.getString("type_rendez_vous")),
                rs.getTime("appointment_timerv")
        );
    }

    // ====================== ACTIONS PSY ======================

    public void updateConfirmationStatusForPsychologist(int idRv, int idPsychologist,
                                                        RendezVous.ConfirmationStatus status) throws SQLException {
        String sql = """
            UPDATE rendez_vous
            SET confirmation_status=?
            WHERE id_rv=? AND id_psychologist=?
        """;
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, status.name());
            pst.setInt(2, idRv);
            pst.setInt(3, idPsychologist);
            pst.executeUpdate();
        }
    }

    public void updateStatutForPsychologist(int idRv, int idPsychologist,
                                            RendezVous.StatutRV statut) throws SQLException {
        String sql = """
            UPDATE rendez_vous
            SET statutrv=?
            WHERE id_rv=? AND id_psychologist=? AND confirmation_status='confirme'
        """;
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setString(1, statut == null ? "" : statut.name());
            pst.setInt(2, idRv);
            pst.setInt(3, idPsychologist);
            pst.executeUpdate();
        }
    }

    // ====================== KPI STATS (Confirmé/Annulé/...) ======================

    public static class RendezVousStats {
        public int total;
        public int confirmed;
        public int pending;
        public int cancelled;
        public int finished;
    }

    public RendezVousStats getStatsForPsychologist(int psyId) throws SQLException {
        RendezVousStats s = new RendezVousStats();

        // total
        try (PreparedStatement pst = cnx.prepareStatement(
                "SELECT COUNT(*) FROM rendez_vous WHERE id_psychologist=?")) {
            pst.setInt(1, psyId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) s.total = rs.getInt(1);
            }
        }

        // confirmation_status
        try (PreparedStatement pst = cnx.prepareStatement(
                "SELECT confirmation_status, COUNT(*) FROM rendez_vous WHERE id_psychologist=? GROUP BY confirmation_status")) {
            pst.setInt(1, psyId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    String status = rs.getString(1);
                    int count = rs.getInt(2);
                    if (status == null) continue;

                    if ("confirme".equalsIgnoreCase(status)) s.confirmed = count;
                    else if ("en_attente".equalsIgnoreCase(status)) s.pending = count;
                    else if ("annule".equalsIgnoreCase(status)) s.cancelled = count;
                }
            }
        }

        // terminés
        try (PreparedStatement pst = cnx.prepareStatement(
                "SELECT COUNT(*) FROM rendez_vous WHERE id_psychologist=? AND statutrv='termine'")) {
            pst.setInt(1, psyId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) s.finished = rs.getInt(1);
            }
        }

        return s;
    }

    // ====================== STAT 6 DERNIERS MOIS (BarChart) ======================

    private String prettyTypeLabel(String raw) {
        if (raw == null) return "Inconnu";
        raw = raw.trim().toLowerCase();

        return switch (raw) {
            case "premiere_consultation" -> "Première consultation";
            case "suivi" -> "Suivi";
            case "urgence" -> "Urgence";
            default -> raw;
        };
    }

    public static class MonthTypeCount {
        public final String monthLabel; // "Jan", "Fév", ...
        public final String typeLabel;  // "Suivi", ...
        public final int count;

        public MonthTypeCount(String monthLabel, String typeLabel, int count) {
            this.monthLabel = monthLabel;
            this.typeLabel = typeLabel;
            this.count = count;
        }
    }

    public List<MonthTypeCount> countByTypeLast6Months(int psyId) throws SQLException {
        String sql =
                "SELECT YEAR(appointment_date) as y, MONTH(appointment_date) as m, type_rendez_vous, COUNT(*) as c " +
                        "FROM rendez_vous " +
                        "WHERE id_psychologist=? " +
                        "  AND appointment_date >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH) " +
                        "GROUP BY y, m, type_rendez_vous " +
                        "ORDER BY y, m";

        List<MonthTypeCount> out = new ArrayList<>();

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, psyId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int month = rs.getInt("m");
                    String typeRaw = rs.getString("type_rendez_vous");
                    int count = rs.getInt("c");

                    String monthLabel = monthToFrShort(month);
                    String typeLabel = prettyTypeLabel(typeRaw);

                    out.add(new MonthTypeCount(monthLabel, typeLabel, count));
                }
            }
        }

        return out;
    }

    private String monthToFrShort(int m) {
        return switch (m) {
            case 1 -> "Jan";
            case 2 -> "Fév";
            case 3 -> "Mar";
            case 4 -> "Avr";
            case 5 -> "Mai";
            case 6 -> "Juin";
            case 7 -> "Juil";
            case 8 -> "Aoû";
            case 9 -> "Sep";
            case 10 -> "Oct";
            case 11 -> "Nov";
            case 12 -> "Déc";
            default -> "M" + m;
        };
    }

    // ====================== COMPAT: si ton controller appelle encore cette méthode ======================
    // Map<typeLabel, Map<monthIndex(1..6), count>>
    public Map<String, Map<Integer, Integer>> getStatsByTypeForJanToJun(int psyId, int year) {
        Map<String, Map<Integer, Integer>> result = new LinkedHashMap<>();

        String sql = """
            SELECT MONTH(appointment_date) AS mois,
                   type_rendez_vous        AS type,
                   COUNT(*)                AS total
            FROM rendez_vous
            WHERE id_psychologist = ?
              AND YEAR(appointment_date) = ?
              AND MONTH(appointment_date) BETWEEN 1 AND 6
            GROUP BY MONTH(appointment_date), type_rendez_vous
            ORDER BY mois ASC
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, psyId);
            ps.setInt(2, year);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int mois = rs.getInt("mois"); // 1..6
                    String typeRaw = rs.getString("type");
                    int total = rs.getInt("total");

                    String typeLabel = prettyTypeLabel(typeRaw);
                    result.computeIfAbsent(typeLabel, k -> new HashMap<>()).put(mois, total);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public String getPatientPhoneByRdvId(int idRv) throws SQLException {
        String sql = """
        SELECT u.telephone
        FROM rendez_vous r
        JOIN users u ON u.id_users = r.id_patient
        WHERE r.id_rv = ?
    """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idRv);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("telephone");
                return null;
            }
        }
    }
}