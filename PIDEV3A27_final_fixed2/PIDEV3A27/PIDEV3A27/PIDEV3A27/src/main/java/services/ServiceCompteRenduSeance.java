package services;

import models.CompteRenduSeance;
import models.CompteRenduView;
import models.RendezVous;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service CRUD + lectures filtrées pour compte_rendu_seance.
 *
 * IMPORTANT:
 * - Une seule classe et une seule connexion "cnx".
 * - Les méthodes findViews* retournent CompteRenduView (JOIN users) pour afficher nom/prénom.
 */
public class ServiceCompteRenduSeance {

    private final Connection cnx;

    public ServiceCompteRenduSeance(Connection cnx) {
        this.cnx = cnx;
    }

    // ===================== READ (ENTITIES) =====================

    public List<CompteRenduSeance> findAll() throws SQLException {
        String sql = """
            SELECT id_compterendu, id_appointment, date_creationcr, progrescr, resumeseancecr, prochainesactioncr,
                   ai_resumecr, ai_resume_canceled
            FROM compte_rendu_seance
            ORDER BY date_creationcr DESC
        """;
        return fetchList(sql);
    }

    /** Patient : voir ses CR (via rendez_vous) */
    public List<CompteRenduSeance> findByPatient(int idPatient) throws SQLException {
        String sql = """
            SELECT cr.id_compterendu, cr.id_appointment, cr.date_creationcr, cr.progrescr, cr.resumeseancecr, cr.prochainesactioncr,
                   cr.ai_resumecr, cr.ai_resume_canceled
            FROM compte_rendu_seance cr
            JOIN rendez_vous rv ON rv.id_rv = cr.id_appointment
            WHERE rv.id_patient = ?
            ORDER BY cr.date_creationcr DESC
        """;
        return fetchList(sql, idPatient);
    }

    /** Psychologue : voir les CR de ses rendez-vous */
    public List<CompteRenduSeance> findByPsychologist(int idPsychologist) throws SQLException {
        String sql = """
            SELECT cr.id_compterendu, cr.id_appointment, cr.date_creationcr, cr.progrescr, cr.resumeseancecr, cr.prochainesactioncr,
                   cr.ai_resumecr, cr.ai_resume_canceled
            FROM compte_rendu_seance cr
            JOIN rendez_vous rv ON rv.id_rv = cr.id_appointment
            WHERE rv.id_psychologist = ?
            ORDER BY cr.date_creationcr DESC
        """;
        return fetchList(sql, idPsychologist);
    }

    // ===================== sécurité =====================

    public boolean appointmentBelongsToPsychologist(int idAppointment, int idPsychologist) throws SQLException {
        String sql = "SELECT 1 FROM rendez_vous WHERE id_rv=? AND id_psychologist=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idAppointment);
            pst.setInt(2, idPsychologist);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean appointmentBelongsToPatient(int idAppointment, int idPatient) throws SQLException {
        String sql = "SELECT 1 FROM rendez_vous WHERE id_rv=? AND id_patient=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idAppointment);
            pst.setInt(2, idPatient);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ===================== CRUD =====================

    public int addAndReturnId(CompteRenduSeance cr) throws SQLException {
        String sql = """
            INSERT INTO compte_rendu_seance
            (id_appointment, date_creationcr, progrescr, resumeseancecr, prochainesactioncr, ai_resumecr, ai_resume_canceled)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, cr.getIdAppointment());
            pst.setTimestamp(2, cr.getDateCreationCr());
            pst.setString(3, cr.getProgresCr() == null ? null : cr.getProgresCr().name());
            pst.setString(4, cr.getResumeSeanceCr());
            pst.setString(5, cr.getProchainesActionCr());
            pst.setString(6, cr.getAiResumeCr()); // ✅ getter exact
            pst.setInt(7, cr.isAiResumeCanceled() ? 1 : 0);

            pst.executeUpdate();

            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public void update(CompteRenduSeance cr) throws SQLException {
        String sql = """
            UPDATE compte_rendu_seance
            SET id_appointment=?,
                date_creationcr=?,
                progrescr=?,
                resumeseancecr=?,
                prochainesactioncr=?,
                ai_resumecr=?,
                ai_resume_canceled=?
            WHERE id_compterendu=?
        """;

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, cr.getIdAppointment());
            pst.setTimestamp(2, cr.getDateCreationCr());
            pst.setString(3, cr.getProgresCr() == null ? null : cr.getProgresCr().name());
            pst.setString(4, cr.getResumeSeanceCr());
            pst.setString(5, cr.getProchainesActionCr());
            pst.setString(6, cr.getAiResumeCr()); // ✅ getter exact
            pst.setInt(7, cr.isAiResumeCanceled() ? 1 : 0);
            pst.setInt(8, cr.getIdCompteRendu());

            pst.executeUpdate();
        }
    }

    public void deleteById(int idCompteRendu) throws SQLException {
        String sql = "DELETE FROM compte_rendu_seance WHERE id_compterendu=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idCompteRendu);
            pst.executeUpdate();
        }
    }

    // ✅ Actions IA : annuler affichage / supprimer résumé IA
    public void cancelAiResume(int idCompteRendu) throws SQLException {
        String sql = "UPDATE compte_rendu_seance SET ai_resume_canceled=1 WHERE id_compterendu=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idCompteRendu);
            pst.executeUpdate();
        }
    }

    public void deleteAiResume(int idCompteRendu) throws SQLException {
        String sql = "UPDATE compte_rendu_seance SET ai_resumecr=NULL, ai_resume_canceled=0 WHERE id_compterendu=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idCompteRendu);
            pst.executeUpdate();
        }
    }

    public void restoreAiResume(int idCompteRendu) throws SQLException {
        String sql = "UPDATE compte_rendu_seance SET ai_resume_canceled=0 WHERE id_compterendu=?";
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idCompteRendu);
            pst.executeUpdate();
        }
    }

    // ===================== RATING (PATIENT) =====================

    public void updateRatingForPatient(int idCompteRendu, int idPatient, int rating) throws SQLException {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Le rating doit être entre 1 et 5");
        }

        String sql = """
            UPDATE compte_rendu_seance cr
            JOIN rendez_vous rv ON rv.id_rv = cr.id_appointment
            SET cr.rating = ?, cr.rating_date = NOW()
            WHERE cr.id_compterendu = ?
              AND rv.id_patient = ?
        """;

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, rating);
            pst.setInt(2, idCompteRendu);
            pst.setInt(3, idPatient);
            pst.executeUpdate();
        }
    }

    // ===================== VIEWS (JOIN users) =====================

    public List<CompteRenduView> findViewsByPsychologist(int idPsychologist) throws SQLException {
        String sql = """
            SELECT cr.*,
                   rv.appointment_date, rv.appointment_timerv, rv.type_rendez_vous, rv.statutrv,
                   p.nom  AS patient_nom, p.prenom AS patient_prenom,
                   psy.nom AS psy_nom,  psy.prenom AS psy_prenom
            FROM compte_rendu_seance cr
            JOIN rendez_vous rv ON rv.id_rv = cr.id_appointment
            JOIN users p   ON p.id_users   = rv.id_patient
            JOIN users psy ON psy.id_users = rv.id_psychologist
            WHERE rv.id_psychologist = ?
            ORDER BY cr.date_creationcr DESC
        """;
        return fetchViews(sql, idPsychologist);
    }

    public List<CompteRenduView> findViewsByPatient(int idPatient) throws SQLException {
        String sql = """
            SELECT cr.*,
                   rv.appointment_date, rv.appointment_timerv, rv.type_rendez_vous, rv.statutrv,
                   p.nom  AS patient_nom, p.prenom AS patient_prenom,
                   psy.nom AS psy_nom,  psy.prenom AS psy_prenom
            FROM compte_rendu_seance cr
            JOIN rendez_vous rv ON rv.id_rv = cr.id_appointment
            JOIN users p   ON p.id_users   = rv.id_patient
            JOIN users psy ON psy.id_users = rv.id_psychologist
            WHERE rv.id_patient = ?
            ORDER BY cr.date_creationcr DESC
        """;
        return fetchViews(sql, idPatient);
    }

    // ===================== internal mappers =====================

    //ramène une List<CompteRenduview>
    private List<CompteRenduView> fetchViews(String sql, int id) throws SQLException {
        List<CompteRenduView> out = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) out.add(mapView(rs));
            }
        }
        return out;
    }
    //ramène une List<CompteRenduSeance>
    private List<CompteRenduSeance> fetchList(String sql, Object... params) throws SQLException {
        List<CompteRenduSeance> list = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) pst.setObject(i + 1, params[i]);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    CompteRenduSeance cr = new CompteRenduSeance(
                            rs.getInt("id_compterendu"),
                            rs.getInt("id_appointment"),
                            rs.getTimestamp("date_creationcr"),
                            rs.getString("progrescr") == null ? null : CompteRenduSeance.ProgresCR.valueOf(rs.getString("progrescr")),
                            rs.getString("resumeseancecr"),
                            rs.getString("prochainesactioncr")
                    );

                    // ✅ champs IA
                    cr.setAiResumeCr(rs.getString("ai_resumecr"));
                    cr.setAiResumeCanceled(rs.getBoolean("ai_resume_canceled"));

                    // rating (peut être null)
                    try {
                        Object r = rs.getObject("rating");
                        if (r != null) cr.setRating(((Number) r).intValue());
                    } catch (SQLException ignored) {}

                    list.add(cr);
                }
            }
        }
        return list;
    }

    private CompteRenduView mapView(ResultSet rs) throws SQLException {
        CompteRenduView v = new CompteRenduView();
        v.setIdCompteRendu(rs.getInt("id_compterendu"));
        v.setIdAppointment(rs.getInt("id_appointment"));
        v.setDateCreationCr(rs.getTimestamp("date_creationcr"));

        String prog = rs.getString("progrescr");
        if (prog != null && !prog.isEmpty()) {
            v.setProgresCr(CompteRenduSeance.ProgresCR.valueOf(prog));
        }

        v.setResumeSeanceCr(rs.getString("resumeseancecr"));
        v.setProchainesActionCr(rs.getString("prochainesactioncr"));

        // ✅ champs IA
        v.setAiResumeCr(rs.getString("ai_resumecr"));
        v.setAiResumeCanceled(rs.getBoolean("ai_resume_canceled"));

        v.setRvDate(rs.getDate("appointment_date"));
        v.setRvTime(rs.getTime("appointment_timerv"));

        String type = rs.getString("type_rendez_vous");
        if (type != null && !type.isEmpty()) v.setRvType(RendezVous.TypeRV.valueOf(type));

        String statut = rs.getString("statutrv");
        if (statut != null && !statut.isEmpty()) v.setRvStatut(RendezVous.StatutRV.valueOf(statut));

        v.setPatientFullName(safeFullName(rs.getString("patient_prenom"), rs.getString("patient_nom")));
        v.setPsychologistFullName(safeFullName(rs.getString("psy_prenom"), rs.getString("psy_nom")));

        // rating peut être null
        try {
            Object r = rs.getObject("rating");
            if (r != null) v.setRating(((Number) r).intValue());
        } catch (SQLException ignored) {}

        return v;
    }

    private String safeFullName(String prenom, String nom) {
        String a = prenom == null ? "" : prenom.trim();
        String b = nom == null ? "" : nom.trim();
        return (a + " " + b).trim();
    }
}