package services;

import models.RendezVous;
import models.RendezVousView;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceRendezVous {

    private final Connection cnx;

    public ServiceRendezVous(Connection cnx) {
        this.cnx = cnx;
    }

    // ===== READ =====

    public List<RendezVous> findByPsychologist(int idPsychologist) throws SQLException {
        String sql = """
            SELECT id_rv, id_patient, id_psychologist, statutrv, appointment_date, type_rendez_vous, appointment_timerv
            FROM rendez_vous
            WHERE id_psychologist = ?
            ORDER BY appointment_date DESC, appointment_timerv DESC
        """;

        List<RendezVous> list = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idPsychologist);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public List<RendezVous> findByPatient(int idPatient) throws SQLException {
        String sql = """
            SELECT id_rv, id_patient, id_psychologist, statutrv, appointment_date, type_rendez_vous, appointment_timerv
            FROM rendez_vous
            WHERE id_patient = ?
            ORDER BY appointment_date DESC, appointment_timerv DESC
        """;

        List<RendezVous> list = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, idPatient);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
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

    
    // ===== VIEWS (avec noms) =====

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
        if (statut != null && !statut.isEmpty()) v.setStatutRv(RendezVous.StatutRV.valueOf(statut));

        String type = rs.getString("type_rendez_vous");
        if (type != null && !type.isEmpty()) v.setTypeRendezVous(RendezVous.TypeRV.valueOf(type));

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

    // ===== CRUD (Patient) =====

    public int addAndReturnId(RendezVous rv) throws SQLException {
        String sql = """
            INSERT INTO rendez_vous (id_patient, id_psychologist, statutrv, appointment_date, type_rendez_vous, appointment_timerv)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, rv.getIdPatient());
            pst.setInt(2, rv.getIdPsychologist());
            pst.setString(3, rv.getStatutRv().name());
            pst.setDate(4, rv.getAppointmentDate());
            pst.setString(5, rv.getTypeRendezVous().name());
            pst.setTime(6, rv.getAppointmentTimeRv());
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
            SET id_psychologist=?, statutrv=?, appointment_date=?, type_rendez_vous=?, appointment_timerv=?
            WHERE id_rv=? AND id_patient=?
        """;

        try (PreparedStatement pst = cnx.prepareStatement(sql)) {
            pst.setInt(1, rv.getIdPsychologist());
            pst.setString(2, rv.getStatutRv().name());
            pst.setDate(3, rv.getAppointmentDate());
            pst.setString(4, rv.getTypeRendezVous().name());
            pst.setTime(5, rv.getAppointmentTimeRv());
            pst.setInt(6, rv.getIdRv());
            pst.setInt(7, idPatient);
            pst.executeUpdate();
        }
    }

    public void deleteForPatient(int idRv, int idPatient) throws SQLException {

        String delCR = "DELETE FROM compte_rendu_seance WHERE id_appointment = ?";
        String delRV = "DELETE FROM rendez_vous WHERE id_rv=? AND id_patient=?";

        try {
            cnx.setAutoCommit(false);

            // 1) supprimer le compte-rendu lié au rendez-vous
            try (PreparedStatement pst1 = cnx.prepareStatement(delCR)) {
                pst1.setInt(1, idRv);
                pst1.executeUpdate();
            }

            // 2) supprimer le rendez-vous (sécurisé: seulement si appartient au patient)
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
        return new RendezVous(
                rs.getInt("id_rv"),
                rs.getInt("id_patient"),
                rs.getInt("id_psychologist"),
                RendezVous.StatutRV.valueOf(rs.getString("statutrv")),
                rs.getDate("appointment_date"),
                RendezVous.TypeRV.valueOf(rs.getString("type_rendez_vous")),
                rs.getTime("appointment_timerv")
        );
    }
}
